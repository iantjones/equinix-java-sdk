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
import api.equinix.javasdk.fabric.client.internal.CloudRouterClient;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.CloudRouterWrapper;

import java.util.Map;

public class CloudRouterClientImpl extends PageableBase implements CloudRouterClient<CloudRouter> {

    public CloudRouterClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters");
    }

    public Page<CloudRouter, CloudRouterJson> search(FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<CloudRouter> equinixRequest = this.buildRequest("SearchCloudRouters", RequestType.PAGINATED_POST, CloudRouterJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredSortedPaginatedPost(filter, sort));
        EquinixResponse<CloudRouter> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public CloudRouterJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<CloudRouterJson> equinixRequest = this.buildRequestWithPathParams("GetCloudRouter", RequestType.SINGLE, pParams, CloudRouterJson.getSingleTypeRef());
        EquinixResponse<CloudRouterJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CloudRouterJson create(CloudRouterCreatorJson cloudRouterCreatorJson) {
        EquinixRequest<CloudRouterJson> equinixRequest = this.buildRequest("PostCloudRouter", RequestType.SINGLE, CloudRouterJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, cloudRouterCreatorJson);
        EquinixResponse<CloudRouterJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CloudRouterJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<CloudRouter> equinixRequest = this.buildRequestWithPathParams("DeleteCloudRouter", RequestType.SINGLE, pParams, CloudRouterJson.getSingleTypeRef());
        EquinixResponse<CloudRouter> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CloudRouterJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<CloudRouter> nextPage(PaginatedRequest<CloudRouter> equinixRequest) {
        EquinixResponse<CloudRouter> equinixResponse = this.invoke(equinixRequest);
        Page<CloudRouter, CloudRouterJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<CloudRouter> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, CloudRouterWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<CloudRouter> nextPage(PaginatedPostRequest<CloudRouter> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<CloudRouter> equinixResponse = this.invoke(equinixRequest);
        Page<CloudRouter, CloudRouterJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<CloudRouter> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, CloudRouterWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
