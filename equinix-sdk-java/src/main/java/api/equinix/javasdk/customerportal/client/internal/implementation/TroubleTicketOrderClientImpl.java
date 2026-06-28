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
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketOrderClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.TroubleTicketOrderLocation;
import api.equinix.javasdk.customerportal.model.TroubleTicketType;
import api.equinix.javasdk.customerportal.model.json.OrderResponseJson;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketLocationsResponseJson;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketOrderResultJson;
import api.equinix.javasdk.customerportal.model.json.TroubleTicketTypesResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketOrderRequest;

import java.util.List;

public class TroubleTicketOrderClientImpl extends ClientBase implements TroubleTicketOrderClient {

    public TroubleTicketOrderClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "TroubleTicketOrders");
    }

    public List<? extends TroubleTicketType> getTypes() {
        TroubleTicketTypesResponseJson response = getAs("GetTroubleTicketTypes", TroubleTicketTypesResponseJson.class);
        return response.getTroubleTicketTypes();
    }

    public List<? extends TroubleTicketOrderLocation> getLocations() {
        TroubleTicketLocationsResponseJson response = getAs("GetTroubleTicketLocations", TroubleTicketLocationsResponseJson.class);
        return response.getLocations();
    }

    public OrderResponse placeOrder(TroubleTicketOrderRequest request) {
        TroubleTicketOrderResultJson result = postAs("PlaceTroubleTicketOrder", request, TroubleTicketOrderResultJson.class);
        return new OrderResponseJson(result.getOrderNumber());
    }
}
