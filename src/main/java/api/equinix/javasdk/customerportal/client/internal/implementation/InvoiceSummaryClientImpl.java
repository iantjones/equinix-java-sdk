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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.customerportal.client.RequestBuilder;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.InvoiceSummaryClient;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.json.InvoiceDetailJson;
import api.equinix.javasdk.customerportal.model.json.InvoiceSummaryJson;
import api.equinix.javasdk.customerportal.model.wrappers.InvoiceSummaryWrapper;

import java.util.List;
import java.util.Map;

public class InvoiceSummaryClientImpl extends PageableBase implements InvoiceSummaryClient<InvoiceSummary> {

    public InvoiceSummaryClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Invoices");
    }

    public Page<InvoiceSummary, InvoiceSummaryJson> list(RequestBuilder.Invoice requestBuilder) {
        Map<String, List<String>> qParams = Utils.processRequestBuilder(requestBuilder);
        EquinixRequest<InvoiceSummary> equinixRequest = this.buildRequestWithQueryParams("ListInvoiceSummaries", RequestType.PAGINATED, qParams, InvoiceSummaryJson.getPagedTypeRef());
        EquinixResponse<InvoiceSummary> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<InvoiceSummary> nextPage(PaginatedRequest<InvoiceSummary> equinixRequest) {
        EquinixResponse<InvoiceSummary> equinixResponse = this.invoke(equinixRequest);
        Page<InvoiceSummary, InvoiceSummaryJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<InvoiceSummary> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, InvoiceSummaryWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
