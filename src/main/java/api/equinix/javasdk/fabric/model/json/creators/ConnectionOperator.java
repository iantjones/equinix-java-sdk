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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ConnectionClientImpl;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderConnectionAdapter;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;
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

    public BatchConnectionBuilder batch() {
        return new BatchConnectionBuilder();
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
        private MinimalServiceToken aSideServiceToken;
        private MinimalServiceToken zSideServiceToken;
        private List<Notification> notifications = Collections.singletonList(new Notification(NotificationType.ALL, new ArrayList<>()));
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
            this.redundancy = new Redundancy(group, priority);
            return this;
        }

        public ConnectionBuilder aSideServiceToken(ServiceToken serviceToken) {
            this.aSideServiceToken = new MinimalServiceToken(serviceToken.getUuid());
            return this;
        }

        public ConnectionBuilder aSideServiceToken(String serviceTokenUuid) {
            this.aSideServiceToken = new MinimalServiceToken(serviceTokenUuid);
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

        public ConnectionBuilder zSideServiceToken(String serviceTokenUuid) {
            this.zSideServiceToken = new MinimalServiceToken(serviceTokenUuid);
            return this;
        }

        public ConnectionBuilder zSideServiceToken(ServiceToken serviceToken) {
            this.zSideServiceToken = new MinimalServiceToken(serviceToken.getUuid());
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

        public ConnectionBuilder zSideAccessPointService(String serviceProfileUuid, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(serviceProfileUuid)
                    .linkProtocol(linkProtocol).create();
            return this;
        }

        public ConnectionBuilder zSideAccessPoint(ServiceProfile serviceProfile, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = serviceProfile.accessPoint().setLinkProtocol(linkProtocol);
            return this;
        }

        public ConnectionBuilder zSideAccessPointServiceProfile(String serviceProfileUuid, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile(serviceProfileUuid)
                    .linkProtocol(linkProtocol).create();
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
         * Configures the Z-side access point using a cloud provider adapter.
         *
         * <p>This is the primary method for creating connections to cloud providers like
         * AWS Direct Connect, Azure ExpressRoute, Google Cloud Interconnect, and Oracle FastConnect.
         * The adapter extracts the service profile UUID, authentication key, seller region,
         * and optional peering type from the cloud provider SDK object.</p>
         *
         * <p>A link protocol must be specified separately (cloud providers typically require DOT1Q).</p>
         *
         * <pre>{@code
         * AwsDirectConnectAdapter<?> adapter = AwsDirectConnectAdapter.of(
         *     "123456789012", "us-east-1", "equinix-aws-profile-uuid");
         *
         * Connection conn = fabric.connections()
         *     .define(ConnectionType.EVPL_VC)
         *     .name("AWS-Connection")
         *     .bandwidth(100)
         *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
         *     .zSideCloudProvider(adapter, LinkProtocol.dot1q().vlanTag(1000).create())
         *     .notification("ops@example.com")
         *     .create();
         * }</pre>
         *
         * @param adapter      the cloud provider adapter containing connection parameters
         * @param linkProtocol the link protocol (typically DOT1Q with a VLAN tag)
         * @return this builder for chaining
         * @see CloudProviderConnectionAdapter
         */
        public ConnectionBuilder zSideCloudProvider(CloudProviderConnectionAdapter<?> adapter, LinkProtocol linkProtocol) {
            this.zSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(linkProtocol)
                    .create();
            return this;
        }

        /**
         * Configures the Z-side access point using a cloud provider adapter with its preferred link protocol.
         *
         * <p>Uses the adapter's {@link CloudProviderConnectionAdapter#getPreferredLinkProtocol()} if available.
         * If the adapter does not specify a preferred link protocol, the access point will be created
         * without one and the API may reject the request.</p>
         *
         * @param adapter the cloud provider adapter containing connection parameters
         * @return this builder for chaining
         * @see #zSideCloudProvider(CloudProviderConnectionAdapter, LinkProtocol)
         */
        public ConnectionBuilder zSideCloudProvider(CloudProviderConnectionAdapter<?> adapter) {
            SimpleAccessPoint.AccessPointBuilder apBuilder = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter);
            this.zSideAccessPoint = apBuilder.create();
            return this;
        }

        /**
         * Configures the A-side access point using a cloud provider adapter.
         *
         * <p>While cloud providers are typically configured on the Z-side, this method is provided
         * for scenarios where the cloud provider connection serves as the A-side access point.</p>
         *
         * @param adapter      the cloud provider adapter containing connection parameters
         * @param linkProtocol the link protocol
         * @return this builder for chaining
         */
        public ConnectionBuilder aSideCloudProvider(CloudProviderConnectionAdapter<?> adapter, LinkProtocol linkProtocol) {
            this.aSideAccessPoint = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(linkProtocol)
                    .create();
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

    @Getter(AccessLevel.PACKAGE)
    public class BatchConnectionBuilder {

        private final List<ConnectionCreatorJson> connections = new ArrayList<>();

        public BatchConnectionBuilder addConnection(ConnectionBuilder connectionBuilder) {
            connections.add(new ConnectionCreatorJson(connectionBuilder));
            return this;
        }

        public List<Connection> createBatch() {
            return ((ConnectionClientImpl) ConnectionOperator.this.getServiceClient()).batch(this.connections);
        }
    }
}
