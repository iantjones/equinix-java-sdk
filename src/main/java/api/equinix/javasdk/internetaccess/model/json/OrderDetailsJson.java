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

package api.equinix.javasdk.internetaccess.model.json;

import api.equinix.javasdk.internetaccess.enums.OrderState;
import api.equinix.javasdk.internetaccess.enums.ServiceOrderType;
import api.equinix.javasdk.internetaccess.model.OrderDetails;
import api.equinix.javasdk.internetaccess.model.implementation.OrderChangeLog;
import api.equinix.javasdk.internetaccess.model.implementation.OrderContact;
import api.equinix.javasdk.internetaccess.model.implementation.OrderLink;
import api.equinix.javasdk.internetaccess.model.implementation.OrderPurchaseOrder;
import api.equinix.javasdk.internetaccess.model.implementation.OrderSignature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Read-only JSON model for an {@link OrderDetails} returned by the Equinix Internet Access (EIA) v1
 * order single get ({@code GET /internetAccess/v1/orders/{orderUUID}}). Implements
 * {@link OrderDetails} directly, so no wrapper is required.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDetailsJson implements OrderDetails {

    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("number")
    private String number;

    @JsonProperty("type")
    private ServiceOrderType type;

    @JsonProperty("contacts")
    private List<OrderContact> contacts;

    @JsonProperty("draft")
    private Boolean draft;

    @JsonProperty("links")
    private List<OrderLink> links;

    @JsonProperty("purchaseOrder")
    private OrderPurchaseOrder purchaseOrder;

    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("signature")
    private OrderSignature signature;

    @JsonProperty("state")
    private OrderState status;

    @JsonProperty("changeLog")
    private OrderChangeLog changeLog;

    @JsonProperty("tags")
    private List<String> tags;
}
