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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.customerportal.client.Shipments;
import com.eqixiac.equinix.customerportal.client.internal.ShipmentClient;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.PendingStorageOrderResponse;
import com.eqixiac.equinix.customerportal.model.ShipmentLocation;
import com.eqixiac.equinix.customerportal.model.ShipmentOrderResponse;
import com.eqixiac.equinix.customerportal.model.json.creators.InboundShipmentOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.OutboundShipmentOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.PendingStorageOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.ShipmentOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.ShipmentUpdateRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShipmentsImpl implements Shipments {

    private final ShipmentClient serviceClient;

    private final CustomerPortal serviceManager;

    public OrderResponse order(ShipmentOrderRequest request) {
        return this.serviceClient.order(request);
    }

    public OrderResponse update(String orderId, ShipmentUpdateRequest request) {
        return this.serviceClient.update(orderId, request);
    }

    public ShipmentOrderResponse orderInbound(InboundShipmentOrderRequest request) {
        return this.serviceClient.orderInbound(request);
    }

    public ShipmentOrderResponse orderOutbound(OutboundShipmentOrderRequest request) {
        return this.serviceClient.orderOutbound(request);
    }

    public List<? extends PendingStorageOrderResponse> orderPendingStorage(PendingStorageOrderRequest request) {
        return this.serviceClient.orderPendingStorage(request);
    }

    public List<? extends ShipmentLocation> listLocations() {
        return this.serviceClient.listLocations(null, null, null);
    }

    public List<? extends ShipmentLocation> listLocations(Boolean detail, String ibxs, String cages) {
        return this.serviceClient.listLocations(detail, ibxs, cages);
    }
}
