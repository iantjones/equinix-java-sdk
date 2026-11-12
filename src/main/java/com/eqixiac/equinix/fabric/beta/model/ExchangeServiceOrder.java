/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.model;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Ordering and billing reference details for this Exchange Service.
 *
 * <p>Maps spec schema {@code ExchangeServiceOrder}. Unknown properties are ignored on read.</p>
 * <p>Request-capable: every property has a chained setter ({@code setX(...)} returns this), so an
 * instance doubles as its own builder.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ExchangeServiceOrder {

    /** Purchase order number reference. */
    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    /** Equinix-assigned order identifier. Read-only (server-populated). */
    @JsonProperty("orderId")
    private String orderId;

    /** Your own reference for this Exchange Service. */
    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    /** Equinix order reference number. */
    @JsonProperty("orderNumber")
    private String orderNumber;

    /** Order Reference Number for Sales Assisted Order Read-only (server-populated). */
    @JsonProperty("orderLineItemNumber")
    private String orderLineItemNumber;

    /** product domain Order Number for Sales Assisted Order Read-only (server-populated). */
    @JsonProperty("domainOrderNumber")
    private String domainOrderNumber;

    /** Equinix-assigned order identifier, this is a derived response attribute Read-only (server-populated). */
    @JsonProperty("uuid")
    private String uuid;
}
