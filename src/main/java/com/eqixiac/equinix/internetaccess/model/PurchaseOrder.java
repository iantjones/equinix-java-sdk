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

import com.eqixiac.equinix.internetaccess.enums.CurrencyCode;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderCategory;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderStatus;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderType;
import com.eqixiac.equinix.internetaccess.model.implementation.PurchaseOrderAccount;
import com.eqixiac.equinix.internetaccess.model.implementation.PurchaseOrderLocation;

import java.math.BigDecimal;
import java.util.List;

/**
 * A purchase order available to a billing account, as returned by the Equinix Internet Access (EIA)
 * v1 purchase-order lookups — the list ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders})
 * and the single get ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders/{number}}).
 *
 * <p>This is a read-only response view.</p>
 */
public interface PurchaseOrder {

    /**
     * @return the purchase order type ({@code STANDARD_PURCHASE_ORDER} or {@code BLANKET_PURCHASE_ORDER})
     */
    PurchaseOrderType getType();

    /**
     * @return the purchase order number
     */
    String getNumber();

    /**
     * @return the billing account the purchase order belongs to
     */
    PurchaseOrderAccount getAccount();

    /**
     * @return the service categories the purchase order applies to
     */
    List<PurchaseOrderCategory> getCategories();

    /**
     * @return the IBX locations the purchase order applies to
     */
    List<PurchaseOrderLocation> getLocations();

    /**
     * @return the purchase order amount
     */
    BigDecimal getAmount();

    /**
     * @return the start date of the purchase order
     */
    String getStartDate();

    /**
     * @return the end date of the purchase order
     */
    String getEndDate();

    /**
     * @return the description of the purchase order
     */
    String getDescription();

    /**
     * @return whether the purchase order is a draft
     */
    Boolean getDraft();

    /**
     * @return the lifecycle status of the purchase order ({@code DRAFT} or {@code ACTIVE})
     */
    PurchaseOrderStatus getStatus();

    /**
     * @return the ISO 4217 currency code of the purchase order amount
     */
    CurrencyCode getCurrency();
}
