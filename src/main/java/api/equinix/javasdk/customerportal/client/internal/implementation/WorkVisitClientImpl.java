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
import api.equinix.javasdk.customerportal.client.internal.WorkVisitClient;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.WorkVisitWrapper;

import java.util.Map;

public class WorkVisitClientImpl extends PageableBase implements WorkVisitClient<WorkVisit> {

    public WorkVisitClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "WorkVisits");
    }

    public Page<WorkVisit, WorkVisitJson> list() {
        EquinixRequest<WorkVisit> equinixRequest = this.buildRequest("ListWorkVisits", RequestType.PAGINATED, WorkVisitJson.getPagedTypeRef());
        EquinixResponse<WorkVisit> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public WorkVisitJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<WorkVisitJson> equinixRequest = this.buildRequestWithPathParams("GetWorkVisit", RequestType.SINGLE, pParams, WorkVisitJson.getSingleTypeRef());
        EquinixResponse<WorkVisitJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public WorkVisitJson create(WorkVisitCreatorJson workVisitCreatorJson) {
        EquinixRequest<WorkVisitJson> equinixRequest = this.buildRequest("PostWorkVisit", RequestType.SINGLE, WorkVisitJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, workVisitCreatorJson);
        EquinixResponse<WorkVisitJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public WorkVisitJson update(String uuid, WorkVisitCreatorJson workVisitCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<WorkVisitJson> equinixRequest = this.buildRequestWithPathParams("UpdateWorkVisit", RequestType.SINGLE, pParams, WorkVisitJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, workVisitCreatorJson);
        EquinixResponse<WorkVisitJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public WorkVisitJson cancel(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<WorkVisitJson> equinixRequest = this.buildRequestWithPathParams("CancelWorkVisit", RequestType.SINGLE, pParams, WorkVisitJson.getSingleTypeRef());
        EquinixResponse<WorkVisitJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public WorkVisitJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<WorkVisit> nextPage(PaginatedRequest<WorkVisit> equinixRequest) {
        EquinixResponse<WorkVisit> equinixResponse = this.invoke(equinixRequest);
        Page<WorkVisit, WorkVisitJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<WorkVisit> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, WorkVisitWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
