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

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.WorkVisitClient;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.WorkVisitLocation;
import com.eqixiac.equinix.customerportal.model.json.OrderResponseJson;
import com.eqixiac.equinix.customerportal.model.json.WorkVisitLocationsResponseJson;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitUpdateRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkVisitClientImpl extends ClientBase implements WorkVisitClient {

    public WorkVisitClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "WorkVisits");
    }

    public OrderResponse order(WorkVisitOrderRequest request) {
        return submitOrder("OrderWorkVisit", null, request);
    }

    public OrderResponse update(String orderId, WorkVisitUpdateRequest request) {
        return submitOrder("UpdateWorkVisit", Map.of("orderId", orderId), request);
    }

    public List<? extends WorkVisitLocation> listLocations(Boolean detail, String ibxs, String cages) {
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
        WorkVisitLocationsResponseJson response = getAs("ListWorkVisitLocations", null,
                queryParams.isEmpty() ? null : queryParams, WorkVisitLocationsResponseJson.class);
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
