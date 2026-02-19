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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderHistoryItemJson implements Serializable, OrderHistoryItem {

    @Getter static TypeReference<Page<OrderHistoryItem, OrderHistoryItemJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<OrderHistoryItemJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<OrderHistoryItemJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("orderType")
    private String orderType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("completedDate")
    private String completedDate;

    @JsonProperty("description")
    private String description;
}
