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
import api.equinix.javasdk.fabric.client.internal.FabricGatewayClient;
import api.equinix.javasdk.fabric.model.*;
import api.equinix.javasdk.fabric.model.FabricGateway;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.*;
import api.equinix.javasdk.fabric.model.json.FabricGatewayJson;
import api.equinix.javasdk.fabric.model.wrappers.FabricGatewayWrapper;

import java.util.Map;

public class FabricGatewayClientImpl extends PageableBase implements FabricGatewayClient<FabricGateway> {

    public FabricGatewayClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "FabricGateways");
    }

    public Page<FabricGateway, FabricGatewayJson> search(FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<FabricGateway> equinixRequest = this.buildRequest("SearchFabricGateways", RequestType.PAGINATED_POST, FabricGatewayJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredSortedPaginatedPost(filter, sort));
        EquinixResponse<FabricGateway> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }
    
    public FabricGatewayJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<FabricGatewayJson> equinixRequest = this.buildRequestWithPathParams("GetFabricGateway", RequestType.SINGLE, pParams, FabricGatewayJson.getSingleTypeRef());
        EquinixResponse<FabricGatewayJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
    
//    public FabricGatewayJson create(FabricGatewayCreatorJson FabricGatewayCreatorJson) {
//        EquinixRequest<FabricGatewayJson> equinixRequest = this.buildRequest("PostFabricGateway", RequestType.SINGLE, FabricGatewayJson.getSingleTypeRef());
//        equinixRequest.setFilters(new SimpleFilterProvider().addFilter("createFabricGatewayFilter", SerializationFilters.createFabricGatewayFilter));
//        Utils.serializeJson(equinixRequest, FabricGatewayCreatorJson);
//        EquinixResponse<FabricGatewayJson> equinixResponse = this.invoke(equinixRequest);
//        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
//    }
    
    public FabricGatewayJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<FabricGateway> equinixRequest = this.buildRequestWithPathParams("DeleteFabricGateway", RequestType.SINGLE, pParams, FabricGatewayJson.getSingleTypeRef());
        EquinixResponse<FabricGateway> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

//
//    /** {@inheritDoc} */
//    public ServiceTokenJson getByUuid(String uuid) {
//        Map<String, String> pParams = Map.of("uuid", uuid);
//        EquinixRequest<ServiceTokenJson> equinixRequest = this.buildRequestWithPathParams("GetServiceToken", RequestType.SINGLE, pParams, ServiceTokenJson.getSingleTypeRef());
//        EquinixResponse<ServiceTokenJson> equinixResponse = this.invoke(equinixRequest);
//        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
//    }
    
    public FabricGatewayJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<FabricGateway> nextPage(PaginatedRequest<FabricGateway> equinixRequest) {
        EquinixResponse<FabricGateway> equinixResponse = this.invoke(equinixRequest);
        Page<FabricGateway, FabricGatewayJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<FabricGateway> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, FabricGatewayWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<FabricGateway> nextPage(PaginatedPostRequest<FabricGateway> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<FabricGateway> equinixResponse = this.invoke(equinixRequest);
        Page<FabricGateway, FabricGatewayJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<FabricGateway> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, FabricGatewayWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
