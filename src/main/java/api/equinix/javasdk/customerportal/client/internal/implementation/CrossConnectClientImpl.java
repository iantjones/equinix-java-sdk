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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.CrossConnectClient;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.json.CrossConnectJson;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.CrossConnectWrapper;

import java.util.Map;

public class CrossConnectClientImpl extends PageableBase implements CrossConnectClient<CrossConnect> {

    public CrossConnectClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "CrossConnects");
    }

    public Page<CrossConnect, CrossConnectJson> list() {
        EquinixRequest<CrossConnect> equinixRequest = this.buildRequest("ListCrossConnects", RequestType.PAGINATED, CrossConnectJson.getPagedTypeRef());
        EquinixResponse<CrossConnect> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public CrossConnectJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<CrossConnectJson> equinixRequest = this.buildRequestWithPathParams("GetCrossConnect", RequestType.SINGLE, pParams, CrossConnectJson.getSingleTypeRef());
        EquinixResponse<CrossConnectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CrossConnectJson create(CrossConnectCreatorJson crossConnectCreatorJson) {
        EquinixRequest<CrossConnectJson> equinixRequest = this.buildRequest("CreateCrossConnect", RequestType.SINGLE, CrossConnectJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, crossConnectCreatorJson);
        EquinixResponse<CrossConnectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CrossConnectJson update(String uuid, CrossConnectCreatorJson crossConnectCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<CrossConnectJson> equinixRequest = this.buildRequestWithPathParams("UpdateCrossConnect", RequestType.SINGLE, pParams, CrossConnectJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, crossConnectCreatorJson);
        EquinixResponse<CrossConnectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CrossConnectJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<CrossConnect> equinixRequest = this.buildRequestWithPathParams("DeleteCrossConnect", RequestType.SINGLE, pParams, CrossConnectJson.getSingleTypeRef());
        EquinixResponse<CrossConnect> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public CrossConnectJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<CrossConnect> nextPage(PaginatedRequest<CrossConnect> equinixRequest) {
        EquinixResponse<CrossConnect> equinixResponse = this.invoke(equinixRequest);
        Page<CrossConnect, CrossConnectJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<CrossConnect> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, CrossConnectWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
