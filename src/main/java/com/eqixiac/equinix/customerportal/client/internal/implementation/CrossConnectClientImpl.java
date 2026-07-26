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
import com.eqixiac.equinix.customerportal.client.internal.CrossConnectClient;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.json.OrderResponseJson;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectDeinstallRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectUpdateRequest;

import java.util.Map;

public class CrossConnectClientImpl extends ClientBase implements CrossConnectClient {

    public CrossConnectClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "CrossConnects");
    }

    public OrderResponse order(CrossConnectOrderRequest request) {
        return submitOrder("OrderCrossConnect", null, request);
    }

    public OrderResponse update(String orderId, CrossConnectUpdateRequest request) {
        return submitOrder("UpdateCrossConnect", Map.of("orderId", orderId), request);
    }

    public OrderResponse deinstall(CrossConnectDeinstallRequest request) {
        return submitOrder("DeinstallCrossConnect", null, request);
    }

    /**
     * Serializes and dispatches an order request, returning the order id parsed from the
     * {@code Location} response header.
     */
    private OrderResponse submitOrder(String serviceEndpoint, Map<String, String> pathParams, Object body) {
        EquinixRequest<Object> request = buildRequest(serviceEndpoint, RequestType.SINGLE, pathParams, null, Object.class);
        SerializationHelper.serializeJson(request, body);
        String orderId = ResponseHandler.extractFromHeader(invoke(request), "Location", OrderLocation.ORDER_ID_PATTERN);
        return new OrderResponseJson(orderId);
    }
}
