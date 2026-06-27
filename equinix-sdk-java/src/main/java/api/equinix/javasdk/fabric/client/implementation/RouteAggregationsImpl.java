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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.RouteAggregations;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationClient;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteAggregationJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationOperator;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationWrapper;

public class RouteAggregationsImpl implements RouteAggregations {

    private final RouteAggregationClient<RouteAggregation> serviceClient;

    public RouteAggregationsImpl(RouteAggregationClient<RouteAggregation> serviceClient) {
        this.serviceClient = serviceClient;
    }

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
        Page<RouteAggregation, RouteAggregationJson> responsePage = this.serviceClient.search(filter, sort);
        PaginatedFilteredList<RouteAggregation> routeAggregationList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, RouteAggregationWrapper::new);
        return new PaginatedFilteredList<>(routeAggregationList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteAggregation getByUuid(String uuid) {
        RouteAggregationJson routeAggregationJson = this.serviceClient.getByUuid(uuid);
        return new RouteAggregationWrapper(routeAggregationJson, this.serviceClient);
    }

    public RouteAggregationOperator.RouteAggregationBuilder define() {
        return new RouteAggregationOperator(this.serviceClient).create();
    }
}
