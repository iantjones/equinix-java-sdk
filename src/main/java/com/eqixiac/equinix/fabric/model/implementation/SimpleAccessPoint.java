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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.fabric.enums.AccessPointRole;
import com.eqixiac.equinix.fabric.enums.AccessPointType;
import com.eqixiac.equinix.fabric.enums.InterfaceType;
import com.eqixiac.equinix.fabric.enums.LinkProtocolType;
import com.eqixiac.equinix.fabric.enums.PeeringType;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class SimpleAccessPoint {

    @JsonProperty("type")
    private AccessPointType type;

    /**
     * Deprecated in the spec but still accepted on service-token selectors.
     */
    @JsonProperty("hideAssetInfo")
    private Boolean hideAssetInfo;

    @JsonProperty("port")
    private SimplifiedMetadataEntity port;

    @JsonProperty("profile")
    private ServiceProfileRef profile;

    @JsonProperty("virtualDevice")
    private SimplifiedVirtualDevice virtualDevice;

    @JsonProperty("linkProtocol")
    private LinkProtocol linkProtocol;

    @JsonProperty("interface")
    private Interface deviceInterface;

    @JsonProperty("sellerRegion")
    private String sellerRegion;

    @JsonProperty("authenticationKey")
    private String authenticationKey;

    @JsonProperty("peeringType")
    private PeeringType peeringType;

    @JsonProperty("router")
    private CloudRouterRef router;

    @JsonProperty("network")
    private SimplifiedTokenNetwork network;

    @JsonProperty("virtualNetwork")
    private VirtualNetwork virtualNetwork;

    @JsonProperty("interconnection")
    private MetalInterconnection interconnection;

    @JsonProperty("role")
    private AccessPointRole role;

    protected SimpleAccessPoint(AccessPointBuilder accessPointBuilder) {
        this.type = accessPointBuilder.type;
        this.hideAssetInfo = accessPointBuilder.hideAssetInfo;
        this.port = accessPointBuilder.port;
        this.profile = accessPointBuilder.profile;
        this.virtualDevice = accessPointBuilder.virtualDevice;
        this.linkProtocol = accessPointBuilder.linkProtocol;
        this.deviceInterface = accessPointBuilder.deviceInterface;
        this.sellerRegion = accessPointBuilder.sellerRegion;
        this.authenticationKey = accessPointBuilder.authenticationKey;
        this.peeringType = accessPointBuilder.peeringType;
        this.router = accessPointBuilder.router;
        this.network = accessPointBuilder.network;
        this.virtualNetwork = accessPointBuilder.virtualNetwork;
        this.interconnection = accessPointBuilder.interconnection;
        this.role = accessPointBuilder.role;
    }

    public SimpleAccessPoint setLinkProtocol(LinkProtocol linkProtocol) {
        this.linkProtocol = linkProtocol;
        return this;
    }

    public SimpleAccessPoint setDeviceInterface(Interface deviceInterface) {
        this.deviceInterface = deviceInterface;
        return this;
    }

    public static AccessPointBuilder define(AccessPointType type) {
        return new AccessPointBuilder(type);
    }

    public static SideWrapper simpleClone(AccessPoint accessPoint, Side connectionSide) {
        AccessPointBuilder apb = new AccessPointBuilder(accessPoint);
        SideWrapper sideWrapper = new SideWrapper();
        if (connectionSide == Side.A_Side) {
            sideWrapper.setASide(apb.create());
        } else if (connectionSide == Side.Z_Side) {
            sideWrapper.setZSide(apb.create());
        }

        return sideWrapper;
    }

    public static class AccessPointBuilder {

        private final AccessPointType type;

        private Boolean hideAssetInfo;

        private SimplifiedMetadataEntity port;

        private ServiceProfileRef profile;

        private SimplifiedVirtualDevice virtualDevice;

        private LinkProtocol linkProtocol;

        private Interface deviceInterface;

        private String sellerRegion;

        private String authenticationKey;

        private PeeringType peeringType;

        private CloudRouterRef router;

        private SimplifiedTokenNetwork network;

        private VirtualNetwork virtualNetwork;

        private MetalInterconnection interconnection;

        private AccessPointRole role;

        protected AccessPointBuilder(AccessPointType type) {
            this.type = type;
        }

        protected AccessPointBuilder(AccessPoint accessPoint) {
            this.type = accessPoint.getType();
            this.port = accessPoint.getPort() != null ? new SimplifiedMetadataEntity(accessPoint.getPort().getUuid()) : null;
            this.profile = accessPoint.getProfile() != null ? new ServiceProfileRef(accessPoint.getProfile().getUuid()) : null;
            this.virtualDevice = accessPoint.getVirtualDevice() != null ? new SimplifiedVirtualDevice(accessPoint.getVirtualDevice().getUuid()) : null;
            this.linkProtocol = accessPoint.getLinkProtocol() != null ? accessPoint.getLinkProtocol() : null;
            this.deviceInterface = accessPoint.getInterface() != null ? accessPoint.getInterface() : null;
        }

        public AccessPointBuilder port(String portUuid) {
            this.port = new SimplifiedMetadataEntity(portUuid);
            return this;
        }

        /**
         * Hides asset information from the token recipient. Deprecated in the spec but still accepted.
         *
         * @param hideAssetInfo whether to hide asset information
         * @return this builder for chaining
         */
        public AccessPointBuilder hideAssetInfo(Boolean hideAssetInfo) {
            this.hideAssetInfo = hideAssetInfo;
            return this;
        }

        public AccessPointBuilder port(Port port) {
            return port(port.getUuid());
        }

        public AccessPointBuilder serviceProfile(String serviceProfileUuid) {
            this.profile = new ServiceProfileRef(serviceProfileUuid);
            return this;
        }

        public AccessPointBuilder serviceProfile(ServiceProfile serviceProfile) {
            return serviceProfile(serviceProfile.getUuid());
        }

        public AccessPointBuilder virtualDevice(String virtualDeviceUuid) {
            this.virtualDevice = new SimplifiedVirtualDevice(virtualDeviceUuid);
            return this;
        }

        public AccessPointBuilder linkProtocol(LinkProtocol linkProtocol) {
            this.linkProtocol = linkProtocol;
            return this;
        }

        public AccessPointBuilder linkProtocol(LinkProtocolType linkProtocolType, Integer vlanTag, Integer vlanCTag, Integer vlanSTag) {
            this.linkProtocol = switch(linkProtocolType) {
                case DOT1Q -> LinkProtocol.dot1q().vlanTag(vlanTag).create();
                case QINQ -> LinkProtocol.qinq().vlanCTag(vlanCTag).vlanSTag(vlanSTag).create();
                case UNTAGGED -> LinkProtocol.untagged().create();
                default -> throw new IllegalArgumentException(
                        "No typed builder for link protocol type " + linkProtocolType
                                + "; use linkProtocol(LinkProtocol) instead.");
            };

            return this;
        }

        public AccessPointBuilder deviceInterface(InterfaceType interfaceType, Integer interfaceId) {
            this.deviceInterface = new Interface(interfaceId, interfaceType);
            return this;
        }

        public AccessPointBuilder sellerRegion(String sellerRegion) {
            this.sellerRegion = sellerRegion;
            return this;
        }

        public AccessPointBuilder authenticationKey(String authenticationKey) {
            this.authenticationKey = authenticationKey;
            return this;
        }

        public AccessPointBuilder peeringType(PeeringType peeringType) {
            this.peeringType = peeringType;
            return this;
        }

        /**
         * Targets a Fabric Cloud Router (by uuid) as this access point. Used to build
         * Fabric Cloud Router connections.
         *
         * @param cloudRouterUuid the Cloud Router uuid
         * @return this builder for chaining
         */
        public AccessPointBuilder cloudRouter(String cloudRouterUuid) {
            this.router = new CloudRouterRef(cloudRouterUuid);
            return this;
        }

        public AccessPointBuilder cloudRouter(CloudRouter cloudRouter) {
            return cloudRouter(cloudRouter.getUuid());
        }

        /**
         * Targets a Fabric Network (by uuid) as this access point. Used to build
         * network-type (E-LAN / E-Tree) connections.
         *
         * @param networkUuid the network uuid
         * @return this builder for chaining
         */
        public AccessPointBuilder network(String networkUuid) {
            this.network = new SimplifiedTokenNetwork(networkUuid);
            return this;
        }

        public AccessPointBuilder network(Network network) {
            return network(network.getUuid());
        }

        public AccessPointBuilder virtualNetwork(String virtualNetworkUuid) {
            this.virtualNetwork = new VirtualNetwork(virtualNetworkUuid);
            return this;
        }

        public AccessPointBuilder interconnection(String interconnectionUuid) {
            this.interconnection = new MetalInterconnection(interconnectionUuid);
            return this;
        }

        public AccessPointBuilder role(AccessPointRole role) {
            this.role = role;
            return this;
        }

        /**
         * Configures this access point using a {@link CloudProviderConnectionAdapter}.
         *
         * <p>Extracts the service profile UUID, authentication key, seller region, and
         * optional peering type from the adapter and applies them to this access point builder.</p>
         *
         * @param adapter the cloud provider adapter to extract connection parameters from
         * @return this builder for chaining
         */
        public AccessPointBuilder fromCloudProvider(CloudProviderConnectionAdapter<?> adapter) {
            this.profile = new ServiceProfileRef(adapter.getServiceProfileUuid());
            this.authenticationKey = adapter.getAuthenticationKey();
            this.sellerRegion = adapter.getSellerRegion();
            if (adapter.getPreferredPeeringType() != null) {
                this.peeringType = adapter.getPreferredPeeringType();
            }
            if (adapter.getPreferredLinkProtocol() != null) {
                this.linkProtocol = adapter.getPreferredLinkProtocol();
            }
            return this;
        }

        public SimpleAccessPoint create() {
            return new SimpleAccessPoint(this);
        }
    }

    @Setter(AccessLevel.PRIVATE)
    protected static class SideWrapper {
        @JsonProperty("aSide")
        private SimpleAccessPoint aSide;
        @JsonProperty("zSide")
        private SimpleAccessPoint zSide;
    }
}
