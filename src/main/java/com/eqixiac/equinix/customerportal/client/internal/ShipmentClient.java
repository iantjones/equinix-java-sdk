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

package com.eqixiac.equinix.customerportal.client.internal;

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

public interface ShipmentClient {

    OrderResponse order(ShipmentOrderRequest request);

    OrderResponse update(String orderId, ShipmentUpdateRequest request);

    ShipmentOrderResponse orderInbound(InboundShipmentOrderRequest request);

    ShipmentOrderResponse orderOutbound(OutboundShipmentOrderRequest request);

    List<? extends PendingStorageOrderResponse> orderPendingStorage(PendingStorageOrderRequest request);

    List<? extends ShipmentLocation> listLocations(Boolean detail, String ibxs, String cages);
}
