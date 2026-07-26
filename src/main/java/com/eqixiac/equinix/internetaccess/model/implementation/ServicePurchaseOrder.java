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

package com.eqixiac.equinix.internetaccess.model.implementation;

import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Purchase order attached to an Equinix Internet Access (EIA) v2 service order.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicePurchaseOrder {

    @JsonProperty("type")
    private PurchaseOrderType type;

    @JsonProperty("number")
    private String number;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("attachment")
    private Attachment attachment;

    /**
     * Attachment reference of a {@link ServicePurchaseOrder}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {

        @JsonProperty("attachmentId")
        private String attachmentId;
    }
}
