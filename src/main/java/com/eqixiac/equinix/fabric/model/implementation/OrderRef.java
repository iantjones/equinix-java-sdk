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

/**
 * Common order details shared by Fabric order references: the order resource {@code href}, the
 * customer purchase-order number, and the resolved order number / order line.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OrderRef {

    @JsonProperty("href")
    private String href;

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("orderLine")
    private String orderLine;

    protected OrderRef() {
    }

    /**
     * Creates an order reference with the request-side attributes populated. The {@code href}
     * is server-assigned and remains unset.
     *
     * @param purchaseOrderNumber the customer purchase-order number
     * @param orderNumber the order reference number
     * @param orderLine the order line
     */
    protected OrderRef(String purchaseOrderNumber, String orderNumber, String orderLine) {
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.orderNumber = orderNumber;
        this.orderLine = orderLine;
    }
}
