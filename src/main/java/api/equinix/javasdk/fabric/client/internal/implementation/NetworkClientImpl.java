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
import api.equinix.javasdk.fabric.client.internal.NetworkClient;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
import api.equinix.javasdk.fabric.model.json.creators.NetworkCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.NetworkWrapper;

import java.util.Map;

public class NetworkClientImpl extends PageableBase implements NetworkClient<Network> {

    public NetworkClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Networks");
    }

    public Page<Network, NetworkJson> search(FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<Network> equinixRequest = this.buildRequest("SearchNetworks", RequestType.PAGINATED_POST, NetworkJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredSortedPaginatedPost(filter, sort));
        EquinixResponse<Network> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public NetworkJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<NetworkJson> equinixRequest = this.buildRequestWithPathParams("GetNetwork", RequestType.SINGLE, pParams, NetworkJson.getSingleTypeRef());
        EquinixResponse<NetworkJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public NetworkJson create(NetworkCreatorJson networkCreatorJson) {
        EquinixRequest<NetworkJson> equinixRequest = this.buildRequest("PostNetwork", RequestType.SINGLE, NetworkJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, networkCreatorJson);
        EquinixResponse<NetworkJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public NetworkJson update(String uuid, NetworkCreatorJson networkCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<NetworkJson> equinixRequest = this.buildRequestWithPathParams("UpdateNetwork", RequestType.SINGLE, pParams, NetworkJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, networkCreatorJson);
        EquinixResponse<NetworkJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public NetworkJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Network> equinixRequest = this.buildRequestWithPathParams("DeleteNetwork", RequestType.SINGLE, pParams, NetworkJson.getSingleTypeRef());
        EquinixResponse<Network> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public NetworkJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Network> nextPage(PaginatedRequest<Network> equinixRequest) {
        EquinixResponse<Network> equinixResponse = this.invoke(equinixRequest);
        Page<Network, NetworkJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Network> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, NetworkWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<Network> nextPage(PaginatedPostRequest<Network> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<Network> equinixResponse = this.invoke(equinixRequest);
        Page<Network, NetworkJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<Network> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, NetworkWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
