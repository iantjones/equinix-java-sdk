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
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import api.equinix.javasdk.customerportal.model.json.SecureCabinetJson;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetCreatorJson;
import api.equinix.javasdk.customerportal.model.wrappers.SecureCabinetWrapper;

import java.util.Map;

public class SecureCabinetClientImpl extends PageableBase implements SecureCabinetClient<SecureCabinet> {

    public SecureCabinetClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SecureCabinets");
    }

    public Page<SecureCabinet, SecureCabinetJson> list() {
        EquinixRequest<SecureCabinet> equinixRequest = this.buildRequest("ListSecureCabinets", RequestType.PAGINATED, SecureCabinetJson.getPagedTypeRef());
        EquinixResponse<SecureCabinet> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public SecureCabinetJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SecureCabinetJson> equinixRequest = this.buildRequestWithPathParams("GetSecureCabinet", RequestType.SINGLE, pParams, SecureCabinetJson.getSingleTypeRef());
        EquinixResponse<SecureCabinetJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SecureCabinetJson create(SecureCabinetCreatorJson secureCabinetCreatorJson) {
        EquinixRequest<SecureCabinetJson> equinixRequest = this.buildRequest("CreateSecureCabinet", RequestType.SINGLE, SecureCabinetJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, secureCabinetCreatorJson);
        EquinixResponse<SecureCabinetJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SecureCabinetJson update(String uuid, SecureCabinetCreatorJson secureCabinetCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<SecureCabinetJson> equinixRequest = this.buildRequestWithPathParams("UpdateSecureCabinet", RequestType.SINGLE, pParams, SecureCabinetJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, secureCabinetCreatorJson);
        EquinixResponse<SecureCabinetJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public SecureCabinetJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<SecureCabinet> nextPage(PaginatedRequest<SecureCabinet> equinixRequest) {
        EquinixResponse<SecureCabinet> equinixResponse = this.invoke(equinixRequest);
        Page<SecureCabinet, SecureCabinetJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<SecureCabinet> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, SecureCabinetWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
