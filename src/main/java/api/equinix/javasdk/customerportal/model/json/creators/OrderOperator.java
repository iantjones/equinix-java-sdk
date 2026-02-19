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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.OrderClientImpl;
import api.equinix.javasdk.customerportal.enums.OrderType;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import api.equinix.javasdk.customerportal.model.wrappers.OrderWrapper;
import lombok.Getter;

public class OrderOperator extends ResourceImpl<Order> {

    @Getter
    private final Pageable<Order> serviceClient;

    public OrderOperator(Pageable<Order> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public OrderBuilder create() {
        return new OrderBuilder();
    }

    @Getter
    public class OrderBuilder {
        private OrderType type;
        private String description;
        private String customerReferenceId;
        private String accountNumber;
        private String ibxCode;

        public OrderBuilder type(OrderType type) {
            this.type = type;
            return this;
        }

        public OrderBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OrderBuilder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public OrderBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public OrderBuilder ibxCode(String ibxCode) {
            this.ibxCode = ibxCode;
            return this;
        }

        public Order create() {
            OrderCreatorJson creatorJson = new OrderCreatorJson(this);
            OrderJson orderJson = ((OrderClientImpl) OrderOperator.this.getServiceClient()).create(creatorJson);
            return new OrderWrapper(orderJson, OrderOperator.this.getServiceClient());
        }
    }
}
