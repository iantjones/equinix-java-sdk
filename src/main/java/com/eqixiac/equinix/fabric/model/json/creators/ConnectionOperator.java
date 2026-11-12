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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.ConnectionClientImpl;
import com.eqixiac.equinix.fabric.enums.*;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceToken;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.implementation.*;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConnectionOperator extends ResourceImpl<Connection> {

    @Getter
    private final PageablePost<Connection> serviceClient;

    public ConnectionOperator(PageablePost<Connection> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ConnectionBuilder create(ConnectionType type) {
        return new ConnectionBuilder(type);
    }

    public ConnectionUpdater update(String uuid) {
        return new ConnectionUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class ConnectionBuilder {

        private final ConnectionType type;
        private String name;
        private Order order;
        private Integer bandwidth;
        private Redundancy redundancy;
        private SimpleAccessPoint aSideAccessPoint;
        private SimpleAccessPoint zSideAccessPoint;
        private ServiceTokenRef aSideServiceToken;
        private ServiceTokenRef zSideServiceToken;
        private List<Notification> notifications = Collections.singletonList(new Notification(NotificationType.ALL, new ArrayList<>()));
        private GeoScopeType geoScope;
        private Project project;
        private List<ConnectionSideAdditionalInfo> additionalInfo;
        private MarketplaceSubscriptionRef marketplaceSubscription;
        private boolean dryRun;

        protected ConnectionBuilder(ConnectionType type) {
            this.type = type;
        }

         public ConnectionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ConnectionBuilder purchaseOrderNumber(String purchaseOrderNumber) {
            this.order = new Order(purchaseOrderNumber);
            return this;
        }

         public ConnectionBuilder bandwidth(Integer bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

         public ConnectionBuilder redundancy(Redundancy redundancy) {
            this.redundancy = redundancy;
            return this;
        }

         public ConnectionBuilder redundancy(String group, RedundancyPriority priority) {
            // ConnectionRedundancy has no 'enabled' member; it is port-only and omitted when null.
            this.redundancy = new Redundancy(null, group, priority);
            return this;
        }

        public ConnectionBuilder aSideServiceToken(ServiceToken serviceToken) {
            this.aSideServiceToken = new ServiceTokenRef(serviceToken.getUuid());
            return this;
        }

        public ConnectionBuilder aSideServiceToken(String serviceTokenUuid) {
            this.aSideServiceToken = new ServiceTokenRef(serviceTokenUuid);
            return this;
        }

        public ConnectionBuilder aSideAccessPoint(Port port, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = port.accessPoint().setLinkProtocol(linkProtocol);
            return this;
        }

        public ConnectionBuilder aSideAccessPointPort(String portUuid, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.COLO)
                    .port(portUuid)
                    .linkProtocol(linkProtocol).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPointService(String serviceUuid, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(serviceUuid)
                    .linkProtocol(linkProtocol).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPoint(ServiceProfile serviceProfile, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = serviceProfile.accessPoint().setLinkProtocol(linkProtocol);
            return this;
        }

        public ConnectionBuilder aSideAccessPointServiceProfile(String serviceProfileUuid, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(serviceProfileUuid)
                    .linkProtocol(linkProtocol).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPoint(String virtualDeviceUuid, LinkProtocol linkProtocol, InterfaceType interfaceType, Integer interfaceId) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.VD)
                    .virtualDevice(virtualDeviceUuid)
                    .linkProtocol(linkProtocol)
                    .deviceInterface(interfaceType, interfaceId).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPoint(CloudRouter cloudRouter, LinkProtocol linkProtocol, InterfaceType interfaceType, Integer interfaceId) {
            return aSideAccessPoint(cloudRouter.getUuid(), linkProtocol, interfaceType, interfaceId);
        }

        /**
         * Configures the A-side access point to target a Fabric Cloud Router (by uuid).
         *
         * @param cloudRouterUuid the Cloud Router uuid
         * @return this builder for chaining
         */
        public ConnectionBuilder aSideAccessPointCloudRouter(String cloudRouterUuid) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.CLOUD_ROUTER)
                    .cloudRouter(cloudRouterUuid).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPointCloudRouter(CloudRouter cloudRouter) {
            return aSideAccessPointCloudRouter(cloudRouter.getUuid());
        }

        /**
         * Configures the A-side access point to target a Fabric Network (by uuid).
         *
         * @param networkUuid the network uuid
         * @return this builder for chaining
         */
        /**
         * A-side Fabric Interconnect access point ({@code type IC}, {@code interconnect.uuid}).
         * The spec publishes no create example for it; send with {@link #dryRun()} first.
         *
         * @param interconnectUuid the interconnect uuid
         * @return this builder
         */
        public ConnectionBuilder aSideAccessPointInterconnect(String interconnectUuid) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.IC)
                    .interconnect(interconnectUuid).create();
            return this;
        }

        public ConnectionBuilder aSideAccessPointNetwork(String networkUuid) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.NETWORK)
                    .network(networkUuid).create();
            return this;
        }

        public ConnectionBuilder zSideServiceToken(String serviceTokenUuid) {
            this.zSideServiceToken = new ServiceTokenRef(serviceTokenUuid);
            return this;
        }

        public ConnectionBuilder zSideServiceToken(ServiceToken serviceToken) {
            this.zSideServiceToken = new ServiceTokenRef(serviceToken.getUuid());
            return this;
        }

        public ConnectionBuilder zSideAccessPoint(Port port, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = port.accessPoint().setLinkProtocol(linkProtocol);
            return this;
        }

        public ConnectionBuilder zSideAccessPointPort(String portUuid, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.COLO)
                    .port(portUuid)
                    .linkProtocol(linkProtocol).create();
            return this;
        }

        /**
         * Service-provider z-side from a service profile read model, with {@code location.metroCode}.
         *
         * @param serviceProfile the provider's service profile
         * @param linkProtocol the z-side link protocol, or {@code null}
         * @param metroCode the metro the provider is reached in, e.g. {@code "SV"}; required
         * @return this builder
         */
        public ConnectionBuilder zSideAccessPoint(ServiceProfile serviceProfile, LinkProtocol linkProtocol, String metroCode) {
            return zSideAccessPointServiceProfile(serviceProfile.getUuid(), linkProtocol, metroCode);
        }

        /**
         * Service-provider z-side with {@code location.metroCode}, the shape every
         * service-provider connection example in the Fabric v4 spec uses. The metro is required:
         * without it Fabric answers HTTP 400 EQ-3142501 "Invalid seller metroCode location"
         * (live-confirmed 2026-11-11), so the overload without a metro was removed.
         *
         * @param serviceProfileUuid the provider's service profile
         * @param linkProtocol the z-side link protocol, or {@code null}
         * @param metroCode the metro the provider is reached in, e.g. {@code "SV"}; required
         * @return this builder
         */
        public ConnectionBuilder zSideAccessPointServiceProfile(String serviceProfileUuid, LinkProtocol linkProtocol,
                                                                String metroCode) {
            SimpleAccessPoint.AccessPointBuilder ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(serviceProfileUuid)
                    .metro(requireMetro(metroCode));
            if (linkProtocol != null) {
                ap.linkProtocol(linkProtocol);
            }
            this.zSideAccessPoint = ap.create();
            return this;
        }

        public ConnectionBuilder zSideAccessPoint(String virtualDeviceUuid, LinkProtocol linkProtocol, InterfaceType interfaceType, Integer interfaceId) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.VD)
                    .virtualDevice(virtualDeviceUuid)
                    .linkProtocol(linkProtocol)
                    .deviceInterface(interfaceType, interfaceId).create();
            return this;
        }

        public ConnectionBuilder zSideAccessPoint(CloudRouter cloudRouter, LinkProtocol linkProtocol, InterfaceType interfaceType, Integer interfaceId) {
            return zSideAccessPoint(cloudRouter.getUuid(), linkProtocol, interfaceType, interfaceId);
        }

        /**
         * Configures the Z-side access point to target a Fabric Cloud Router (by uuid).
         *
         * @param cloudRouterUuid the Cloud Router uuid
         * @return this builder for chaining
         */
        public ConnectionBuilder zSideAccessPointCloudRouter(String cloudRouterUuid) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.CLOUD_ROUTER)
                    .cloudRouter(cloudRouterUuid).create();
            return this;
        }

        public ConnectionBuilder zSideAccessPointCloudRouter(CloudRouter cloudRouter) {
            return zSideAccessPointCloudRouter(cloudRouter.getUuid());
        }

        /**
         * Configures the Z-side access point to target a Fabric Network (by uuid).
         *
         * @param networkUuid the network uuid
         * @return this builder for chaining
         */
        public ConnectionBuilder zSideAccessPointNetwork(String networkUuid) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.NETWORK)
                    .network(networkUuid).create();
            return this;
        }

        /**
         * Sets the Z-side access point to a pre-built {@link SimpleAccessPoint}. Use it for
         * access point properties that have no dedicated {@code zSide...} method, such as
         * {@code activationKey} and {@code environment}.
         *
         * <p>The access point is sent as built. This method adds no defaults and performs no
         * validation; a later {@code zSide...} call replaces it.</p>
         *
         * <p><b>Beta</b>: {@code activationKey} and {@code environment} are defined on the Fabric
         * v4 catalog's {@code AccessPoint} schema (fetched 2026-09-21) without a connection-create
         * example. The combination below is a request the schema permits, not one the catalog
         * demonstrates. Send it with {@code dryRun()} first.</p>
         *
         * <pre>{@code
         * SimpleAccessPoint zSide = SimpleAccessPoint.define(AccessPointType.SP)
         *     .serviceProfile(icProfileUuid)
         *     .environment(environmentUuid)
         *     .activationKey(activationKey)
         *     .create();
         *
         * Connection simulated = fabric.connections()
         *     .define(ConnectionType.IP_VC)
         *     .name("cloud-router-to-provider")
         *     .bandwidth(1000)
         *     .aSideAccessPointCloudRouter(cloudRouterUuid)
         *     .zSideAccessPoint(zSide)
         *     .notification("ops@example.com")
         *     .dryRun()
         *     .create();
         * }</pre>
         *
         * @param accessPoint the Z-side access point; must not be {@code null}
         * @return this builder for chaining
         * @throws NullPointerException if {@code accessPoint} is {@code null}
         */
        public ConnectionBuilder zSideAccessPoint(SimpleAccessPoint accessPoint) {
            this.zSideAccessPoint = Objects.requireNonNull(accessPoint, "accessPoint");
            return this;
        }

        /**
         * Configures the Z-side access point from a cloud provider adapter: service profile,
         * authentication key, seller region (when the provider uses one), peering type and the
         * adapter's {@link CloudProviderConnectionAdapter#getMetroCode() metro}.
         *
         * <pre>{@code
         * AwsDirectConnectAdapter<?> adapter = AwsDirectConnectAdapter.of(
         *     "123456789012", "us-east-1", "equinix-aws-profile-uuid", "DC");
         *
         * Connection conn = fabric.connections()
         *     .define(ConnectionType.EVPL_VC)
         *     .name("AWS-Connection")
         *     .bandwidth(100)
         *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
         *     .zSideCloudProvider(adapter, null)
         *     .notification("ops@example.com")
         *     .create();
         * }</pre>
         *
         * @param adapter      the cloud provider adapter; its {@code getMetroCode()} must not be {@code null}
         * @param linkProtocol the z-side link protocol, or {@code null} (the spec's cloud examples send none)
         * @return this builder for chaining
         * @throws IllegalArgumentException when the adapter carries no metro; use
         *         {@link #zSideCloudProvider(CloudProviderConnectionAdapter, LinkProtocol, String)}
         * @see CloudProviderConnectionAdapter
         */
        public ConnectionBuilder zSideCloudProvider(CloudProviderConnectionAdapter<?> adapter, LinkProtocol linkProtocol) {
            return zSideCloudProvider(adapter, linkProtocol, adapter.getMetroCode());
        }

        /**
         * As {@link #zSideCloudProvider(CloudProviderConnectionAdapter, LinkProtocol)} with an explicit
         * z-side {@code location.metroCode}, which overrides the adapter's.
         *
         * @param adapter the cloud provider adapter
         * @param linkProtocol the link protocol, or {@code null}
         * @param metroCode the provider metro, e.g. {@code "SV"}; required
         * @return this builder for chaining
         */
        public ConnectionBuilder zSideCloudProvider(CloudProviderConnectionAdapter<?> adapter, LinkProtocol linkProtocol,
                                                    String metroCode) {
            SimpleAccessPoint.AccessPointBuilder ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .metro(requireMetro(metroCode));
            if (linkProtocol != null) {
                ap.linkProtocol(linkProtocol);
            }
            this.zSideAccessPoint = ap.create();
            return this;
        }

        /**
         * Configures the Z-side access point from a cloud provider adapter, using the adapter's
         * preferred link protocol (if any) and its metro.
         *
         * @param adapter the cloud provider adapter; its {@code getMetroCode()} must not be {@code null}
         * @return this builder for chaining
         * @see #zSideCloudProvider(CloudProviderConnectionAdapter, LinkProtocol)
         */
        public ConnectionBuilder zSideCloudProvider(CloudProviderConnectionAdapter<?> adapter) {
            return zSideCloudProvider(adapter, null, adapter.getMetroCode());
        }

        private static String requireMetro(String metroCode) {
            if (metroCode == null || metroCode.isBlank()) {
                throw new IllegalArgumentException("A service-provider access point needs location.metroCode: "
                        + "Fabric answers HTTP 400 EQ-3142501 without it. Pass the provider metro.");
            }
            return metroCode;
        }

        public ConnectionBuilder geoScope(GeoScopeType geoScope) {
            this.geoScope = geoScope;
            return this;
        }

        public ConnectionBuilder project(String projectId) {
            this.project = new Project(projectId);
            return this;
        }

        public ConnectionBuilder additionalInfo(List<ConnectionSideAdditionalInfo> additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        public ConnectionBuilder additionalInfo(String key, String value) {
            if (this.additionalInfo == null) {
                this.additionalInfo = new ArrayList<>();
            }
            this.additionalInfo.add(new ConnectionSideAdditionalInfo(key, value));
            return this;
        }

        public ConnectionBuilder marketplaceSubscription(String subscriptionUuid) {
            this.marketplaceSubscription = new MarketplaceSubscriptionRef(subscriptionUuid);
            return this;
        }

        public ConnectionBuilder dryRun() {
            this.dryRun = true;
            return this;
        }

         public ConnectionBuilder notification(NotificationType type, String emailAddress) {
            if(notifications.stream().noneMatch(o -> o.getType().equals(type))) {
                notifications.add(new Notification(type, Collections.singletonList(emailAddress)));
            }
            else {
                Objects.requireNonNull(notifications.stream().filter(o -> o.getType().equals(type)).findFirst().orElse(null)).addEmail(emailAddress);
            }
            return this;
        }

        public ConnectionBuilder notification(String emailAddress) {
            if(notifications.stream().noneMatch(o -> o.getType().equals(NotificationType.ALL))) {
                notifications.add(new Notification(NotificationType.ALL, Collections.singletonList(emailAddress)));
            }
            else {
                Objects.requireNonNull(notifications.stream().filter(o -> o.getType().equals(NotificationType.ALL)).findFirst().orElse(null)).addEmail(emailAddress);
            }
            return this;
        }

         public ConnectionBuilder notification(List<Notification> notifications) {
            this.notifications = notifications;
            return this;
        }

        public Connection create() {
            ConnectionCreatorJson connectionCreatorJson = new ConnectionCreatorJson(this);
            ConnectionClientImpl clientImpl = (ConnectionClientImpl) ConnectionOperator.this.getServiceClient();
            ConnectionJson connectionJson = dryRun
                    ? clientImpl.dryRunCreate(connectionCreatorJson)
                    : clientImpl.create(connectionCreatorJson);
            return new ConnectionWrapper(connectionJson, ConnectionOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent updater for an existing connection. Typed setters record RFC&nbsp;6902 replace
     * operations (the API accepts a JSON Patch array at {@code PATCH /connections/{uuid}}); use
     * {@link #patch(PatchOperation)} for any path not covered by a typed setter (e.g.
     * notifications, A-side migration). Call {@link #save()} to apply, optionally after
     * {@link #dryRun()} to only validate the update.
     */
    public class ConnectionUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();
        private boolean dryRun;

        protected ConnectionUpdater(String uuid) {
            this.uuid = uuid;
        }

        public ConnectionUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        public ConnectionUpdater bandwidth(Integer bandwidth) {
            operations.add(PatchOperation.replace("/bandwidth", bandwidth));
            return this;
        }

        /**
         * Replaces the order term length ({@code /order/termLength}), e.g. to upgrade an on-demand
         * connection to a term-based connection (Fabric release R2025.5).
         *
         * @param termLength the new term length in months (for example 12, 24 or 36)
         * @return this updater
         */
        public ConnectionUpdater termLength(Integer termLength) {
            if (termLength == null || termLength < 1) {
                throw new IllegalArgumentException("termLength must be a positive number of months");
            }
            operations.add(PatchOperation.replace("/order/termLength", termLength));
            return this;
        }

        /**
         * Edits the VLAN tag of an EVPL connection side (Fabric release "EVPL VLAN edit"): replaces
         * {@code /{aSide|zSide}/accessPoint/linkProtocol/vlanTag}.
         *
         * <p>Provenance: the release notes announce the feature, but spec 4.34.0 publishes no example
         * for it; the path follows the connection's own {@code linkProtocol} shape and is unverified
         * live (no net-new connection may be created for testing). Run it with {@link #dryRun()}
         * first.</p>
         *
         * @param side the side whose VLAN changes
         * @param vlanTag the new DOT1Q VLAN tag (2-4092)
         * @return this updater
         */
        public ConnectionUpdater vlanTag(Side side, int vlanTag) {
            requireVlan(vlanTag, "vlanTag");
            operations.add(PatchOperation.replace(sidePath(side) + "/accessPoint/linkProtocol/vlanTag", vlanTag));
            return this;
        }

        /**
         * Edits the QinQ VLAN tags of a connection side: replaces
         * {@code .../linkProtocol/vlanSTag} and {@code .../linkProtocol/vlanCTag}. Same provenance and
         * dry-run advice as {@link #vlanTag(Side, int)}.
         *
         * @param side the side whose VLANs change
         * @param vlanSTag the new outer (service) tag
         * @param vlanCTag the new inner (customer) tag
         * @return this updater
         */
        public ConnectionUpdater vlanTags(Side side, int vlanSTag, int vlanCTag) {
            requireVlan(vlanSTag, "vlanSTag");
            requireVlan(vlanCTag, "vlanCTag");
            String base = sidePath(side) + "/accessPoint/linkProtocol/";
            operations.add(PatchOperation.replace(base + "vlanSTag", vlanSTag));
            operations.add(PatchOperation.replace(base + "vlanCTag", vlanCTag));
            return this;
        }

        private String sidePath(Side side) {
            if (side == Side.A_Side) {
                return "/aSide";
            }
            if (side == Side.Z_Side) {
                return "/zSide";
            }
            throw new IllegalArgumentException("side must be A_Side or Z_Side");
        }

        private void requireVlan(int tag, String name) {
            if (tag < 2 || tag > 4092) {
                throw new IllegalArgumentException(name + " must be 2..4092: " + tag);
            }
        }

        public ConnectionUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Marks this update as a dry run: {@link #save()} sends the same JSON Patch to
         * {@code PATCH /fabric/v4/connections/{uuid}} with the {@code dryRun=true} query
         * parameter — per the Fabric v4 spec, an "option to verify that API calls will succeed".
         * Nothing is persisted: the API responds {@code 200} (the real update responds
         * {@code 202}) with a simulation of the post-update connection, which {@code save()}
         * returns.
         *
         * @return this updater
         */
        public ConnectionUpdater dryRun() {
            this.dryRun = true;
            return this;
        }

        public Connection save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            ConnectionClientImpl clientImpl = (ConnectionClientImpl) ConnectionOperator.this.getServiceClient();
            ConnectionJson connectionJson = dryRun
                    ? clientImpl.dryRunUpdate(uuid, operations)
                    : clientImpl.update(uuid, operations);
            return new ConnectionWrapper(connectionJson, ConnectionOperator.this.getServiceClient());
        }
    }
}
