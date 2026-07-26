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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.RouteAggregationRules;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationRuleClient;
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleOperator;
import com.eqixiac.equinix.fabric.model.wrappers.RouteAggregationRuleWrapper;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouteAggregationRulesImpl implements RouteAggregationRules {

    private final RouteAggregationRuleClient<RouteAggregationRule> serviceClient;

    public PaginatedList<RouteAggregationRule> list(String routeAggregationId) {
        Page<RouteAggregationRuleJson> responsePage = this.serviceClient.list(routeAggregationId);
        PaginatedList<RouteAggregationRule> routeAggregationRuleList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, RouteAggregationRuleWrapper::new);
        return new PaginatedList<>(routeAggregationRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteAggregationRule getByUuid(String routeAggregationId, String uuid) {
        RouteAggregationRuleJson routeAggregationRuleJson = this.serviceClient.getByUuid(routeAggregationId, uuid);
        return new RouteAggregationRuleWrapper(routeAggregationRuleJson, this.serviceClient);
    }

    public RouteAggregationRuleOperator.RouteAggregationRuleBuilder define(String routeAggregationId) {
        return new RouteAggregationRuleOperator(this.serviceClient, routeAggregationId).create();
    }

    public RouteAggregationRule replace(String routeAggregationId, String uuid, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson) {
        RouteAggregationRuleJson routeAggregationRuleJson = this.serviceClient.replace(routeAggregationId, uuid, routeAggregationRuleCreatorJson);
        return new RouteAggregationRuleWrapper(routeAggregationRuleJson, this.serviceClient);
    }

    public List<RouteAggregationRule> createBulk(String routeAggregationId, List<RouteAggregationRuleCreatorJson> routeAggregationRuleCreatorJsonList) {
        return this.serviceClient.createBulk(routeAggregationId, routeAggregationRuleCreatorJsonList).stream()
                .map(json -> (RouteAggregationRule) new RouteAggregationRuleWrapper(json, this.serviceClient))
                .collect(Collectors.toList());
    }

    public PaginatedFilteredList<RouteAggregationRule> search(String routeAggregationId, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteAggregationRuleJson> responsePage = this.serviceClient.search(routeAggregationId, filter, sort);
        PaginatedFilteredList<RouteAggregationRule> routeAggregationRuleList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteAggregationRuleWrapper::new);
        return new PaginatedFilteredList<>(routeAggregationRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public List<Change> getChanges(String routeAggregationId, String uuid) {
        return this.serviceClient.getChanges(routeAggregationId, uuid);
    }

    public Change getChange(String routeAggregationId, String uuid, String changeId) {
        return this.serviceClient.getChange(routeAggregationId, uuid, changeId);
    }
}
