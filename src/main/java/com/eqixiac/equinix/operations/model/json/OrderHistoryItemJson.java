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

package com.eqixiac.equinix.operations.model.json;

import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.eqixiac.equinix.operations.model.OrderHistoryItem;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryAccount;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryContact;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryLink;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for an order history record ({@code order-header}). Read-only list element of an order
 * history search response, so it implements {@link OrderHistoryItem} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderHistoryItemJson implements OrderHistoryItem {

    @Getter static TypeReference<List<OrderHistoryItemJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("type")
    private List<String> type;

    @JsonProperty("orderStatus")
    private OrderHistoryStatus orderStatus;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("submittedDate")
    private String submittedDate;

    @JsonProperty("account")
    private OrderHistoryAccount account;

    // The spec documents singular orderingContact / notificationContact objects; the live service
    // returns orderingContacts (an object) and notificationContacts / technicalContacts (arrays).
    @JsonProperty("orderingContact")
    @JsonAlias("orderingContacts")
    private OrderHistoryContact orderingContact;

    @JsonProperty("notificationContact")
    private OrderHistoryContact notificationContact;

    @JsonProperty("notificationContacts")
    private List<OrderHistoryContact> notificationContacts;

    @JsonProperty("technicalContacts")
    private List<OrderHistoryContact> technicalContacts;

    @JsonProperty("orderSource")
    private String orderSource;

    @JsonProperty("ibx")
    private List<String> ibx;

    @JsonProperty("customerReferenceNumbers")
    private List<String> customerReferenceNumbers;

    @JsonProperty("links")
    private List<OrderHistoryLink> links;
}
