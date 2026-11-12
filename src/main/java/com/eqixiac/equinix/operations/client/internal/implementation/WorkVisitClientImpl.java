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

package com.eqixiac.equinix.operations.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.operations.client.implementation.OperationsConfigImpl;
import com.eqixiac.equinix.operations.client.internal.WorkVisitClient;
import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.json.OrderResponseJson;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitUpdateRequest;

import java.util.Map;

public class WorkVisitClientImpl extends ClientBase implements WorkVisitClient {

    public WorkVisitClientImpl(OperationsConfigImpl configClient) {
        super(configClient, "Operations", "WorkVisits");
    }

    public OrderResponse order(WorkVisitOrderRequest request) {
        return submitOrder("OrderWorkVisit", null, request);
    }

    public OrderResponse update(String orderId, WorkVisitUpdateRequest request) {
        return submitOrder("UpdateWorkVisit", Map.of("orderId", orderId), request);
    }

    private OrderResponse submitOrder(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, null, Object.class);
        SerializationHelper.serializeJson(request, body);
        String orderId = ResponseHandler.extractFromHeader(invoke(request), "Location", OrderLocation.ORDER_ID_PATTERN);
        return new OrderResponseJson(orderId);
    }
}
