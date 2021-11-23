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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceTokenClientImpl;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>ServiceTokenOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceTokenOperator extends ResourceImpl<ServiceToken> {

    @Getter
    private final Pageable<ServiceToken> serviceClient;

    /**
     * <p>Constructor for ServiceTokenOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ServiceTokenOperator(Pageable<ServiceToken> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param issuerSide a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator.ServiceTokenBuilder} object.
     */
    public ServiceTokenBuilder create(Side issuerSide) {
        return new ServiceTokenBuilder(issuerSide);
    }

    @Getter
    public class ServiceTokenBuilder {

        private ServiceTokenType serviceTokenType;
        private Integer expiry;

        private final Side issuerSide;

        private ConnectionType connectionType;
        private Boolean allowRemoteConnection = false;
        private Boolean allowCustomBandwidth = false;
        private Integer bandwidthLimit;

        private AccessPointType accessPointType;
        private String portUuid;

        private LinkProtocolType linkProtocolType;
        private Integer vLanTag;
        private Integer vLanCTag;
        private Integer vLanSTag;

        private List<String> emails;

        protected ServiceTokenBuilder(Side issuerSide) {
            this.issuerSide = issuerSide;
        }

        public ServiceTokenOperator.ServiceTokenBuilder ofType(ServiceTokenType serviceTokenType) {
            this.serviceTokenType = serviceTokenType;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder withExpiry(Integer expiry) {
            this.expiry = expiry;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder forConnectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder allowRemoteConnection(Boolean allowRemoteConnection) {
            this.allowRemoteConnection = allowRemoteConnection;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder allowCustomBandwidth(Boolean allowCustomBandwidth) {
            this.allowCustomBandwidth = allowCustomBandwidth;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder withBandwidthLimit(Integer bandwidthLimit) {
            this.bandwidthLimit = bandwidthLimit;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder forAccessPointType(AccessPointType accessPointType) {
            this.accessPointType = accessPointType;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder onPortUuid(String portUuid) {
            this.portUuid = portUuid;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder usingProtocolDot1q(Integer vLanTag) {
            this.linkProtocolType = LinkProtocolType.DOT1Q;
            this.vLanTag = vLanTag;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder usingProtocolQinQ(Integer vLanCTag, Integer vlanSTag) {
            this.linkProtocolType = LinkProtocolType.QINQ;
            this.vLanCTag = vLanCTag;
            this.vLanSTag = vlanSTag;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder withNotificationEmail(String emailAddress) {
            if(this.emails == null) {
                this.emails = new ArrayList<>();
            }

            this.emails.add(emailAddress);
            return this;
        }

        public ServiceToken create() {
            ServiceTokenCreatorJson serviceTokenCreatorJson = new ServiceTokenCreatorJson(this);
            ServiceTokenJson serviceTokenJson = ((ServiceTokenClientImpl)ServiceTokenOperator.this.getServiceClient()).create(serviceTokenCreatorJson);
            return new ServiceTokenWrapper(serviceTokenJson, ServiceTokenOperator.this.getServiceClient());
        }
    }
}
