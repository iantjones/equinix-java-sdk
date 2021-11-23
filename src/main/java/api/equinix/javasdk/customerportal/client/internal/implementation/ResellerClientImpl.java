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
import api.equinix.javasdk.customerportal.client.internal.ResellerClient;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.json.ResellerJson;
import api.equinix.javasdk.customerportal.model.wrappers.ResellerWrapper;

public class ResellerClientImpl extends PageableBase implements ResellerClient<Reseller> {

    public ResellerClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Resellers");
    }

    public Page<Reseller, ResellerJson> list() {
        EquinixRequest<Reseller> equinixRequest = this.buildRequest("ListResellers", RequestType.PAGINATED, ResellerJson.getPagedTypeRef());
        EquinixResponse<Reseller> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<Reseller> nextPage(PaginatedRequest<Reseller> equinixRequest) {
        EquinixResponse<Reseller> equinixResponse = this.invoke(equinixRequest);
        Page<Reseller, ResellerJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Reseller> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ResellerWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
