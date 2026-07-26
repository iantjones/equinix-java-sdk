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
import com.eqixiac.equinix.fabric.client.RouteFilters;
import com.eqixiac.equinix.fabric.client.internal.RouteFilterClient;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteFilter;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterOperator;
import com.eqixiac.equinix.fabric.model.wrappers.RouteFilterWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouteFiltersImpl implements RouteFilters {

    private final RouteFilterClient<RouteFilter> serviceClient;

    public PaginatedFilteredList<RouteFilter> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<RouteFilter> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<RouteFilter> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<RouteFilter> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteFilterJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<RouteFilter> routeFilterList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteFilterWrapper::new);
        return new PaginatedFilteredList<>(routeFilterList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteFilter getByUuid(String uuid) {
        RouteFilterJson routeFilterJson = this.serviceClient.getByUuid(uuid);
        return new RouteFilterWrapper(routeFilterJson, this.serviceClient);
    }

    public RouteFilterOperator.RouteFilterBuilder define() {
        return new RouteFilterOperator(this.serviceClient).create();
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
