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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.OrderClient;
import com.eqixiac.equinix.customerportal.model.Order;
import com.eqixiac.equinix.customerportal.model.OrderNegotiation;
import com.eqixiac.equinix.customerportal.model.json.OrderJson;
import com.eqixiac.equinix.customerportal.model.json.OrderNegotiationJson;
import com.eqixiac.equinix.customerportal.model.json.creators.CancelRequestJson;
import com.eqixiac.equinix.customerportal.model.json.creators.NegotiationsRequestJson;
import com.eqixiac.equinix.customerportal.model.json.creators.NoteRequestJson;
import com.eqixiac.equinix.customerportal.model.wrappers.OrderWrapper;

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

    public OrderJson getByUuid(String orderId, List<String> ibxs) {
        if (ibxs == null || ibxs.isEmpty()) {
            return getByUuid(orderId);
        }
        return getAs("GetOrder", Map.of("orderId", orderId), Map.of("ibxs", ibxs), OrderJson.class);
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
