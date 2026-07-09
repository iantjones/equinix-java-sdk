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
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.SerializationHelper;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.PendingStorageOrderResponse;
import api.equinix.javasdk.customerportal.model.ShipmentLocation;
import api.equinix.javasdk.customerportal.model.ShipmentOrderResponse;
import api.equinix.javasdk.customerportal.model.json.OrderResponseJson;
import api.equinix.javasdk.customerportal.model.json.PendingStorageOrderResponseJson;
import api.equinix.javasdk.customerportal.model.json.ShipmentLocationsResponseJson;
import api.equinix.javasdk.customerportal.model.json.ShipmentOrderResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OutboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PendingStorageOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentUpdateRequest;

import java.util.HashMap;
import java.util.List;
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

    public ShipmentOrderResponse orderInbound(InboundShipmentOrderRequest request) {
        return postAs("OrderInboundShipment", request, ShipmentOrderResponseJson.class);
    }

    public ShipmentOrderResponse orderOutbound(OutboundShipmentOrderRequest request) {
        return postAs("OrderOutboundShipment", request, ShipmentOrderResponseJson.class);
    }

    public List<? extends PendingStorageOrderResponse> orderPendingStorage(PendingStorageOrderRequest request) {
        return postListAs("OrderPendingStorageShipment", request, PendingStorageOrderResponseJson.class);
    }

    public List<? extends ShipmentLocation> listLocations(Boolean detail, String ibxs, String cages) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (detail != null) {
            queryParams.put("detail", List.of(String.valueOf(detail)));
        }
        if (ibxs != null) {
            queryParams.put("ibxs", List.of(ibxs));
        }
        if (cages != null) {
            queryParams.put("cages", List.of(cages));
        }
        ShipmentLocationsResponseJson response = getAs("ListShipmentLocations", null,
                queryParams.isEmpty() ? null : queryParams, ShipmentLocationsResponseJson.class);
        // An empty 2xx body (204, "{}") deserializes to null / a null "locations" member.
        return response == null || response.getLocations() == null ? List.of() : response.getLocations();
    }

    private OrderResponse submitOrder(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, null, Object.class);
        SerializationHelper.serializeJson(request, body);
        String orderId = ResponseHandler.extractFromHeader(invoke(request), "Location", OrderLocation.ORDER_ID_PATTERN);
        return new OrderResponseJson(orderId);
    }
}
