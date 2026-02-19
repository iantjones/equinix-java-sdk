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
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteFilterClient;
import api.equinix.javasdk.fabric.model.RouteFilter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteFilterJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterWrapper;

import java.util.Map;

public class RouteFilterClientImpl extends PageableBase implements RouteFilterClient<RouteFilter> {

    public RouteFilterClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteFilters");
    }

    public Page<RouteFilter, RouteFilterJson> search(FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<RouteFilter> equinixRequest = this.buildRequest("SearchRouteFilters", RequestType.PAGINATED_POST, RouteFilterJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredSortedPaginatedPost(filter, sort));
        EquinixResponse<RouteFilter> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RouteFilterJson> equinixRequest = this.buildRequestWithPathParams("GetRouteFilter", RequestType.SINGLE, pParams, RouteFilterJson.getSingleTypeRef());
        EquinixResponse<RouteFilterJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterJson create(RouteFilterCreatorJson routeFilterCreatorJson) {
        EquinixRequest<RouteFilterJson> equinixRequest = this.buildRequest("PostRouteFilter", RequestType.SINGLE, RouteFilterJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, routeFilterCreatorJson);
        EquinixResponse<RouteFilterJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<RouteFilter> equinixRequest = this.buildRequestWithPathParams("DeleteRouteFilter", RequestType.SINGLE, pParams, RouteFilterJson.getSingleTypeRef());
        EquinixResponse<RouteFilter> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public RouteFilterJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<RouteFilter> nextPage(PaginatedRequest<RouteFilter> equinixRequest) {
        EquinixResponse<RouteFilter> equinixResponse = this.invoke(equinixRequest);
        Page<RouteFilter, RouteFilterJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<RouteFilter> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, RouteFilterWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<RouteFilter> nextPage(PaginatedPostRequest<RouteFilter> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<RouteFilter> equinixResponse = this.invoke(equinixRequest);
        Page<RouteFilter, RouteFilterJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<RouteFilter> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, RouteFilterWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
