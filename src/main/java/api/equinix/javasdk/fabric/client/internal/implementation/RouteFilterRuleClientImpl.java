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
import api.equinix.javasdk.fabric.client.internal.RouteFilterRuleClient;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.RouteFilterRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterRuleWrapper;

import java.util.Map;

public class RouteFilterRuleClientImpl extends PageableBase implements RouteFilterRuleClient<RouteFilterRule> {

    public RouteFilterRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteFilterRules");
    }

    public Page<RouteFilterRule, RouteFilterRuleJson> list(String routeFilterId) {
        Map<String, String> pParams = Map.of("routeFilterId", routeFilterId);
        EquinixRequest<RouteFilterRule> equinixRequest = this.buildRequestWithPathParams("ListRouteFilterRules", RequestType.PAGINATED, pParams, RouteFilterRuleJson.getPagedTypeRef());
        EquinixResponse<RouteFilterRule> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterRuleJson getByUuid(String routeFilterId, String uuid) {
        Map<String, String> pParams = Map.of("routeFilterId", routeFilterId, "uuid", uuid);
        EquinixRequest<RouteFilterRuleJson> equinixRequest = this.buildRequestWithPathParams("GetRouteFilterRule", RequestType.SINGLE, pParams, RouteFilterRuleJson.getSingleTypeRef());
        EquinixResponse<RouteFilterRuleJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterRuleJson create(String routeFilterId, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson) {
        Map<String, String> pParams = Map.of("routeFilterId", routeFilterId);
        EquinixRequest<RouteFilterRuleJson> equinixRequest = this.buildRequestWithPathParams("PostRouteFilterRule", RequestType.SINGLE, pParams, RouteFilterRuleJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routeFilterRuleCreatorJson);
        EquinixResponse<RouteFilterRuleJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterRuleJson delete(String routeFilterId, String uuid) {
        Map<String, String> pParams = Map.of("routeFilterId", routeFilterId, "uuid", uuid);
        EquinixRequest<RouteFilterRule> equinixRequest = this.buildRequestWithPathParams("DeleteRouteFilterRule", RequestType.SINGLE, pParams, RouteFilterRuleJson.getSingleTypeRef());
        EquinixResponse<RouteFilterRule> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterRuleJson refresh(String routeFilterId, String uuid) {
        return this.getByUuid(routeFilterId, uuid);
    }

    public PaginatedList<RouteFilterRule> nextPage(PaginatedRequest<RouteFilterRule> equinixRequest) {
        EquinixResponse<RouteFilterRule> equinixResponse = this.invoke(equinixRequest);
        Page<RouteFilterRule, RouteFilterRuleJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RouteFilterRule> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RouteFilterRuleWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
