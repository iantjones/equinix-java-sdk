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
import api.equinix.javasdk.customerportal.model.BillingAccountV2;
import api.equinix.javasdk.customerportal.model.json.creators.BillingAccountSearchRequest;

/**
 * Client interface for searching billing accounts via the Platform Billing Account v2 (BAS) API at
 * {@code /billing/v2/billingAccounts}.
 *
 * <p>{@link #search(BillingAccountSearchRequest)} pages over the customer's billing accounts
 * matching the supplied criteria, while {@link #getByAccountNumber(String)} and
 * {@link #getByAccountId(String)} fetch a single account by its number or id respectively.</p>
 */
public interface BillingAccountsSearch {

    /**
     * Searches the customer's billing accounts using the supplied criteria.
     *
     * @param request the search criteria
     * @return a paginated list of matching billing accounts
     */
    PaginatedList<BillingAccountV2> search(BillingAccountSearchRequest request);

    /**
     * Returns a single billing account by its account number.
     *
     * @param accountNumber the billing account number
     * @return the billing account detail
     */
    BillingAccountV2 getByAccountNumber(String accountNumber);

    /**
     * Returns a single billing account by its account id.
     *
     * @param accountId the billing account id
     * @return the billing account detail
     */
    BillingAccountV2 getByAccountId(String accountId);
}
