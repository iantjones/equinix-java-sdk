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

package com.eqixiac.equinix.internetaccess.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.model.AccountAgreement;
import com.eqixiac.equinix.internetaccess.model.AccountDetails;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 account lookups — the billing accounts
 * available at an IBX ({@code GET /internetAccess/v1/accounts}), a single account by number
 * ({@code GET /internetAccess/v1/accounts/{accountNumber}}) and an account's agreements
 * ({@code GET /internetAccess/v1/accounts/{accountNumber}/agreements}).
 */
public interface InternetAccessAccounts {

    /**
     * Returns the accounts available at the given IBX.
     *
     * @param operationalUnitsIbx the IBX to look up accounts for
     * @return a paginated list of matching accounts
     */
    PaginatedList<AccountDetails> list(String operationalUnitsIbx);

    /**
     * Returns the accounts available at the given IBX, narrowed to a project.
     *
     * @param operationalUnitsIbx the IBX to look up accounts for
     * @param projectId the project identifier to narrow accounts to, or {@code null} for no
     *                  project constraint
     * @return a paginated list of matching accounts
     */
    PaginatedList<AccountDetails> list(String operationalUnitsIbx, String projectId);

    /**
     * Returns the account with the given account number.
     *
     * @param accountNumber the account number
     * @return the matching account
     */
    AccountDetails getByNumber(String accountNumber);

    /**
     * Returns the agreements associated with the given account at an IBX.
     *
     * @param accountNumber the account number
     * @param ibx the IBX to look up agreements for
     * @return a paginated list of matching agreements
     */
    PaginatedList<AccountAgreement> agreements(String accountNumber, String ibx);
}
