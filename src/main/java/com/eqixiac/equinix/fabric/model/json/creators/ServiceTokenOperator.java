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
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.fabric.client.internal.implementation.ServiceTokenClientImpl;
import com.eqixiac.equinix.fabric.enums.*;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.ServiceToken;
import com.eqixiac.equinix.fabric.model.json.ServiceTokenJson;
import com.eqixiac.equinix.fabric.model.wrappers.ServiceTokenWrapper;
import lombok.Getter;

import java.time.LocalDateTime;
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
     * the refreshed model. Call {@link #dryRun()} first to only validate the update.
     */
    public class ServiceTokenUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();
        private boolean dryRun;

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

        /**
         * Marks this update as a dry run: {@link #save()} sends the same change-operations
         * {@code PATCH} to {@code /fabric/v4/serviceTokens/{uuid}} with the {@code dryRun=true}
         * query parameter — per the Fabric v4 spec, an "option to verify that API calls will
         * succeed". Nothing is persisted: the API responds {@code 200} with the
         * validated/simulated token entity, which {@code save()} returns.
         *
         * @return this builder
         */
        public ServiceTokenUpdater dryRun() {
            this.dryRun = true;
            return this;
        }

        public ServiceToken save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            ServiceTokenClientImpl clientImpl = (ServiceTokenClientImpl) ServiceTokenOperator.this.getServiceClient();
            ServiceTokenJson serviceTokenJson = dryRun
                    ? clientImpl.dryRunUpdate(uuid, operations)
                    : clientImpl.update(uuid, operations);
            return new ServiceTokenWrapper(serviceTokenJson, ServiceTokenOperator.this.getServiceClient());
        }
    }

    @Getter
    public class ServiceTokenBuilder {

        private ServiceTokenType serviceTokenType;
        private String name;
        private String description;
        private Integer expiry;
        private LocalDateTime expirationDateTime;
        private String projectId;

        private final Side issuerSide;

        private ConnectionType connectionType;
        private Boolean allowRemoteConnection = false;
        private Boolean allowCustomBandwidth = false;
        private Integer bandwidthLimit;
        private List<Integer> supportedBandwidths;

        private AccessPointType accessPointType;
        private String portUuid;
        private Boolean hideAssetInfo;

        private String virtualDeviceUuid;
        private Integer interfaceId;
        private String networkUuid;

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

        public ServiceTokenOperator.ServiceTokenBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder withExpiry(Integer expiry) {
            this.expiry = expiry;
            return this;
        }

        /**
         * Sets the expiration date and time of the service token.
         *
         * <p>UTC contract: the {@code LocalDateTime} input is UTC wall clock (matching every
         * timestamp the SDK returns, including {@code ServiceToken.getExpirationDateTime()});
         * it is sent verbatim with a {@code 'Z'} designator. Use
         * {@code LocalDateTime.now(ZoneOffset.UTC)} as the base for relative expirations.</p>
         *
         * @param expirationDateTime the expiration as UTC wall clock, sent as {@code expirationDateTime}
         * @return this builder
         */
        public ServiceTokenOperator.ServiceTokenBuilder withExpirationDateTime(LocalDateTime expirationDateTime) {
            this.expirationDateTime = expirationDateTime;
            return this;
        }

        public ServiceTokenOperator.ServiceTokenBuilder inProject(String projectId) {
            this.projectId = projectId;
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

        public ServiceTokenOperator.ServiceTokenBuilder withSupportedBandwidths(List<Integer> supportedBandwidths) {
            this.supportedBandwidths = supportedBandwidths;
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

        /**
         * Targets a Network Edge virtual device (access point selector type {@code VD}).
         *
         * @param virtualDeviceUuid the Network Edge assigned virtual device identifier
         * @return this builder
         */
        public ServiceTokenOperator.ServiceTokenBuilder onVirtualDeviceUuid(String virtualDeviceUuid) {
            this.virtualDeviceUuid = virtualDeviceUuid;
            return this;
        }

        /**
         * Selects a network interface on the targeted virtual device.
         *
         * @param interfaceId the Network Edge assigned interface identifier
         * @return this builder
         */
        public ServiceTokenOperator.ServiceTokenBuilder withNetworkInterfaceId(Integer interfaceId) {
            this.interfaceId = interfaceId;
            return this;
        }

        /**
         * Targets a Fabric network (access point selector type {@code NETWORK}), e.g. for
         * EVPLAN/EPLAN tokens.
         *
         * @param networkUuid the network identifier
         * @return this builder
         */
        public ServiceTokenOperator.ServiceTokenBuilder onNetworkUuid(String networkUuid) {
            this.networkUuid = networkUuid;
            return this;
        }

        /**
         * Hides asset information from the token recipient. Deprecated in the spec but still accepted.
         *
         * @return this builder
         */
        public ServiceTokenOperator.ServiceTokenBuilder hideAssetInfo() {
            this.hideAssetInfo = true;
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
