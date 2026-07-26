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

import com.eqixiac.equinix.fabric.enums.PortPurchaseOrderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Purchase order information associated with a port order (the Fabric v4
 * {@code PortOrderPurchaseOrder} schema). {@code type} and the deprecated
 * {@code selectionType} take the values {@code EXEMPTION}, {@code EXISTING},
 * {@code NEW} or {@code BLANKET}.
 *
 * <p>Prefer {@code builder()} over the positional constructor — five {@code String}
 * parameters plus two identically-typed enum parameters make positional construction
 * transposition-prone.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOrderPurchaseOrder {

    @JsonProperty("number")
    private String number;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("attachmentId")
    private String attachmentId;

    /**
     * @deprecated the API marks {@code selectionType} deprecated; use {@code getType()} instead.
     */
    @Deprecated
    @JsonProperty("selectionType")
    private PortPurchaseOrderType selectionType;

    @JsonProperty("type")
    private PortPurchaseOrderType type;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (five same-typed {@code String} parameters and
     * two identically-typed {@link PortPurchaseOrderType} parameters) rather than by field
     * declaration order.
     *
     * @param number        the purchase order number
     * @param amount        the purchase order amount
     * @param startDate     the purchase order start date
     * @param endDate       the purchase order end date
     * @param attachmentId  the purchase order attachment id
     * @param selectionType the deprecated selection type (prefer {@code type})
     * @param type          the purchase order type
     */
    @Builder
    public PortOrderPurchaseOrder(String number, String amount, String startDate, String endDate,
                                  String attachmentId, PortPurchaseOrderType selectionType,
                                  PortPurchaseOrderType type) {
        this.number = number;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.attachmentId = attachmentId;
        this.selectionType = selectionType;
        this.type = type;
    }
}
