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
import com.eqixiac.equinix.fabric.client.RouteFilterRules;
import com.eqixiac.equinix.fabric.client.internal.RouteFilterRuleClient;
import com.eqixiac.equinix.fabric.model.RouteFilterRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterRuleOperator;
import com.eqixiac.equinix.fabric.model.wrappers.RouteFilterRuleWrapper;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouteFilterRulesImpl implements RouteFilterRules {

    private final RouteFilterRuleClient<RouteFilterRule> serviceClient;

    public PaginatedList<RouteFilterRule> list(String routeFilterId) {
        Page<RouteFilterRuleJson> responsePage = this.serviceClient.list(routeFilterId);
        PaginatedList<RouteFilterRule> routeFilterRuleList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, RouteFilterRuleWrapper::new);
        return new PaginatedList<>(routeFilterRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteFilterRule getByUuid(String routeFilterId, String uuid) {
        RouteFilterRuleJson routeFilterRuleJson = this.serviceClient.getByUuid(routeFilterId, uuid);
        return new RouteFilterRuleWrapper(routeFilterRuleJson, this.serviceClient);
    }

    public RouteFilterRuleOperator.RouteFilterRuleBuilder define(String routeFilterId) {
        return new RouteFilterRuleOperator(this.serviceClient, routeFilterId).create();
    }

    public RouteFilterRule replace(String routeFilterId, String uuid, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson) {
        RouteFilterRuleJson routeFilterRuleJson = this.serviceClient.replace(routeFilterId, uuid, routeFilterRuleCreatorJson);
        return new RouteFilterRuleWrapper(routeFilterRuleJson, this.serviceClient);
    }

    public List<RouteFilterRule> createBulk(String routeFilterId, List<RouteFilterRuleCreatorJson> routeFilterRuleCreatorJsonList) {
        return this.serviceClient.createBulk(routeFilterId, routeFilterRuleCreatorJsonList).stream()
                .map(json -> (RouteFilterRule) new RouteFilterRuleWrapper(json, this.serviceClient))
                .collect(Collectors.toList());
    }

    public PaginatedFilteredList<RouteFilterRule> search(String routeFilterId, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteFilterRuleJson> responsePage = this.serviceClient.search(routeFilterId, filter, sort);
        PaginatedFilteredList<RouteFilterRule> routeFilterRuleList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteFilterRuleWrapper::new);
        return new PaginatedFilteredList<>(routeFilterRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public List<Change> getChanges(String routeFilterId, String uuid) {
        return this.serviceClient.getChanges(routeFilterId, uuid);
    }

    public Change getChange(String routeFilterId, String uuid, String changeId) {
        return this.serviceClient.getChange(routeFilterId, uuid, changeId);
    }
}
