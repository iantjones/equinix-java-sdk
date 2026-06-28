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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.OrderClient;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.OrderNegotiation;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import api.equinix.javasdk.customerportal.model.json.OrderNegotiationJson;
import api.equinix.javasdk.customerportal.model.json.creators.CancelRequestJson;
import api.equinix.javasdk.customerportal.model.json.creators.NegotiationsRequestJson;
import api.equinix.javasdk.customerportal.model.json.creators.NoteRequestJson;
import api.equinix.javasdk.customerportal.model.wrappers.OrderWrapper;

import java.util.List;
import java.util.Map;

public class OrderClientImpl extends ResourceClientBase<Order, OrderJson> implements OrderClient<Order> {

    public OrderClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Orders", OrderJson.class);
    }

    @Override
    protected Order wrap(OrderJson json) {
        return new OrderWrapper(json, this);
    }

    public OrderJson getByUuid(String orderId) {
        return getOne("GetOrder", Map.of("orderId", orderId));
    }

    public OrderJson refresh(String orderId) {
        return this.getByUuid(orderId);
    }

    public List<? extends OrderNegotiation> getNegotiations(String orderId) {
        return listAs("GetOrderNegotiations", Map.of("orderId", orderId), null, OrderNegotiationJson.class);
    }

    public Boolean replyNegotiation(String orderId, NegotiationsRequestJson negotiationsRequestJson) {
        return booleanOp("ReplyOrderNegotiation", RequestType.SINGLE, Map.of("orderId", orderId), null, negotiationsRequestJson);
    }

    public Boolean addNote(String orderId, NoteRequestJson noteRequestJson) {
        return booleanOp("AddOrderNote", RequestType.SINGLE, Map.of("orderId", orderId), null, noteRequestJson);
    }

    public Boolean cancel(String orderId, CancelRequestJson cancelRequestJson) {
        return booleanOp("CancelOrder", RequestType.SINGLE, Map.of("orderId", orderId), null, cancelRequestJson);
    }
}
