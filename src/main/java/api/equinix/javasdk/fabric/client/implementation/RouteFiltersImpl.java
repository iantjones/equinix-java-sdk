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
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.RouteFilters;
import api.equinix.javasdk.fabric.client.internal.RouteFilterClient;
import api.equinix.javasdk.fabric.model.RouteFilter;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteFilterJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterOperator;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterWrapper;

public class RouteFiltersImpl implements RouteFilters {

    private final Fabric serviceManager;

    private final RouteFilterClient<RouteFilter> serviceClient;

    public RouteFiltersImpl(RouteFilterClient<RouteFilter> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

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
        Page<RouteFilter, RouteFilterJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<RouteFilter> routeFilterList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteFilterWrapper::new);
        return new PaginatedFilteredList<>(routeFilterList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteFilter getByUuid(String uuid) {
        RouteFilterJson routeFilterJson = this.serviceClient.getByUuid(uuid);
        return new RouteFilterWrapper(routeFilterJson, this.serviceClient);
    }

    public RouteFilterOperator.RouteFilterBuilder define() {
        return new RouteFilterOperator(this.serviceClient).create();
    }
}
