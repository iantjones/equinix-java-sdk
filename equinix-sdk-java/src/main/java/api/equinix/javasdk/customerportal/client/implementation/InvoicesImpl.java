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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.Invoices;
import api.equinix.javasdk.customerportal.client.RequestBuilder;
import api.equinix.javasdk.customerportal.client.internal.InvoiceDetailClient;
import api.equinix.javasdk.customerportal.client.internal.InvoiceSummaryClient;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.json.InvoiceDetailJson;
import api.equinix.javasdk.customerportal.model.json.InvoiceSummaryJson;
import api.equinix.javasdk.customerportal.model.wrappers.InvoiceDetailWrapper;
import api.equinix.javasdk.customerportal.model.wrappers.InvoiceSummaryWrapper;

public class InvoicesImpl implements Invoices {

    private final CustomerPortal serviceManager;

    private final InvoiceSummaryClient<InvoiceSummary> serviceClientSummary;

    private final InvoiceDetailClient<InvoiceDetail> serviceClientDetail;

    public InvoicesImpl(InvoiceSummaryClient<InvoiceSummary> serviceClientSummary, InvoiceDetailClient<InvoiceDetail> serviceClientDetail,
                        CustomerPortal serviceManager) {
        this.serviceClientSummary = serviceClientSummary;
        this.serviceClientDetail = serviceClientDetail;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<InvoiceSummary> summaries() {
        return summaries(null);
    }

    public PaginatedList<InvoiceSummary> summaries(RequestBuilder.Invoice requestBuilder) {
        Page<InvoiceSummary, InvoiceSummaryJson> responsePage = this.serviceClientSummary.list(requestBuilder);
        PaginatedList<InvoiceSummary> invoiceSummaryList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClientSummary, InvoiceSummaryWrapper::new);
        return new PaginatedList<>(invoiceSummaryList, this.serviceClientSummary, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<InvoiceDetail> details() {
        return details(null);
    }

    public PaginatedList<InvoiceDetail> details(RequestBuilder.Invoice requestBuilder) {
        Page<InvoiceDetail, InvoiceDetailJson> responsePage = this.serviceClientDetail.list(requestBuilder);
        PaginatedList<InvoiceDetail> invoiceDetailList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClientDetail, InvoiceDetailWrapper::new);
        return new PaginatedList<>(invoiceDetailList, this.serviceClientDetail, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
