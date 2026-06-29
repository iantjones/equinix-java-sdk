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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.BillingAccount;

/**
 * Client interface for viewing billing accounts and downloading invoice documents in the Equinix
 * Customer Portal.
 *
 * <p>Backed by the Billing v1 API at {@code /v1/finance/accounts}. {@link #summaries()} pages over
 * the billing accounts the current user may view, {@link #getByAccountNumber(String)} fetches a
 * single account's summary, and {@link #downloadInvoiceDocument(String, String, String)} retrieves
 * an invoice document as raw bytes.</p>
 */
public interface BillingAccounts {

    /**
     * Pages over a summary of all the billing accounts that the current user has permission to view.
     *
     * @return a paginated list of billing account summaries
     */
    PaginatedList<BillingAccount> summaries();

    /**
     * Pages over a summary of all the billing accounts that the current user has permission to view,
     * sorted by the supplied sort specifier.
     *
     * @param sorts the sort specifier, e.g. {@code "ACCOUNT_NUMBER"} ascending or
     *              {@code "-ACCOUNT_NUMBER"} descending (may be {@code null})
     * @return a paginated list of billing account summaries
     */
    PaginatedList<BillingAccount> summaries(String sorts);

    /**
     * Returns the billing summary of a single account by its account number.
     *
     * @param accountNumber the billing account number
     * @return the billing account summary
     */
    BillingAccount getByAccountNumber(String accountNumber);

    /**
     * Returns the billing detail of a single account by its account number, restricting the
     * available finance documents to the supplied months.
     *
     * @param accountNumber the billing account number
     * @param months        the months to pull finance documents for, as a comma-separated list of
     *                      ISO local dates (e.g. {@code "2017-12-03,2018-01-03"}); may be {@code null}
     * @return the billing account detail
     */
    BillingAccount getByAccountNumber(String accountNumber, String months);

    /**
     * Downloads a specific invoice document for an account by its invoice and document identifiers.
     *
     * @param accountNumber the billing account number
     * @param invoiceId     the invoice identifier
     * @param documentId    the document identifier
     * @return the document content as raw bytes
     */
    byte[] downloadInvoiceDocument(String accountNumber, String invoiceId, String documentId);
}
