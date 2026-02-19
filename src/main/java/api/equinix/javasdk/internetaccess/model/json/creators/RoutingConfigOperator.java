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
import api.equinix.javasdk.internetaccess.client.internal.implementation.RoutingConfigClientImpl;
import api.equinix.javasdk.internetaccess.enums.RoutingConfigType;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;
import api.equinix.javasdk.internetaccess.model.json.RoutingConfigJson;
import api.equinix.javasdk.internetaccess.model.wrappers.RoutingConfigWrapper;
import lombok.Getter;

import java.util.List;

public class RoutingConfigOperator extends ResourceImpl<RoutingConfig> {

    @Getter
    private final Pageable<RoutingConfig> serviceClient;

    public RoutingConfigOperator(Pageable<RoutingConfig> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public RoutingConfigBuilder create() {
        return new RoutingConfigBuilder();
    }

    @Getter
    public class RoutingConfigBuilder {
        private RoutingConfigType type;
        private Long asn;
        private List<String> prefixes;

        public RoutingConfigBuilder type(RoutingConfigType type) {
            this.type = type;
            return this;
        }

        public RoutingConfigBuilder asn(Long asn) {
            this.asn = asn;
            return this;
        }

        public RoutingConfigBuilder prefixes(List<String> prefixes) {
            this.prefixes = prefixes;
            return this;
        }

        public RoutingConfig create() {
            RoutingConfigCreatorJson creatorJson = new RoutingConfigCreatorJson(this);
            RoutingConfigJson routingConfigJson = ((RoutingConfigClientImpl) RoutingConfigOperator.this.getServiceClient()).create(creatorJson);
            return new RoutingConfigWrapper(routingConfigJson, RoutingConfigOperator.this.getServiceClient());
        }
    }
}
