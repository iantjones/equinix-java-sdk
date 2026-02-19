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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.InternetAccessServiceCreatorJson;
import api.equinix.javasdk.internetaccess.model.wrappers.InternetAccessServiceWrapper;

import java.util.Map;

public class InternetAccessServiceClientImpl extends PageableBase implements InternetAccessServiceClient<InternetAccessService> {

    public InternetAccessServiceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Services");
    }

    public Page<InternetAccessService, InternetAccessServiceJson> list() {
        EquinixRequest<InternetAccessService> equinixRequest = this.buildRequest("ListServices", RequestType.PAGINATED, InternetAccessServiceJson.getPagedTypeRef());
        EquinixResponse<InternetAccessService> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessServiceJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<InternetAccessServiceJson> equinixRequest = this.buildRequestWithPathParams("GetService", RequestType.SINGLE, pParams, InternetAccessServiceJson.getSingleTypeRef());
        EquinixResponse<InternetAccessServiceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessServiceJson create(InternetAccessServiceCreatorJson internetAccessServiceCreatorJson) {
        EquinixRequest<InternetAccessServiceJson> equinixRequest = this.buildRequest("CreateService", RequestType.SINGLE, InternetAccessServiceJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, internetAccessServiceCreatorJson);
        EquinixResponse<InternetAccessServiceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessServiceJson update(String uuid, InternetAccessServiceCreatorJson internetAccessServiceCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<InternetAccessServiceJson> equinixRequest = this.buildRequestWithPathParams("UpdateService", RequestType.SINGLE, pParams, InternetAccessServiceJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, internetAccessServiceCreatorJson);
        EquinixResponse<InternetAccessServiceJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessServiceJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<InternetAccessService> equinixRequest = this.buildRequestWithPathParams("DeleteService", RequestType.SINGLE, pParams, InternetAccessServiceJson.getSingleTypeRef());
        EquinixResponse<InternetAccessService> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public InternetAccessServiceJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<InternetAccessService> nextPage(PaginatedRequest<InternetAccessService> equinixRequest) {
        EquinixResponse<InternetAccessService> equinixResponse = this.invoke(equinixRequest);
        Page<InternetAccessService, InternetAccessServiceJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<InternetAccessService> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, InternetAccessServiceWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
