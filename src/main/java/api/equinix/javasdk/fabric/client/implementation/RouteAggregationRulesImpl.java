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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.RouteAggregationRules;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationRuleClient;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationRuleOperator;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationRuleWrapper;

public class RouteAggregationRulesImpl implements RouteAggregationRules {

    private final Fabric serviceManager;

    private final RouteAggregationRuleClient<RouteAggregationRule> serviceClient;

    public RouteAggregationRulesImpl(RouteAggregationRuleClient<RouteAggregationRule> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<RouteAggregationRule> list(String routeAggregationId) {
        Page<RouteAggregationRule, RouteAggregationRuleJson> responsePage = this.serviceClient.list(routeAggregationId);
        PaginatedList<RouteAggregationRule> routeAggregationRuleList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, RouteAggregationRuleWrapper::new);
        return new PaginatedList<>(routeAggregationRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteAggregationRule getByUuid(String routeAggregationId, String uuid) {
        RouteAggregationRuleJson routeAggregationRuleJson = this.serviceClient.getByUuid(routeAggregationId, uuid);
        return new RouteAggregationRuleWrapper(routeAggregationRuleJson, this.serviceClient);
    }

    public RouteAggregationRuleOperator.RouteAggregationRuleBuilder define(String routeAggregationId) {
        return new RouteAggregationRuleOperator(this.serviceClient, routeAggregationId).create();
    }
}
