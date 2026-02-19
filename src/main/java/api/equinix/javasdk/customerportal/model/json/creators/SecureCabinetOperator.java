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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SecureCabinetClientImpl;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import api.equinix.javasdk.customerportal.model.json.SecureCabinetJson;
import api.equinix.javasdk.customerportal.model.wrappers.SecureCabinetWrapper;
import lombok.Getter;

public class SecureCabinetOperator extends ResourceImpl<SecureCabinet> {

    @Getter
    private final Pageable<SecureCabinet> serviceClient;

    public SecureCabinetOperator(Pageable<SecureCabinet> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public SecureCabinetBuilder create() {
        return new SecureCabinetBuilder();
    }

    @Getter
    public class SecureCabinetBuilder {
        private String lockType;
        private String accessCode;

        public SecureCabinetBuilder lockType(String lockType) {
            this.lockType = lockType;
            return this;
        }

        public SecureCabinetBuilder accessCode(String accessCode) {
            this.accessCode = accessCode;
            return this;
        }

        public SecureCabinet create() {
            SecureCabinetCreatorJson creatorJson = new SecureCabinetCreatorJson(this);
            SecureCabinetJson secureCabinetJson = ((SecureCabinetClientImpl) SecureCabinetOperator.this.getServiceClient()).create(creatorJson);
            return new SecureCabinetWrapper(secureCabinetJson, SecureCabinetOperator.this.getServiceClient());
        }
    }
}
