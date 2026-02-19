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
import api.equinix.javasdk.customerportal.client.internal.implementation.DigitalLOAClientImpl;
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.json.DigitalLOAJson;
import api.equinix.javasdk.customerportal.model.wrappers.DigitalLOAWrapper;
import lombok.Getter;

public class DigitalLOAOperator extends ResourceImpl<DigitalLOA> {

    @Getter
    private final Pageable<DigitalLOA> serviceClient;

    public DigitalLOAOperator(Pageable<DigitalLOA> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public DigitalLOABuilder create() {
        return new DigitalLOABuilder();
    }

    @Getter
    public class DigitalLOABuilder {
        private String loaType;
        private String ibx;
        private String accountNumber;

        public DigitalLOABuilder loaType(String loaType) {
            this.loaType = loaType;
            return this;
        }

        public DigitalLOABuilder ibx(String ibx) {
            this.ibx = ibx;
            return this;
        }

        public DigitalLOABuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public DigitalLOA create() {
            DigitalLOACreatorJson creatorJson = new DigitalLOACreatorJson(this);
            DigitalLOAJson digitalLOAJson = ((DigitalLOAClientImpl) DigitalLOAOperator.this.getServiceClient()).create(creatorJson);
            return new DigitalLOAWrapper(digitalLOAJson, DigitalLOAOperator.this.getServiceClient());
        }
    }
}
