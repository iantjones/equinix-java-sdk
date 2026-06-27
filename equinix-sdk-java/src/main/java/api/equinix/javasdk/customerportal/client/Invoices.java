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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.core.http.response.PaginatedList;

/**
 * Client interface for accessing invoice data in the Equinix Customer Portal.
 * Provides operations to retrieve invoice summaries and line-item details,
 * with optional filtering by date range, account number, and transaction ID.
 */
public interface Invoices {

    /**
     * Lists all invoice summaries for the current account.
     *
     * @return a paginated list of invoice summaries
     */
    PaginatedList<InvoiceSummary> summaries();

    /**
     * Lists invoice summaries matching the specified filter criteria.
     *
     * @param requestBuilder the query parameters for filtering invoices
     * @return a paginated list of matching invoice summaries
     */
    PaginatedList<InvoiceSummary> summaries(RequestBuilder.Invoice requestBuilder);

    /**
     * Lists all invoice details for the current account.
     *
     * @return a paginated list of invoice details
     */
    PaginatedList<InvoiceDetail> details();

    /**
     * Lists invoice details matching the specified filter criteria.
     *
     * @param requestBuilder the query parameters for filtering invoices
     * @return a paginated list of matching invoice details
     */
    PaginatedList<InvoiceDetail> details(RequestBuilder.Invoice requestBuilder);
}
