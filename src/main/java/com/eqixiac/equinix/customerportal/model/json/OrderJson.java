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

import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.customerportal.enums.OrderStatus;
import com.eqixiac.equinix.customerportal.enums.QuoteRequestType;
import com.eqixiac.equinix.customerportal.enums.SubChannel;
import com.eqixiac.equinix.customerportal.model.implementation.AdditionalInfo;
import com.eqixiac.equinix.customerportal.model.implementation.OrderContactInfo;
import com.eqixiac.equinix.customerportal.model.implementation.OrderLine;
import com.eqixiac.equinix.customerportal.model.implementation.OrderNote;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for an order ({@code Orders} = {@code ordersBase} + {@code OrderDetails}) from the
 * Orders v2 API. Wrapped by {@code OrderWrapper} for the public {@code Order} view.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderJson {

    @Getter static TypeReference<List<OrderJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("quoteRequestType")
    private QuoteRequestType quoteRequestType;

    @JsonProperty("contacts")
    private List<OrderContactInfo> contacts;

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("closedDateTime")
    private String closedDateTime;

    @JsonProperty("estimatedCompletionDateTime")
    private String estimatedCompletionDateTime;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("subChannel")
    private SubChannel subChannel;

    @JsonProperty("notes")
    private List<OrderNote> notes;

    @JsonProperty("additionalInfo")
    private List<AdditionalInfo> additionalInfo;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("cancellable")
    private Boolean cancellable;

    @JsonProperty("modifiable")
    private Boolean modifiable;

    @JsonProperty("details")
    private List<OrderLine> details;
}
