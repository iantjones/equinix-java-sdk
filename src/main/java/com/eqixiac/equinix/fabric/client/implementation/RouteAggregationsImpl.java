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
import com.eqixiac.equinix.fabric.client.RouteAggregations;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationClient;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteAggregation;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationOperator;
import com.eqixiac.equinix.fabric.model.wrappers.RouteAggregationWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouteAggregationsImpl implements RouteAggregations {

    private final RouteAggregationClient<RouteAggregation> serviceClient;

    public PaginatedFilteredList<RouteAggregation> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<RouteAggregation> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<RouteAggregation> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<RouteAggregation> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteAggregationJson> responsePage = this.serviceClient.search(filter, sort);
        PaginatedFilteredList<RouteAggregation> routeAggregationList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteAggregationWrapper::new);
        return new PaginatedFilteredList<>(routeAggregationList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteAggregation getByUuid(String uuid) {
        RouteAggregationJson routeAggregationJson = this.serviceClient.getByUuid(uuid);
        return new RouteAggregationWrapper(routeAggregationJson, this.serviceClient);
    }

    public RouteAggregationOperator.RouteAggregationBuilder define() {
        return new RouteAggregationOperator(this.serviceClient).create();
    }

    public List<Change> getChanges(String uuid) {
        return this.serviceClient.getChanges(uuid);
    }

    public Change getChange(String uuid, String changeId) {
        return this.serviceClient.getChange(uuid, changeId);
    }

    public List<Connection> getConnections(String uuid) {
        return this.serviceClient.getConnections(uuid);
    }
}
