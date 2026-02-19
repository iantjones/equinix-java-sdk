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
import api.equinix.javasdk.fabric.client.RouteFilterRules;
import api.equinix.javasdk.fabric.client.internal.RouteFilterRuleClient;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.RouteFilterRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleOperator;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterRuleWrapper;

public class RouteFilterRulesImpl implements RouteFilterRules {

    private final Fabric serviceManager;

    private final RouteFilterRuleClient<RouteFilterRule> serviceClient;

    public RouteFilterRulesImpl(RouteFilterRuleClient<RouteFilterRule> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<RouteFilterRule> list(String routeFilterId) {
        Page<RouteFilterRule, RouteFilterRuleJson> responsePage = this.serviceClient.list(routeFilterId);
        PaginatedList<RouteFilterRule> routeFilterRuleList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, RouteFilterRuleWrapper::new);
        return new PaginatedList<>(routeFilterRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteFilterRule getByUuid(String routeFilterId, String uuid) {
        RouteFilterRuleJson routeFilterRuleJson = this.serviceClient.getByUuid(routeFilterId, uuid);
        return new RouteFilterRuleWrapper(routeFilterRuleJson, this.serviceClient);
    }

    public RouteFilterRuleOperator.RouteFilterRuleBuilder define(String routeFilterId) {
        return new RouteFilterRuleOperator(this.serviceClient, routeFilterId).create();
    }
}
