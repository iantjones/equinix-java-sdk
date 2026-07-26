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

package com.eqixiac.equinix.networkedge.model.json.creators;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.IPAddress;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.networkedge.client.internal.implementation.BGPPeeringClientImpl;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.networkedge.model.BGPPeering;
import com.eqixiac.equinix.networkedge.model.json.BGPPeeringJson;
import com.eqixiac.equinix.networkedge.model.wrappers.BGPPeeringWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author ianjones
 */
@Setter(AccessLevel.PRIVATE)
public class BGPPeeringOperator extends ResourceImpl<BGPPeering> {

    @Getter
    private final Pageable<BGPPeering> serviceClient;

    public BGPPeeringOperator(Pageable<BGPPeering> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public BGPPeeringBuilder create() {
        return new BGPPeeringBuilder();
    }

    public BGPPeeringUpdater update(BGPPeeringJson json) {
        return new BGPPeeringUpdater(json);
    }

    @Getter
    public class BGPPeeringBuilder {
        private String connectionUuid;
        private String localIpAddress;
        private String remoteIpAddress;
        private Long localAsn;
        private Long remoteAsn;
        private String authenticationKey;

        protected BGPPeeringBuilder() {
        }

         public BGPPeeringBuilder forConnection(String connectionUuid) {
            this.connectionUuid = connectionUuid;
            return this;
        }

         public BGPPeeringBuilder forConnection(Connection connection) {
            return forConnection(connection.getUuid());
        }

         public BGPPeeringBuilder withLocalIpAddress(String localIpAddress) {
            this.localIpAddress = localIpAddress;
            return this;
        }

        /**
         * Typed variant of {@code withLocalIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, so a subnet prefix (e.g. {@code /30}) is preserved and the
         * wire value is identical to passing the equivalent string.
         */
         public BGPPeeringBuilder withLocalIpAddress(IPAddress localIpAddress) {
            return withLocalIpAddress(localIpAddress == null ? null : localIpAddress.toCidr());
        }

         public BGPPeeringBuilder withRemoteIpAddress(String remoteIpAddress) {
            this.remoteIpAddress = remoteIpAddress;
            return this;
        }

        /**
         * Typed variant of {@code withRemoteIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, producing the identical wire value to the String setter.
         */
         public BGPPeeringBuilder withRemoteIpAddress(IPAddress remoteIpAddress) {
            return withRemoteIpAddress(remoteIpAddress == null ? null : remoteIpAddress.toCidr());
        }

         public BGPPeeringBuilder withLocalAsn(Long localAsn) {
            this.localAsn = localAsn;
            return this;
        }

         public BGPPeeringBuilder withRemoteAsn(Long remoteAsn) {
            this.remoteAsn = remoteAsn;
            return this;
        }

         public BGPPeeringBuilder withAuthenticationKey(String authenticationKey) {
            this.authenticationKey = authenticationKey;
            return this;
        }

        public BGPPeering save() {
            BGPPeeringCreatorJson deviceLinkCreatorJson = new BGPPeeringCreatorJson(this);
            BGPPeeringJson deviceLinkJson = ((BGPPeeringClientImpl) BGPPeeringOperator.this.getServiceClient()).create(deviceLinkCreatorJson);
            return new BGPPeeringWrapper(deviceLinkJson, BGPPeeringOperator.this.getServiceClient());
        }
    }

    public class BGPPeeringUpdater {

        private BGPPeeringJson json;
        private BGPPeeringUpdaterJson updaterJson;

        protected BGPPeeringUpdater(BGPPeeringJson json) {
            this.json = json;
            this.updaterJson = Constants.converter().convertValue(this.json, BGPPeeringUpdaterJson.class);
        }

        public BGPPeeringUpdater withLocalIpAddress(String localIpAddress) {
            this.updaterJson.setLocalIpAddress(localIpAddress);
            return this;
        }

        /**
         * Typed variant of {@code withLocalIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, so a subnet prefix (e.g. {@code /30}) is preserved and the
         * wire value is identical to passing the equivalent string.
         */
        public BGPPeeringUpdater withLocalIpAddress(IPAddress localIpAddress) {
            return withLocalIpAddress(localIpAddress == null ? null : localIpAddress.toCidr());
        }

        public BGPPeeringUpdater withRemoteIpAddress(String remoteIpAddress) {
            this.updaterJson.setRemoteIpAddress(remoteIpAddress);
            return this;
        }

        /**
         * Typed variant of {@code withRemoteIpAddress(String)}. Serializes the address via
         * {@link IPAddress#toCidr()}, producing the identical wire value to the String setter.
         */
        public BGPPeeringUpdater withRemoteIpAddress(IPAddress remoteIpAddress) {
            return withRemoteIpAddress(remoteIpAddress == null ? null : remoteIpAddress.toCidr());
        }

        public BGPPeeringUpdater withLocalAsn(Long localAsn) {
            this.updaterJson.setLocalAsn(localAsn);
            return this;
        }

        public BGPPeeringUpdater withRemoteAsn(Long remoteAsn) {
            this.updaterJson.setRemoteAsn(remoteAsn);
            return this;
        }

        public BGPPeeringUpdater withAuthenticationKey(String authenticationKey) {
            this.updaterJson.setAuthenticationKey(authenticationKey);
            return this;
        }

        public BGPPeering save() {
            json = ((BGPPeeringClientImpl) BGPPeeringOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new BGPPeeringWrapper(json, BGPPeeringOperator.this.getServiceClient());
        }
    }
}
