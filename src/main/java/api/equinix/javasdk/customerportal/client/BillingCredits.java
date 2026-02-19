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
import api.equinix.javasdk.customerportal.model.BillingCredit;

/**
 * Client interface for accessing billing credits in the Equinix Customer Portal.
 * Provides read-only operations to list and retrieve billing credit entries
 * applied to the customer account.
 */
public interface BillingCredits {

    /**
     * Lists all billing credits for the current account.
     *
     * @return a paginated list of billing credits
     */
    PaginatedList<BillingCredit> list();

    /**
     * Retrieves a specific billing credit by its unique identifier.
     *
     * @param uuid the unique identifier of the billing credit
     * @return the matching billing credit
     */
    BillingCredit getByUuid(String uuid);
}
