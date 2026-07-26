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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Order details for a Precision Time service (the Fabric v4 {@code precisionTimeOrder} schema).
 *
 * <p>Prefer {@code builder()} over the positional constructor — all three parameters are
 * interchangeable order-number {@code String}s.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrecisionTimeOrder {

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("customerReferenceNumber")
    private String customerReferenceNumber;

    @JsonProperty("orderNumber")
    private String orderNumber;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (three same-typed {@code String} parameters)
     * rather than by field declaration order.
     *
     * @param purchaseOrderNumber     the purchase order number
     * @param customerReferenceNumber the customer reference number
     * @param orderNumber             the Equinix order number
     */
    @Builder
    public PrecisionTimeOrder(String purchaseOrderNumber, String customerReferenceNumber, String orderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.customerReferenceNumber = customerReferenceNumber;
        this.orderNumber = orderNumber;
    }
}
