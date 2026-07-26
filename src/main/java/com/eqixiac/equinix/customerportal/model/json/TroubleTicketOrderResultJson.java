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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Raw result body of a placed trouble ticket order. The trouble ticket v1 API returns the
 * generated order number under the capitalized {@code OrderNumber} property; this model captures it
 * so the public API can adapt it to the shared {@link com.eqixiac.equinix.customerportal.model.OrderResponse}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroubleTicketOrderResultJson {

    @JsonProperty("OrderNumber")
    private String orderNumber;
}
