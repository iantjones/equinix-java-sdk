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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.customerportal.client.CrossConnects;
import api.equinix.javasdk.customerportal.client.internal.CrossConnectClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectDeinstallRequest;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.CrossConnectUpdateRequest;

public class CrossConnectsImpl implements CrossConnects {

    private final CustomerPortal serviceManager;

    private final CrossConnectClient serviceClient;

    public CrossConnectsImpl(CrossConnectClient serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public OrderResponse order(CrossConnectOrderRequest request) {
        return this.serviceClient.order(request);
    }

    public OrderResponse update(String orderId, CrossConnectUpdateRequest request) {
        return this.serviceClient.update(orderId, request);
    }

    public OrderResponse deinstall(CrossConnectDeinstallRequest request) {
        return this.serviceClient.deinstall(request);
    }
}
