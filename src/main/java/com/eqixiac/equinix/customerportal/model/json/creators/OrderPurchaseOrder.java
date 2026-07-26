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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Purchase order reference ({@code purchaseOrder}) for a colocation v2 order. When {@code type}
 * is {@code EXEMPTED} no number is required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPurchaseOrder {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("number")
    private final String number;

    @JsonProperty("startDate")
    private final String startDate;

    @JsonProperty("endDate")
    private final String endDate;

    @JsonProperty("amount")
    private final Double amount;

    @JsonProperty("attachmentId")
    private final String attachmentId;

    public OrderPurchaseOrder(String type, String number) {
        this(type, number, null, null, null, null);
    }

    public OrderPurchaseOrder(String type, String number, String startDate, String endDate, Double amount) {
        this(type, number, startDate, endDate, amount, null);
    }

    /**
     * Builds a purchase-order reference including a previously uploaded PO document.
     *
     * @param type         the purchase order type ({@code EXEMPTED}, {@code NEW} or {@code EXISTING})
     * @param number       the purchase order number
     * @param startDate    the purchase order start date
     * @param endDate      the purchase order end date
     * @param amount       the purchase order amount
     * @param attachmentId the id of a previously uploaded purchase-order attachment
     */
    public OrderPurchaseOrder(String type, String number, String startDate, String endDate, Double amount, String attachmentId) {
        this.type = type;
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.attachmentId = attachmentId;
    }
}
