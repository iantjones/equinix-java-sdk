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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteAggregationRuleClientImpl;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationRuleWrapper;
import lombok.Getter;

public class RouteAggregationRuleOperator extends ResourceImpl<RouteAggregationRule> {

    @Getter
    private final Pageable<RouteAggregationRule> serviceClient;

    private final String routeAggregationId;

    public RouteAggregationRuleOperator(Pageable<RouteAggregationRule> serviceClient, String routeAggregationId) {
        this.serviceClient = serviceClient;
        this.routeAggregationId = routeAggregationId;
    }

    public RouteAggregationRuleBuilder create() {
        return new RouteAggregationRuleBuilder();
    }

    @Getter
    public class RouteAggregationRuleBuilder {

        private String name;
        private String prefix;
        private String description;

        protected RouteAggregationRuleBuilder() {
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public RouteAggregationRuleOperator.RouteAggregationRuleBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public RouteAggregationRule create() {
            RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson = new RouteAggregationRuleCreatorJson(this);
            RouteAggregationRuleJson routeAggregationRuleJson = ((RouteAggregationRuleClientImpl) RouteAggregationRuleOperator.this.getServiceClient()).create(RouteAggregationRuleOperator.this.routeAggregationId, routeAggregationRuleCreatorJson);
            return new RouteAggregationRuleWrapper(routeAggregationRuleJson, RouteAggregationRuleOperator.this.getServiceClient());
        }
    }
}
