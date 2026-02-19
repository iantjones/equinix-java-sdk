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
import api.equinix.javasdk.customerportal.client.internal.DigitalLOAClient;
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.json.DigitalLOAJson;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLOACreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.DigitalLOAWrapper;

import java.util.Map;

public class DigitalLOAClientImpl extends PageableBase implements DigitalLOAClient<DigitalLOA> {

    public DigitalLOAClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "DigitalLOA");
    }

    public Page<DigitalLOA, DigitalLOAJson> list() {
        EquinixRequest<DigitalLOA> equinixRequest = this.buildRequest("ListDigitalLOAs", RequestType.PAGINATED, DigitalLOAJson.getPagedTypeRef());
        EquinixResponse<DigitalLOA> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public DigitalLOAJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<DigitalLOAJson> equinixRequest = this.buildRequestWithPathParams("GetDigitalLOA", RequestType.SINGLE, pParams, DigitalLOAJson.getSingleTypeRef());
        EquinixResponse<DigitalLOAJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public DigitalLOAJson create(DigitalLOACreatorJson digitalLOACreatorJson) {
        EquinixRequest<DigitalLOAJson> equinixRequest = this.buildRequest("CreateDigitalLOA", RequestType.SINGLE, DigitalLOAJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, digitalLOACreatorJson);
        EquinixResponse<DigitalLOAJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public DigitalLOAJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<DigitalLOA> nextPage(PaginatedRequest<DigitalLOA> equinixRequest) {
        EquinixResponse<DigitalLOA> equinixResponse = this.invoke(equinixRequest);
        Page<DigitalLOA, DigitalLOAJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<DigitalLOA> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, DigitalLOAWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
