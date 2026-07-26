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
import com.eqixiac.equinix.networkedge.client.internal.implementation.PublicKeyClientImpl;
import com.eqixiac.equinix.networkedge.enums.KeyType;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import com.eqixiac.equinix.networkedge.model.json.PublicKeyJson;
import com.eqixiac.equinix.networkedge.model.wrappers.PublicKeyWrapper;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
public class PublicKeyOperator {

    @Getter
    private final Pageable<PublicKey> serviceClient;

    public PublicKeyOperator(Pageable<PublicKey> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PublicKeyBuilder create(String keyName, String keyValue) {
        return new PublicKeyOperator.PublicKeyBuilder(keyName, keyValue);
    }

    @Getter
    public class PublicKeyBuilder {

        private final String keyName;
        private final String keyValue;
        private KeyType keyType;
        private String accountUcmId;

        public PublicKeyBuilder(String keyName, String keyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
        }

        public PublicKeyBuilder forAccount(String accountUcmId) {
            this.accountUcmId = accountUcmId;
            return this;
        }

        public PublicKeyBuilder withKeyType(KeyType keyType) {
            this.keyType = keyType;
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
