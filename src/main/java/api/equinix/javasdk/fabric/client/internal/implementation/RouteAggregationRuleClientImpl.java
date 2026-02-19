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
import api.equinix.javasdk.fabric.client.internal.RouteAggregationRuleClient;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationRuleWrapper;

import java.util.Map;

public class RouteAggregationRuleClientImpl extends PageableBase implements RouteAggregationRuleClient<RouteAggregationRule> {

    public RouteAggregationRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteAggregationRules");
    }

    public Page<RouteAggregationRule, RouteAggregationRuleJson> list(String routeAggregationId) {
        Map<String, String> pParams = Map.of("routeAggregationId", routeAggregationId);
        EquinixRequest<RouteAggregationRule> equinixRequest = this.buildRequestWithPathParams("GetRouteAggregationRules", RequestType.PAGINATED, pParams, RouteAggregationRuleJson.getPagedTypeRef());
        EquinixResponse<RouteAggregationRule> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationRuleJson getByUuid(String routeAggregationId, String uuid) {
        Map<String, String> pParams = Map.of("routeAggregationId", routeAggregationId, "uuid", uuid);
        EquinixRequest<RouteAggregationRuleJson> equinixRequest = this.buildRequestWithPathParams("GetRouteAggregationRule", RequestType.SINGLE, pParams, RouteAggregationRuleJson.getSingleTypeRef());
        EquinixResponse<RouteAggregationRuleJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationRuleJson create(String routeAggregationId, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson) {
        Map<String, String> pParams = Map.of("routeAggregationId", routeAggregationId);
        EquinixRequest<RouteAggregationRuleJson> equinixRequest = this.buildRequestWithPathParams("PostRouteAggregationRule", RequestType.SINGLE, pParams, RouteAggregationRuleJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routeAggregationRuleCreatorJson);
        EquinixResponse<RouteAggregationRuleJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationRuleJson update(String routeAggregationId, String uuid, Object updates) {
        Map<String, String> pParams = Map.of("routeAggregationId", routeAggregationId, "uuid", uuid);
        EquinixRequest<RouteAggregationRuleJson> equinixRequest = this.buildRequestWithPathParams("PatchRouteAggregationRule", RequestType.SINGLE, pParams, RouteAggregationRuleJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, updates);
        EquinixResponse<RouteAggregationRuleJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationRuleJson delete(String routeAggregationId, String uuid) {
        Map<String, String> pParams = Map.of("routeAggregationId", routeAggregationId, "uuid", uuid);
        EquinixRequest<RouteAggregationRule> equinixRequest = this.buildRequestWithPathParams("DeleteRouteAggregationRule", RequestType.SINGLE, pParams, RouteAggregationRuleJson.getSingleTypeRef());
        EquinixResponse<RouteAggregationRule> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteAggregationRuleJson refresh(String routeAggregationId, String uuid) {
        return this.getByUuid(routeAggregationId, uuid);
    }

    public PaginatedList<RouteAggregationRule> nextPage(PaginatedRequest<RouteAggregationRule> equinixRequest) {
        EquinixResponse<RouteAggregationRule> equinixResponse = this.invoke(equinixRequest);
        Page<RouteAggregationRule, RouteAggregationRuleJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RouteAggregationRule> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RouteAggregationRuleWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
