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

package api.equinix.javasdk.networkedge.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.networkedge.client.internal.implementation.BGPPeeringClientImpl;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.json.BGPPeeringJson;
import api.equinix.javasdk.networkedge.model.wrappers.BGPPeeringWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>BGPPeeringOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class BGPPeeringOperator extends ResourceImpl<BGPPeering> {

    @Getter
    private final Pageable<BGPPeering> serviceClient;

    /**
     * <p>Constructor for BGPPeeringOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public BGPPeeringOperator(Pageable<BGPPeering> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringBuilder} object.
     */
    public BGPPeeringBuilder create() {
        return new BGPPeeringBuilder();
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringUpdater} object.
     */
    public BGPPeeringUpdater update(BGPPeeringJson json) {
        return new BGPPeeringUpdater(json);
    }

    @Getter
    public class BGPPeeringBuilder {
        private String connectionUuid;
        private String localIpAddress;
        private String remoteIpAddress;
        private Integer localAsn;
        private Integer remoteAsn;
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

         public BGPPeeringBuilder withRemoteIpAddress(String remoteIpAddress) {
            this.remoteIpAddress = remoteIpAddress;
            return this;
        }

         public BGPPeeringBuilder withLocalAsn(Integer localAsn) {
            this.localAsn = localAsn;
            return this;
        }

         public BGPPeeringBuilder withRemoteAsn(Integer remoteAsn) {
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
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, BGPPeeringUpdaterJson.class);
        }

        public BGPPeeringUpdater withLocalIpAddress(String localIpAddress) {
            this.updaterJson.setLocalIpAddress(localIpAddress);
            return this;
        }

        public BGPPeeringUpdater withRemoteIpAddress(String remoteIpAddress) {
            this.updaterJson.setRemoteIpAddress(remoteIpAddress);
            return this;
        }

        public BGPPeeringUpdater withLocalAsn(Integer localAsn) {
            this.updaterJson.setLocalAsn(localAsn);
            return this;
        }

        public BGPPeeringUpdater withRemoteAsn(Integer remoteAsn) {
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
