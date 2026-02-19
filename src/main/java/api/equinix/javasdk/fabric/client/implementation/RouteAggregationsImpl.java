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
import api.equinix.javasdk.fabric.client.RouteAggregations;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationClient;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.json.RouteAggregationJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationOperator;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationWrapper;

public class RouteAggregationsImpl implements RouteAggregations {

    private final Fabric serviceManager;

    private final RouteAggregationClient<RouteAggregation> serviceClient;

    public RouteAggregationsImpl(RouteAggregationClient<RouteAggregation> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<RouteAggregation> list() {
        Page<RouteAggregation, RouteAggregationJson> responsePage = this.serviceClient.list();
        PaginatedList<RouteAggregation> routeAggregationList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, RouteAggregationWrapper::new);
        return new PaginatedList<>(routeAggregationList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RouteAggregation getByUuid(String uuid) {
        RouteAggregationJson routeAggregationJson = this.serviceClient.getByUuid(uuid);
        return new RouteAggregationWrapper(routeAggregationJson, this.serviceClient);
    }

    public RouteAggregationOperator.RouteAggregationBuilder define() {
        return new RouteAggregationOperator(this.serviceClient).create();
    }
}
