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

package com.eqixiac.equinix.customerportal.model.json;

import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

/**
 * JSON model for an order submission result. Deserializes the Secure Cabinet v1 body
 * ({@code orderNumber}) and is also constructed directly from the {@code Location} header order
 * id returned by the colocation v2 order APIs.
 */
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponseJson implements OrderResponse {

    @JsonProperty("orderNumber")
    private String orderNumber;

    public OrderResponseJson(String orderId) {
        this.orderNumber = orderId;
    }

    @Override
    public String getOrderId() {
        return this.orderNumber;
    }
}
