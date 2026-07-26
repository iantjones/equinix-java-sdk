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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.Invoices;
import com.eqixiac.equinix.customerportal.client.RequestBuilder;
import com.eqixiac.equinix.customerportal.client.internal.InvoiceDetailClient;
import com.eqixiac.equinix.customerportal.client.internal.InvoiceSummaryClient;
import com.eqixiac.equinix.customerportal.model.InvoiceDetail;
import com.eqixiac.equinix.customerportal.model.InvoiceSummary;
import com.eqixiac.equinix.customerportal.model.json.InvoiceDetailJson;
import com.eqixiac.equinix.customerportal.model.json.InvoiceSummaryJson;
import com.eqixiac.equinix.customerportal.model.wrappers.InvoiceDetailWrapper;
import com.eqixiac.equinix.customerportal.model.wrappers.InvoiceSummaryWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InvoicesImpl implements Invoices {

    private final InvoiceSummaryClient<InvoiceSummary> serviceClientSummary;

    private final InvoiceDetailClient<InvoiceDetail> serviceClientDetail;

    private final CustomerPortal serviceManager;

    public PaginatedList<InvoiceSummary> summaries() {
        return summaries(null);
    }

    public PaginatedList<InvoiceSummary> summaries(RequestBuilder.Invoice requestBuilder) {
        Page<InvoiceSummaryJson> responsePage = this.serviceClientSummary.list(requestBuilder);
        PaginatedList<InvoiceSummary> invoiceSummaryList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClientSummary, InvoiceSummaryWrapper::new);
        return new PaginatedList<>(invoiceSummaryList, this.serviceClientSummary, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<InvoiceDetail> details() {
        return details(null);
    }

    public PaginatedList<InvoiceDetail> details(RequestBuilder.Invoice requestBuilder) {
        Page<InvoiceDetailJson> responsePage = this.serviceClientDetail.list(requestBuilder);
        PaginatedList<InvoiceDetail> invoiceDetailList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClientDetail, InvoiceDetailWrapper::new);
        return new PaginatedList<>(invoiceDetailList, this.serviceClientDetail, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
