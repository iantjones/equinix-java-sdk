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

package com.eqixiac.equinix.internetaccess.model;

import com.eqixiac.equinix.internetaccess.model.implementation.AccountBilling;
import com.eqixiac.equinix.internetaccess.model.implementation.AccountOperationalUnit;

import java.util.List;

/**
 * An Equinix Internet Access (EIA) v1 billing account, as returned by the account lookups
 * {@code GET /internetAccess/v1/accounts} and {@code GET /internetAccess/v1/accounts/{accountNumber}}.
 *
 * <p>This is a read-only response view.</p>
 */
public interface AccountDetails {

    /**
     * @return the account number
     */
    String getAccountNumber();

    /**
     * @return the account name
     */
    String getAccountName();

    /**
     * @return the billing data for the account
     */
    AccountBilling getBilling();

    /**
     * @return the operational units (IBXs and metros) associated with the account
     */
    List<AccountOperationalUnit> getOperationalUnits();

    /**
     * @return the organization identifier of the account
     */
    String getOrgId();

    /**
     * @return the organization name of the account
     */
    String getOrganizationName();
}
