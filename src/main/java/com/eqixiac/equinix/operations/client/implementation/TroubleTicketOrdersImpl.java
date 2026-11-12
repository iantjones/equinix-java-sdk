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

package com.eqixiac.equinix.operations.client.implementation;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.operations.client.TroubleTicketOrders;
import com.eqixiac.equinix.operations.client.internal.TroubleTicketOrderClient;
import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.TroubleTicketOrderLocation;
import com.eqixiac.equinix.operations.model.TroubleTicketType;
import com.eqixiac.equinix.operations.model.json.creators.TroubleTicketOrderRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TroubleTicketOrdersImpl implements TroubleTicketOrders {

    private final TroubleTicketOrderClient serviceClient;

    private final Operations serviceManager;

    public List<? extends TroubleTicketType> getTypes() {
        return this.serviceClient.getTypes(null);
    }

    public List<? extends TroubleTicketType> getTypes(String category) {
        return this.serviceClient.getTypes(category);
    }

    public List<? extends TroubleTicketOrderLocation> getLocations() {
        return this.serviceClient.getLocations(null, null, null);
    }

    public List<? extends TroubleTicketOrderLocation> getLocations(Boolean detail, String ibxs, String cages) {
        return this.serviceClient.getLocations(detail, ibxs, cages);
    }

    public OrderResponse placeOrder(TroubleTicketOrderRequest request) {
        return this.serviceClient.placeOrder(request);
    }
}
