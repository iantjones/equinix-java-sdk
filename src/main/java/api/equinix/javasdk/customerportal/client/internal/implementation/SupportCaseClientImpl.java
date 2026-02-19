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
import api.equinix.javasdk.customerportal.client.internal.SupportCaseClient;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.SupportCaseWrapper;

import java.util.Map;

public class SupportCaseClientImpl extends PageableBase implements SupportCaseClient<SupportCase> {

    public SupportCaseClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SupportCases");
    }

    public Page<SupportCase, SupportCaseJson> list() {
        EquinixRequest<SupportCase> equinixRequest = this.buildRequest("ListSupportCases", RequestType.PAGINATED, SupportCaseJson.getPagedTypeRef());
        EquinixResponse<SupportCase> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public SupportCaseJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SupportCaseJson> equinixRequest = this.buildRequestWithPathParams("GetSupportCase", RequestType.SINGLE, pParams, SupportCaseJson.getSingleTypeRef());
        EquinixResponse<SupportCaseJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SupportCaseJson create(SupportCaseCreatorJson supportCaseCreatorJson) {
        EquinixRequest<SupportCaseJson> equinixRequest = this.buildRequest("CreateSupportCase", RequestType.SINGLE, SupportCaseJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, supportCaseCreatorJson);
        EquinixResponse<SupportCaseJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SupportCaseJson update(String uuid, SupportCaseCreatorJson supportCaseCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SupportCaseJson> equinixRequest = this.buildRequestWithPathParams("UpdateSupportCase", RequestType.SINGLE, pParams, SupportCaseJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, supportCaseCreatorJson);
        EquinixResponse<SupportCaseJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SupportCaseJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SupportCase> equinixRequest = this.buildRequestWithPathParams("UpdateSupportCase", RequestType.SINGLE, pParams, SupportCaseJson.getSingleTypeRef());
        EquinixResponse<SupportCase> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SupportCaseJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<SupportCase> nextPage(PaginatedRequest<SupportCase> equinixRequest) {
        EquinixResponse<SupportCase> equinixResponse = this.invoke(equinixRequest);
        Page<SupportCase, SupportCaseJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SupportCase> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SupportCaseWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
