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

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
public class ServiceTokenCreatorJson {

    @JsonProperty("type")
    private ServiceTokenType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("expiry")
    private Integer expiry;

    @JsonProperty("expirationDateTime")
    private String expirationDateTime;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("connection")
    private Connection connection;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @Setter(AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Connection {

        @JsonProperty("type")
        private ConnectionType type;

        @JsonProperty("issuerSide")
        private Side issuerSide;

        @JsonProperty("allowRemoteConnection")
        private Boolean allowRemoteConnection;

        @JsonProperty("bandwidthLimit")
        private Integer bandwidthLimit;

        @JsonProperty("allowCustomBandwidth")
        private Boolean allowCustomBandwidth;

        @JsonProperty("supportedBandwidths")
        private List<Integer> supportedBandwidths;

        @JsonProperty("aSide")
        private ConnectionConfig aSide;

        @JsonProperty("zSide")
        private ConnectionConfig zSide;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ConnectionConfig {

        @JsonProperty("accessPointSelectors")
        List<AccessPointSelector> accessPointSelectors;
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AccessPointSelector {

        @JsonProperty("type")
        private AccessPointType type;

        /** Deprecated in the spec but still accepted. */
        @JsonProperty("hideAssetInfo")
        private Boolean hideAssetInfo;

        @JsonProperty("port")
        private PortSummary port;

        @JsonProperty("linkProtocol")
        private LinkProtocol linkProtocol;

        @JsonProperty("virtualDevice")
        private VirtualDeviceSummary virtualDevice;

        @JsonProperty("interface")
        private VirtualDeviceInterface vdInterface;

        @JsonProperty("network")
        private NetworkSummary network;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class PortSummary {

        @JsonProperty("uuid")
        private String uuid;
    }

    /** Spec schema {@code SimplifiedVirtualDevice} (writable members; {@code type} enum: {@code EDGE}). */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class VirtualDeviceSummary {

        @JsonProperty("type")
        private VirtualDeviceType type;

        @JsonProperty("uuid")
        private String uuid;
    }

    /** Spec schema {@code VirtualDeviceInterface}. */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class VirtualDeviceInterface {

        @JsonProperty("type")
        private String type;

        @JsonProperty("id")
        private Integer id;
    }

    /** Spec schema {@code SimplifiedTokenNetwork} (writable members). */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class NetworkSummary {

        @JsonProperty("uuid")
        private String uuid;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class LinkProtocol {

        @JsonProperty("type")
        private LinkProtocolType type;

        @JsonProperty("vlanTag")
        private Integer vlanTag;

        @JsonProperty("vlanCTag")
        private Integer vlanCTag;

        @JsonProperty("vlanSTag")
        private Integer vlanSTag;
    }

    public ServiceTokenCreatorJson(ServiceTokenOperator.ServiceTokenBuilder serviceTokenBuilder) {
        this.type = serviceTokenBuilder.getServiceTokenType();
        this.name = serviceTokenBuilder.getName();
        this.description = serviceTokenBuilder.getDescription();
        this.expiry = serviceTokenBuilder.getExpiry();

        if (serviceTokenBuilder.getExpirationDateTime() != null) {
            this.expirationDateTime = serviceTokenBuilder.getExpirationDateTime().format(Constants.queryParamFormatter);
        }

        if (serviceTokenBuilder.getProjectId() != null) {
            this.project = new Project(serviceTokenBuilder.getProjectId());
        }

        AccessPointSelector accessPointSelector = new AccessPointSelector();
        accessPointSelector.type = serviceTokenBuilder.getAccessPointType();
        accessPointSelector.hideAssetInfo = serviceTokenBuilder.getHideAssetInfo();

        if (serviceTokenBuilder.getPortUuid() != null) {
            accessPointSelector.port = new PortSummary(serviceTokenBuilder.getPortUuid());
        }

        if (serviceTokenBuilder.getLinkProtocolType() != null) {
            accessPointSelector.linkProtocol = new LinkProtocol(serviceTokenBuilder.getLinkProtocolType(),
                    serviceTokenBuilder.getVLanTag(), serviceTokenBuilder.getVLanCTag(), serviceTokenBuilder.getVLanSTag());
        }

        if (serviceTokenBuilder.getVirtualDeviceUuid() != null) {
            accessPointSelector.virtualDevice = new VirtualDeviceSummary(VirtualDeviceType.EDGE, serviceTokenBuilder.getVirtualDeviceUuid());
        }

        if (serviceTokenBuilder.getInterfaceId() != null) {
            accessPointSelector.vdInterface = new VirtualDeviceInterface("NETWORK", serviceTokenBuilder.getInterfaceId());
        }

        if (serviceTokenBuilder.getNetworkUuid() != null) {
            accessPointSelector.network = new NetworkSummary(serviceTokenBuilder.getNetworkUuid());
        }

        ConnectionConfig connectionConfig = new ConnectionConfig(List.of(accessPointSelector));

        Connection connection = new Connection();
        connection.setType(serviceTokenBuilder.getConnectionType());
        connection.setAllowRemoteConnection(serviceTokenBuilder.getAllowRemoteConnection());
        connection.setAllowCustomBandwidth(serviceTokenBuilder.getAllowCustomBandwidth());
        connection.setBandwidthLimit(serviceTokenBuilder.getBandwidthLimit());
        connection.setIssuerSide(serviceTokenBuilder.getIssuerSide());
        connection.setSupportedBandwidths(serviceTokenBuilder.getSupportedBandwidths());

        if(serviceTokenBuilder.getIssuerSide() == Side.A_Side) {
            connection.setASide(connectionConfig);
        }
        else if(serviceTokenBuilder.getIssuerSide() == Side.Z_Side) {
            connection.setZSide(connectionConfig);
        }

        if(serviceTokenBuilder.getEmails() != null && serviceTokenBuilder.getEmails().size() > 0) {
            this.notifications = List.of(new Notification(NotificationType.NOTIFICATION, serviceTokenBuilder.getEmails()));
        }
        this.connection = connection;
    }
}
