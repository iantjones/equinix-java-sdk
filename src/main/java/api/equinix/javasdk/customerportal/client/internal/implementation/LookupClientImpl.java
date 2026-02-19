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
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.json.LookupLocationJson;

import java.util.Map;

public class LookupClientImpl extends PageableBase implements LookupClient<LookupLocation> {

    public LookupClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Lookup");
    }

    public Page<LookupLocation, LookupLocationJson> listLocations() {
        EquinixRequest<LookupLocation> equinixRequest = this.buildRequest("ListLocations", RequestType.PAGINATED, LookupLocationJson.getPagedTypeRef());
        EquinixResponse<LookupLocation> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public LookupLocationJson getLocationByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<LookupLocationJson> equinixRequest = this.buildRequestWithPathParams("GetLocation", RequestType.SINGLE, pParams, LookupLocationJson.getSingleTypeRef());
        EquinixResponse<LookupLocationJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public Page<LookupLocation, LookupLocationJson> listPatchPanels() {
        EquinixRequest<LookupLocation> equinixRequest = this.buildRequest("ListPatchPanels", RequestType.PAGINATED, LookupLocationJson.getPagedTypeRef());
        EquinixResponse<LookupLocation> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<LookupLocation> nextPage(PaginatedRequest<LookupLocation> equinixRequest) {
        EquinixResponse<LookupLocation> equinixResponse = this.invoke(equinixRequest);
        Page<LookupLocation, LookupLocationJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<LookupLocation> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, (json, client) -> json);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
