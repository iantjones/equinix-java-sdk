/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.enums.PeeringType;
import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderConnectionAdapter;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;

/**
 * Assembles a <em>complete</em> Fabric connection body from a {@link PlannedConnection} — both
 * endpoints, encapsulation, and cloud authorization — so a live dry-run or a real create sends the
 * whole request the API needs, not the endpoint-less name + bandwidth shell that Fabric rejects with
 * {@code EQ-3142501 "Null value for aSide access point"}.
 *
 * <p>A provider connection is <strong>Cloud&nbsp;Router&nbsp;&rarr;&nbsp;service&nbsp;profile</strong>:
 * its A-side is the Fabric Cloud Router (by uuid — the thing that does not exist at PLAN time, so its
 * uuid is passed in by the caller once the router is real), and its Z-side is the cloud provider,
 * carrying the service-profile uuid, the customer authorization key, the seller region and (for Azure)
 * the peering type, all read off the planned connection. A backbone link is
 * <strong>Cloud&nbsp;Router&nbsp;&rarr;&nbsp;Cloud&nbsp;Router</strong>.</p>
 *
 * <p>This is the single home of the connection-body shape so the plan-time lens-3b dry-run (against a
 * pre-existing endpoint) and the execution-time lens-3a pre-flight + real create (against a
 * just-provisioned router) build identical bodies. The returned builder is left un-terminated: the
 * caller chains {@code .dryRun().create()} to validate, or {@code .create()} to provision.</p>
 */
public final class ConnectionBodies {

    private ConnectionBodies() {}

    /**
     * A human-readable label for the cloud-specific authorization key a connection to the given
     * provider requires. Backed by the documented per-provider key semantics
     * (see {@link CloudProviderConnectionAdapter#getAuthenticationKey()}), it works with nothing but
     * the provider type — no catalog fetch and no customer resources — so it is safe to show a
     * brand-new customer exactly what to gather.
     *
     * @param type the cloud provider type; {@code null} tolerated
     * @return the authorization-key label, never blank
     */
    public static String authenticationKeyLabel(CloudProviderType type) {
        if (type == null) {
            return "cloud provider authorization key";
        }
        return switch (type) {
            case AWS -> "AWS Account ID (12-digit)";
            case AZURE -> "Azure ExpressRoute service key (GUID)";
            case GOOGLE_CLOUD -> "GCP pairing key";
            case ORACLE_CLOUD -> "Oracle FastConnect virtual-circuit OCID";
            case IBM_CLOUD -> "IBM Cloud Direct Link authorization ID";
            case ALIBABA_CLOUD -> "Alibaba Cloud account ID";
            case OTHER -> "cloud provider authorization key";
        };
    }

    /**
     * Resolves a Fabric provider label ("AWS", "Amazon Web Services", "Azure ExpressRoute", ...) to a
     * {@link CloudProviderType}, falling back to {@link CloudProviderType#OTHER} for a label that
     * matches no well-known provider (a third-party service profile).
     *
     * @param providerLabel the provider label; {@code null}/blank tolerated
     * @return the resolved type, never {@code null}
     */
    public static CloudProviderType resolveCloudType(String providerLabel) {
        if (providerLabel != null) {
            for (CloudProviderType type : CloudProviderType.values()) {
                if (type.matchesServiceProfileName(providerLabel)) {
                    return type;
                }
            }
        }
        return CloudProviderType.OTHER;
    }

    /**
     * Builds a cloud-provider Z-side adapter from a planned connection's resolved provider fields
     * (service-profile uuid, authorization key, seller region, and Azure peering type). Uniform
     * across every provider — the typed reference adapters differ only in these same fields plus the
     * provider type and (Azure) peering, all of which this carries — so IBM and Alibaba, which have
     * no dedicated adapter, still send their authorization key and seller region.
     *
     * @param connection the planned provider connection
     * @return an adapter suitable for {@link ConnectionOperator.ConnectionBuilder#zSideCloudProvider}
     */
    public static CloudProviderConnectionAdapter<?> cloudAdapter(PlannedConnection connection) {
        return new PlannedCloudAdapter(connection);
    }

    /**
     * Assembles a provider connection body against a to-be-created (or already existing) Cloud Router
     * identified by uuid. Used by the execution-time pre-flight + real create once the router uuid is
     * known, and by the plan-time lens-3b dry-run when the caller supplies an existing Cloud Router.
     *
     * @param connections     the Fabric connections client
     * @param connection      the planned provider connection
     * @param aSideRouterUuid the real Cloud Router uuid to use as the A-side
     * @return an un-terminated builder; chain {@code .dryRun().create()} or {@code .create()}
     */
    public static ConnectionOperator.ConnectionBuilder providerBody(
            Connections connections, PlannedConnection connection, String aSideRouterUuid) {
        ConnectionOperator.ConnectionBuilder builder = connections.define(connection.getConnectionType())
                .name(connection.getName())
                .bandwidth(connection.getBandwidthMbps())
                .aSideAccessPointCloudRouter(aSideRouterUuid)
                .zSideCloudProvider(cloudAdapter(connection), linkProtocol(connection.getZSideVlanTag()));
        return withNotification(withRedundancy(builder, connection), connection);
    }

