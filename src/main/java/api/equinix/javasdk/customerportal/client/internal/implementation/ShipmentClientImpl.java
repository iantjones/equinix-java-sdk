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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.OrderResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentUpdateRequest;

import java.util.Map;

public class ShipmentClientImpl extends ClientBase implements ShipmentClient {

    public ShipmentClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Shipments");
    }

    public OrderResponse order(ShipmentOrderRequest request) {
        return submitOrder("OrderShipment", null, request);
    }

    public OrderResponse update(String orderId, ShipmentUpdateRequest request) {
        return submitOrder("UpdateShipment", Map.of("orderId", orderId), request);
    }

    private OrderResponse submitOrder(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, null, Object.class);
        Utils.serializeJson(request, body);
        String orderId = Utils.extractFromHeader(invoke(request), "Location", OrderLocation.ORDER_ID_PATTERN);
        return new OrderResponseJson(orderId);
    }
}
