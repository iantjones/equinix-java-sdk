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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.RouteAggregationRuleClientImpl;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class RouteAggregationRuleWrapper extends ResourceImpl<RouteAggregationRule> implements RouteAggregationRule {

    @Delegate(excludes = RouteAggregationRuleMutability.class)
    private RouteAggregationRuleJson jsonObject;
    @Getter
    private final Pageable<RouteAggregationRule> serviceClient;

    public RouteAggregationRuleWrapper(RouteAggregationRuleJson routeAggregationRuleJson, Pageable<RouteAggregationRule> serviceClient) {
        this.jsonObject = routeAggregationRuleJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete(String routeAggregationId) {
        this.jsonObject = ((RouteAggregationRuleClientImpl)this.serviceClient).delete(routeAggregationId, this.getUuid());
        return true;
    }

    public void refresh(String routeAggregationId) {
        this.jsonObject = ((RouteAggregationRuleClientImpl)this.serviceClient).refresh(routeAggregationId, this.getUuid());
    }

    private interface RouteAggregationRuleMutability {
        Boolean delete(String routeAggregationId);
        void refresh(String routeAggregationId);
    }
}
