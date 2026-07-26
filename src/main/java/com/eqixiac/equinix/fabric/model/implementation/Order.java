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

package com.eqixiac.equinix.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Order details for an ordered Fabric resource (the Fabric v4 {@code Order} schema, used by
 * Connection and Cloud Router requests and responses). On requests the writable members are
 * {@code purchaseOrderNumber}, {@code customerReferenceNumber} and {@code termLength}; the
 * fluent {@code with*} methods return {@code this} for chaining. {@code orderId},
 * {@code orderNumber}, {@code billingTier} and {@code contractedBandwidth} are assigned by
 * Equinix and only populated on responses.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("customerReferenceNumber")
    private String customerReferenceNumber;

    @JsonProperty("billingTier")
    private String billingTier;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("termLength")
    private Integer termLength;

    @JsonProperty("contractedBandwidth")
    private Integer contractedBandwidth;

    public Order(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    /**
     * Sets the purchase order number.
     *
     * @param purchaseOrderNumber the purchase order number
     * @return this order, for chaining
     */
    public Order withPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
        return this;
    }

    /**
     * Sets the customer reference number.
     *
     * @param customerReferenceNumber the customer's own order reference
     * @return this order, for chaining
     */
    public Order withCustomerReferenceNumber(String customerReferenceNumber) {
        this.customerReferenceNumber = customerReferenceNumber;
        return this;
    }

    /**
     * Sets the term length in months; valid values are 1, 12, 24 and 36, where 1 (the default)
     * is the on-demand case.
     *
     * @param termLength the term length in months
     * @return this order, for chaining
     */
    public Order withTermLength(Integer termLength) {
        this.termLength = termLength;
        return this;
    }
}
