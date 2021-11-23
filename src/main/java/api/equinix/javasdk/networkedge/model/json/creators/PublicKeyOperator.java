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
import api.equinix.javasdk.networkedge.client.internal.implementation.PublicKeyClientImpl;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.json.PublicKeyJson;
import api.equinix.javasdk.networkedge.model.wrappers.PublicKeyWrapper;
import lombok.Getter;

/**
 * <p>PublicKeyOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PublicKeyOperator {

    @Getter
    private final Pageable<PublicKey> serviceClient;

    /**
     * <p>Constructor for PublicKeyOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public PublicKeyOperator(Pageable<PublicKey> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param keyName a {@link java.lang.String} object.
     * @param keyValue a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.PublicKeyOperator.PublicKeyBuilder} object.
     */
    public PublicKeyBuilder create(String keyName, String keyValue) {
        return new PublicKeyOperator.PublicKeyBuilder(keyName, keyValue);
    }

    @Getter
    public class PublicKeyBuilder {

        private final String keyName;
        private final String keyValue;
        private String accountUcmId;

        public PublicKeyBuilder(String keyName, String keyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
        }

        public PublicKeyBuilder forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public PublicKeyCreatorJson buildCreatorJson() {
            return new PublicKeyCreatorJson(this);
        }

        public PublicKey create() {
            PublicKeyCreatorJson publicKeyCreatorJson = new PublicKeyCreatorJson(this);
            PublicKeyJson publicKeyJson = ((PublicKeyClientImpl) PublicKeyOperator.this.getServiceClient()).create(publicKeyCreatorJson);
           return new PublicKeyWrapper(publicKeyJson, PublicKeyOperator.this.getServiceClient());
        }
    }
}
