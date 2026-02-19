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

package api.equinix.javasdk.internetaccess.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.internetaccess.client.internal.implementation.InternetAccessServiceClientImpl;
import api.equinix.javasdk.internetaccess.enums.InternetAccessServiceType;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.wrappers.InternetAccessServiceWrapper;
import lombok.Getter;

public class InternetAccessServiceOperator extends ResourceImpl<InternetAccessService> {

    @Getter
    private final Pageable<InternetAccessService> serviceClient;

    public InternetAccessServiceOperator(Pageable<InternetAccessService> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public InternetAccessServiceBuilder create() {
        return new InternetAccessServiceBuilder();
    }

    @Getter
    public class InternetAccessServiceBuilder {
        private String name;
        private InternetAccessServiceType type;
        private Integer bandwidth;
        private String ibx;

        public InternetAccessServiceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public InternetAccessServiceBuilder type(InternetAccessServiceType type) {
            this.type = type;
            return this;
        }

        public InternetAccessServiceBuilder bandwidth(Integer bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public InternetAccessServiceBuilder ibx(String ibx) {
            this.ibx = ibx;
            return this;
        }

        public InternetAccessService create() {
            InternetAccessServiceCreatorJson creatorJson = new InternetAccessServiceCreatorJson(this);
            InternetAccessServiceJson internetAccessServiceJson = ((InternetAccessServiceClientImpl) InternetAccessServiceOperator.this.getServiceClient()).create(creatorJson);
            return new InternetAccessServiceWrapper(internetAccessServiceJson, InternetAccessServiceOperator.this.getServiceClient());
        }
    }
}
