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

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceTokenClientImpl;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class ServiceTokenOperator extends ResourceImpl<ServiceToken> {

    @Getter
    private final Pageable<ServiceToken> serviceClient;

    public ServiceTokenOperator(Pageable<ServiceToken> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ServiceTokenBuilder create(Side issuerSide) {
        return new ServiceTokenBuilder(issuerSide);
    }

    /**
     * Begins a fluent PATCH update of an existing service token, identified by uuid.
     *
     * @param uuid the uuid of the service token to update
     * @return a {@link ServiceTokenUpdater}
     */
    public ServiceTokenUpdater update(String uuid) {
        return new ServiceTokenUpdater(uuid);
    }

    /**
     * Fluent builder for PATCH-updating an existing service token. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH} and returns
     * the refreshed model.
     */
    public class ServiceTokenUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected ServiceTokenUpdater(String uuid) {
            this.uuid = uuid;
        }

        public ServiceTokenUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        public ServiceTokenUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        public ServiceTokenUpdater expiry(Integer expiry) {
            operations.add(PatchOperation.replace("/expiry", expiry));
            return this;
        }

        public ServiceTokenUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        public ServiceToken save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            ServiceTokenJson serviceTokenJson = ((ServiceTokenClientImpl) ServiceTokenOperator.this.getServiceClient()).update(uuid, operations);
            return new ServiceTokenWrapper(serviceTokenJson, ServiceTokenOperator.this.getServiceClient());
        }
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
        private boolean dryRun;

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

        public ServiceTokenOperator.ServiceTokenBuilder onPort(Port port) {
            return onPortUuid(port.getUuid());
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

        public ServiceTokenOperator.ServiceTokenBuilder dryRun() {
            this.dryRun = true;
            return this;
        }

        public ServiceToken create() {
            ServiceTokenCreatorJson serviceTokenCreatorJson = new ServiceTokenCreatorJson(this);
            ServiceTokenClientImpl clientImpl = (ServiceTokenClientImpl) ServiceTokenOperator.this.getServiceClient();
            ServiceTokenJson serviceTokenJson = dryRun
                    ? clientImpl.dryRunCreate(serviceTokenCreatorJson)
                    : clientImpl.create(serviceTokenCreatorJson);
            return new ServiceTokenWrapper(serviceTokenJson, ServiceTokenOperator.this.getServiceClient());
        }
    }
}
