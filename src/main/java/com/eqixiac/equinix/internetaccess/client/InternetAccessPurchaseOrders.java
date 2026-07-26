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
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderCategory;
import com.eqixiac.equinix.internetaccess.model.PurchaseOrder;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 purchase-order lookups — the list
 * ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders}) and the single get
 * ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders/{number}}).
 */
public interface InternetAccessPurchaseOrders {

    /**
     * Returns the purchase orders available to the given billing account.
     *
     * @param accountNumber the billing account number
     * @return a paginated list of purchase orders
     */
    PaginatedList<PurchaseOrder> list(String accountNumber);

    /**
     * Returns the purchase orders available to the given billing account, narrowed by IBX location
     * and/or service category.
     *
     * @param accountNumber the billing account number
     * @param ibx the IBX code to filter on, or {@code null} for no location constraint
     * @param category the service category to filter on, or {@code null} for no category constraint
     * @return a paginated list of matching purchase orders
     */
    PaginatedList<PurchaseOrder> list(String accountNumber, String ibx, PurchaseOrderCategory category);

    /**
     * Returns a single purchase order by number.
     *
     * @param accountNumber the billing account number
     * @param number the purchase order number
     * @return the purchase order
     */
    PurchaseOrder get(String accountNumber, String number);
}
