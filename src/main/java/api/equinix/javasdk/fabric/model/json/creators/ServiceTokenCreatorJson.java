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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.serializers.LinkProtocolTypeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;

import java.util.List;

/**
 * <p>ServiceTokenJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class ServiceTokenCreatorJson {

    @Getter static TypeReference<Page<ServiceToken, ServiceTokenCreatorJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<ServiceTokenCreatorJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ServiceTokenCreatorJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("type")
    private ServiceTokenType type;

    @JsonProperty("expiry")
    private Integer expiry;

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

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AccessPointSelector {

        @JsonProperty("type")
        private AccessPointType type;

        @JsonProperty("port")
        private BasicPort port;

        @JsonProperty("linkProtocol")
        private LinkProtocol linkProtocol;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class BasicPort {

        @JsonProperty("uuid")
        private String uuid;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class LinkProtocol {

        //@JsonSerialize(using = LinkProtocolTypeSerializer.class)
        @JsonProperty("type")
        private LinkProtocolType type;

        @JsonProperty("vlanTag")
        private Integer vlanTag;

        @JsonProperty("vlanCTag")
        private Integer vlanCTag;

        @JsonProperty("vlanSTag")
        private Integer vlanSTag;
    }

    /**
     * <p>Constructor for ServiceTokenCreatorJson.</p>
     *
     * @param serviceTokenBuilder a {@link api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator.ServiceTokenBuilder} object.
     */
    public ServiceTokenCreatorJson(ServiceTokenOperator.ServiceTokenBuilder serviceTokenBuilder) {
        this.type = serviceTokenBuilder.getServiceTokenType();
        this.expiry = serviceTokenBuilder.getExpiry();

        LinkProtocol linkProtocol = new LinkProtocol(serviceTokenBuilder.getLinkProtocolType(), serviceTokenBuilder.getVLanTag(),
                serviceTokenBuilder.getVLanCTag(), serviceTokenBuilder.getVLanSTag());

        BasicPort port = new BasicPort(serviceTokenBuilder.getPortUuid());

        AccessPointSelector accessPointSelector = new AccessPointSelector(serviceTokenBuilder.getAccessPointType(), port, linkProtocol);

        ConnectionConfig connectionConfig = new ConnectionConfig(List.of(accessPointSelector));

        Connection connection = new Connection();
        connection.setType(serviceTokenBuilder.getConnectionType());
        connection.setAllowRemoteConnection(serviceTokenBuilder.getAllowRemoteConnection());
        connection.setAllowCustomBandwidth(serviceTokenBuilder.getAllowCustomBandwidth());
        connection.setBandwidthLimit(serviceTokenBuilder.getBandwidthLimit());
        connection.setIssuerSide(serviceTokenBuilder.getIssuerSide());

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
