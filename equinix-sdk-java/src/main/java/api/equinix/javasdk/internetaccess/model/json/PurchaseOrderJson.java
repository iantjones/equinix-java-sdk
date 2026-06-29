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

package api.equinix.javasdk.internetaccess.model.json;

import api.equinix.javasdk.internetaccess.enums.CurrencyCode;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderCategory;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderStatus;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderType;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import api.equinix.javasdk.internetaccess.model.implementation.PurchaseOrderAccount;
import api.equinix.javasdk.internetaccess.model.implementation.PurchaseOrderLocation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only JSON model for a {@link PurchaseOrder} returned by the Equinix Internet Access (EIA) v1
 * purchase-order lookups — the list ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders})
 * and the single get ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders/{number}}).
 * Implements {@link PurchaseOrder} directly, so no wrapper is required.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseOrderJson implements PurchaseOrder {

    @JsonProperty("type")
    private PurchaseOrderType type;

    @JsonProperty("number")
    private String number;

    @JsonProperty("account")
    private PurchaseOrderAccount account;

    @JsonProperty("categories")
    private List<PurchaseOrderCategory> categories;

    @JsonProperty("locations")
    private List<PurchaseOrderLocation> locations;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("draft")
    private Boolean draft;

    @JsonProperty("status")
    private PurchaseOrderStatus status;

    @JsonProperty("currency")
    private CurrencyCode currency;
}
