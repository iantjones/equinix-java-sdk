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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.customerportal.client.Orders;
import api.equinix.javasdk.customerportal.client.internal.OrderClient;
import api.equinix.javasdk.customerportal.enums.NegotiationAction;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.OrderNegotiation;
import api.equinix.javasdk.customerportal.model.json.OrderJson;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentReference;
import api.equinix.javasdk.customerportal.model.json.creators.CancelRequestJson;
import api.equinix.javasdk.customerportal.model.json.creators.NegotiationsRequestJson;
import api.equinix.javasdk.customerportal.model.json.creators.NoteRequestJson;
import api.equinix.javasdk.customerportal.model.wrappers.OrderWrapper;

import java.util.List;

public class OrdersImpl implements Orders {

    private final CustomerPortal serviceManager;

    private final OrderClient<Order> serviceClient;

    public OrdersImpl(OrderClient<Order> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public Order getByUuid(String orderId) {
        OrderJson orderJson = this.serviceClient.getByUuid(orderId);
        return new OrderWrapper(orderJson, this.serviceClient);
    }

    public Order getByUuid(String orderId, List<String> ibxs) {
        OrderJson orderJson = this.serviceClient.getByUuid(orderId, ibxs);
        return new OrderWrapper(orderJson, this.serviceClient);
    }

    public List<? extends OrderNegotiation> getNegotiations(String orderId) {
        return this.serviceClient.getNegotiations(orderId);
    }

    public Boolean replyNegotiation(String orderId, NegotiationAction action, String referenceId) {
        return this.replyNegotiation(orderId, action, referenceId, null);
    }

    public Boolean replyNegotiation(String orderId, NegotiationAction action, String referenceId, String reason) {
        return this.serviceClient.replyNegotiation(orderId, new NegotiationsRequestJson(action, referenceId, reason));
    }

    public Boolean addNote(String orderId, String text) {
        return this.addNote(orderId, text, null, null);
    }

    public Boolean addNote(String orderId, String text, String referenceId, List<AttachmentReference> attachments) {
        return this.serviceClient.addNote(orderId, new NoteRequestJson(text, referenceId, attachments));
    }

    public Boolean cancel(String orderId, String reason) {
        return this.cancel(orderId, reason, null, null);
    }

    public Boolean cancel(String orderId, String reason, List<AttachmentReference> attachments, List<String> lineIds) {
        return this.serviceClient.cancel(orderId, new CancelRequestJson(reason, attachments, lineIds));
    }
}