    /**
     * Assembles a provider connection body against a pre-existing customer port as the A-side
     * (lens 3b: the endpoint is real, so the connection can be dry-run at PLAN time).
     *
     * @param connections   the Fabric connections client
     * @param connection    the planned provider connection
     * @param aSidePortUuid the existing customer port uuid to use as the A-side
     * @return an un-terminated builder; chain {@code .dryRun().create()} or {@code .create()}
     */
    public static ConnectionOperator.ConnectionBuilder providerBodyOnPort(
            Connections connections, PlannedConnection connection, String aSidePortUuid) {
        ConnectionOperator.ConnectionBuilder builder = connections.define(connection.getConnectionType())
                .name(connection.getName())
                .bandwidth(connection.getBandwidthMbps())
                .aSideAccessPointPort(aSidePortUuid, linkProtocol(connection.getZSideVlanTag()))
                .zSideCloudProvider(cloudAdapter(connection), linkProtocol(connection.getZSideVlanTag()));
        return withNotification(withRedundancy(builder, connection), connection);
    }

    /**
     * Assembles a backbone link body (Cloud Router to Cloud Router). Both router uuids must be known,
     * so this is used at execution time once Phase&nbsp;1 has provisioned the routers.
     *
     * @param connections     the Fabric connections client
     * @param connection      the planned backbone connection
     * @param aSideRouterUuid the A-side Cloud Router uuid
     * @param zSideRouterUuid the Z-side Cloud Router uuid
     * @return an un-terminated builder; chain {@code .dryRun().create()} or {@code .create()}
     */
    public static ConnectionOperator.ConnectionBuilder backboneBody(
            Connections connections, PlannedConnection connection, String aSideRouterUuid, String zSideRouterUuid) {
        ConnectionOperator.ConnectionBuilder builder = connections.define(connection.getConnectionType())
                .name(connection.getName())
                .bandwidth(connection.getBandwidthMbps())
                .aSideAccessPointCloudRouter(aSideRouterUuid)
                .zSideAccessPointCloudRouter(zSideRouterUuid);
        return withNotification(builder, connection);
    }

    private static ConnectionOperator.ConnectionBuilder withNotification(
            ConnectionOperator.ConnectionBuilder builder, PlannedConnection connection) {
        // Every configured recipient is sent, not just the first: each call appends the address to
        // the single ALL-type notification entry on the wire body.
        if (connection.getNotificationEmails() != null) {
            for (String email : connection.getNotificationEmails()) {
                if (email != null && !email.isBlank()) {
                    builder.notification(email);
                }
            }
        }
        return builder;
    }

    /**
     * Stamps connection-level redundancy when the planned connection names a redundancy group, so a
     * primary + diverse-secondary VC pair to the same cloud is expressed on the wire instead of being
     * silently dropped. A connection with no group is left standalone (the common case). When a group is
     * set but no priority is given, {@link RedundancyPriority#PRIMARY} is the documented default rather
     * than sending an ambiguous group with no role.
     */
    private static ConnectionOperator.ConnectionBuilder withRedundancy(
            ConnectionOperator.ConnectionBuilder builder, PlannedConnection connection) {
        String group = connection.getZSideRedundancyGroup();
        if (group == null || group.isBlank()) {
            return builder;
        }
        RedundancyPriority priority = connection.getRedundancyPriority() != null
                ? connection.getRedundancyPriority()
                : RedundancyPriority.PRIMARY;
        builder.redundancy(group, priority);
        return builder;
    }

    /**
     * A cloud VC requires DOT1Q encapsulation with a VLAN tag. When the caller has not yet supplied a
     * VLAN (a to-be-gathered customer input), a tagless DOT1Q is built so the request shape is still
     * DOT1Q; the API surfaces the missing tag rather than the SDK guessing one.
     */
    private static LinkProtocol linkProtocol(Integer vlanTag) {
        return vlanTag != null
                ? LinkProtocol.dot1q().vlanTag(vlanTag).create()
                : LinkProtocol.dot1q().create();
    }

    /**
     * A {@link CloudProviderConnectionAdapter} whose fields are read straight off a
     * {@link PlannedConnection}. Lets a plan describe its Z-side cloud endpoint without a live cloud
     * SDK object on the classpath.
     */
    private static final class PlannedCloudAdapter implements CloudProviderConnectionAdapter<Void> {

        private final PlannedConnection connection;

        PlannedCloudAdapter(PlannedConnection connection) {
            this.connection = connection;
        }

        @Override
        public String getServiceProfileUuid() {
            return connection.getZSideServiceProfileUuid();
        }

        @Override
        public String getAuthenticationKey() {
            return connection.getZSideAuthenticationKey();
        }

        @Override
        public String getSellerRegion() {
            return connection.getZSideSellerRegion();
        }

        @Override
        public Void getSource() {
            return null;
        }

        @Override
        public CloudProviderType getProviderType() {
            return connection.getZSideCloudType() != null
                    ? connection.getZSideCloudType() : CloudProviderType.OTHER;
        }

        @Override
        public PeeringType getPreferredPeeringType() {
            return connection.getZSidePeeringType();
        }
    }
}
