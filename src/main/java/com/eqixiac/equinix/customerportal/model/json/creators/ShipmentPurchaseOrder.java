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
 * Purchase-order details on a v1 shipment order ({@code purchaseOrder} in the shipments v1
 * spec). All fields are optional: start/end dates (ISO date-time), the purchase order number
 * and the amount of money the purchase order contains ({@code price.amount}, measured in the
 * currency of the customer's account).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentPurchaseOrder {

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("number")
    private String number;

    @JsonProperty("price")
    private Price price;

    public ShipmentPurchaseOrder startDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public ShipmentPurchaseOrder endDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public ShipmentPurchaseOrder number(String number) {
        this.number = number;
        return this;
    }

    public ShipmentPurchaseOrder price(Double amount) {
        this.price = new Price(amount);
        return this;
    }

    /**
     * The amount of money the purchase order contains ({@code price} in the shipments v1 spec).
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Price {

        @JsonProperty("amount")
        private final Double amount;

        public Price(Double amount) {
            this.amount = amount;
        }
    }
}
