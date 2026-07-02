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

package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.PurchaseOrderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The purchase order referenced by an order line ({@code purchaseOrder}): its {@code type}
 * ({@code EXEMPTED}, {@code NEW} or {@code EXISTING}), {@code number} and {@code closingDate}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPurchaseOrder {

    @JsonProperty("type")
    private PurchaseOrderType type;

    @JsonProperty("number")
    private String number;

    @JsonProperty("closingDate")
    private String closingDate;
}
