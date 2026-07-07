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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.PendingStorageOrderResponse;
import api.equinix.javasdk.customerportal.model.ShipmentLocation;
import api.equinix.javasdk.customerportal.model.ShipmentOrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.InboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.OutboundShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PendingStorageOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.ShipmentUpdateRequest;

import java.util.List;

/**
 * Client interface for scheduling equipment shipments in the Equinix Customer Portal.
 *
 * <p>Combines the shipment order APIs at {@code /colocations/v2/orders/shipments} and
 * {@code /v1/orders/shipment}. Shipments are scheduled and updated as orders; the resulting order
 * is tracked through {@link Orders} and {@link OrderHistory}, and a shipment is cancelled via
 * {@link Orders#cancel(String, String)}.</p>
 *
 * <p>{@link #order(ShipmentOrderRequest)} and {@link #update(String, ShipmentUpdateRequest)}
 * return the generated order id (parsed from the {@code Location} header). The typed
 * inbound/outbound/pending-storage submissions ({@link #orderInbound(InboundShipmentOrderRequest)},
 * {@link #orderOutbound(OutboundShipmentOrderRequest)},
 * {@link #orderPendingStorage(PendingStorageOrderRequest)}) return the order number(s) in the
 * response body. The IBX locations where the current user may place shipment orders are available
 * via {@code listLocations()}.</p>
 */
public interface Shipments {

    /**
     * Schedules an inbound or outbound shipment.
     *
     * <p>Maps to {@code POST /colocations/v2/orders/shipments}
     * ({@code Schedule inbound or outbound shipment}).</p>
     *
     * @param request the shipment order request body
     * @return the order submission result carrying the generated order id
     */
    OrderResponse order(ShipmentOrderRequest request);

    /**
     * Updates a pending shipment order.
     *
     * <p>Maps to {@code PATCH /colocations/v2/orders/shipments/{orderId}}
     * ({@code Update inbound or outbound shipment}).</p>
     *
     * @param orderId the identifier of the shipment order
     * @param request the update request body
     * @return the order submission result carrying the order id
     */
    OrderResponse update(String orderId, ShipmentUpdateRequest request);

    /**
     * Submits an inbound shipment order.
     *
     * <p>Maps to {@code POST /v1/orders/shipment/inbound} ({@code shipment-inbound}).</p>
     *
     * @param request the inbound shipment order request body
     * @return the order submission result carrying the generated order number
     */
    ShipmentOrderResponse orderInbound(InboundShipmentOrderRequest request);

    /**
     * Submits an outbound shipment order.
     *
     * <p>Maps to {@code POST /v1/orders/shipment/outbound} ({@code outbound_shipment_submit}).</p>
     *
     * @param request the outbound shipment order request body
     * @return the order submission result carrying the generated order number
     */
    ShipmentOrderResponse orderOutbound(OutboundShipmentOrderRequest request);

    /**
     * Submits a pending inbound shipment (pending storage) order.
     *
     * <p>Maps to {@code POST /v1/orders/shipment/pendingStorage} ({@code shipment-pending}).</p>
     *
     * @param request the pending storage order request body
     * @return one submission result per stored shipment submitted
     */
    List<? extends PendingStorageOrderResponse> orderPendingStorage(PendingStorageOrderRequest request);

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place shipment
     * orders.
     *
     * @return the list of permitted locations
     */
    List<? extends ShipmentLocation> listLocations();

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place shipment
     * orders, optionally filtered.
     *
     * <p>Maps to {@code GET /v1/orders/shipment/locations} ({@code getLocation}).</p>
     *
     * @param detail when {@code true}, returns detailed permission with cages and cabinets, or {@code null} for the default
     * @param ibxs   a comma-separated list of IBX codes to filter by (e.g. {@code AM1,AM2}), or {@code null}
     * @param cages  a comma-separated list of cage ids to filter by (e.g. {@code AM1:02:002MC1}), or {@code null}
     * @return the list of permitted locations
     */
    List<? extends ShipmentLocation> listLocations(Boolean detail, String ibxs, String cages);
}
