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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TroubleTicketOrderClientImpl extends ClientBase implements TroubleTicketOrderClient {

    public TroubleTicketOrderClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "TroubleTicketOrders");
    }

    public List<? extends TroubleTicketType> getTypes(String category) {
        Map<String, List<String>> queryParams = null;
        if (category != null) {
            queryParams = Map.of("category", List.of(category));
        }
        TroubleTicketTypesResponseJson response = getAs("GetTroubleTicketTypes", null, queryParams,
                TroubleTicketTypesResponseJson.class);
        return response.getTroubleTicketTypes();
    }

    public List<? extends TroubleTicketOrderLocation> getLocations(Boolean detail, String ibxs, String cages) {
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
        TroubleTicketLocationsResponseJson response = getAs("GetTroubleTicketLocations", null,
                queryParams.isEmpty() ? null : queryParams, TroubleTicketLocationsResponseJson.class);
        return response.getLocations();
    }

    public OrderResponse placeOrder(TroubleTicketOrderRequest request) {
        TroubleTicketOrderResultJson result = postAs("PlaceTroubleTicketOrder", request, TroubleTicketOrderResultJson.class);
        return new OrderResponseJson(result.getOrderNumber());
    }
}
