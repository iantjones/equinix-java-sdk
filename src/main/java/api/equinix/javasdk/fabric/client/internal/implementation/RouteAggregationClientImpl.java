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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationClient;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.json.RouteAggregationJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationWrapper;

import java.util.Map;

public class RouteAggregationClientImpl extends PageableBase implements RouteAggregationClient<RouteAggregation> {

    public RouteAggregationClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteAggregations");
    }

    public Page<RouteAggregation, RouteAggregationJson> list() {
        EquinixRequest<RouteAggregation> equinixRequest = this.buildRequest("GetRouteAggregations", RequestType.PAGINATED, RouteAggregationJson.getPagedTypeRef());
        EquinixResponse<RouteAggregation> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RouteAggregationJson> equinixRequest = this.buildRequestWithPathParams("GetRouteAggregation", RequestType.SINGLE, pParams, RouteAggregationJson.getSingleTypeRef());
        EquinixResponse<RouteAggregationJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationJson create(RouteAggregationCreatorJson routeAggregationCreatorJson) {
        EquinixRequest<RouteAggregationJson> equinixRequest = this.buildRequest("PostRouteAggregation", RequestType.SINGLE, RouteAggregationJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routeAggregationCreatorJson);
        EquinixResponse<RouteAggregationJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationJson update(String uuid, Object updates) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RouteAggregationJson> equinixRequest = this.buildRequestWithPathParams("PatchRouteAggregation", RequestType.SINGLE, pParams, RouteAggregationJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, updates);
        EquinixResponse<RouteAggregationJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RouteAggregation> equinixRequest = this.buildRequestWithPathParams("DeleteRouteAggregation", RequestType.SINGLE, pParams, RouteAggregationJson.getSingleTypeRef());
        EquinixResponse<RouteAggregation> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<RouteAggregation> nextPage(PaginatedRequest<RouteAggregation> equinixRequest) {
        EquinixResponse<RouteAggregation> equinixResponse = this.invoke(equinixRequest);
        Page<RouteAggregation, RouteAggregationJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RouteAggregation> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RouteAggregationWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
