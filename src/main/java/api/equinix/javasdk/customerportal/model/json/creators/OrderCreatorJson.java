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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class OrderCreatorJson {

    @JsonProperty("type") private OrderType type;
    @JsonProperty("description") private String description;
    @JsonProperty("customerReferenceId") private String customerReferenceId;
    @JsonProperty("accountNumber") private String accountNumber;
    @JsonProperty("ibxCode") private String ibxCode;

    public OrderCreatorJson(OrderOperator.OrderBuilder builder) {
        this.type = builder.getType();
        this.description = builder.getDescription();
        this.customerReferenceId = builder.getCustomerReferenceId();
        this.accountNumber = builder.getAccountNumber();
        this.ibxCode = builder.getIbxCode();
    }
}
