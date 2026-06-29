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
import api.equinix.javasdk.customerportal.client.SecureCabinets;
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.ProductAvailability;
import api.equinix.javasdk.customerportal.model.json.creators.SecureCabinetOrderRequest;

import java.util.List;

public class SecureCabinetsImpl implements SecureCabinets {

    private final CustomerPortal serviceManager;

    private final SecureCabinetClient serviceClient;

    public SecureCabinetsImpl(SecureCabinetClient serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public OrderResponse createOrder(SecureCabinetOrderRequest request) {
        return this.serviceClient.createOrder(request);
    }

    public List<? extends ProductAvailability> getProductsAvailability(String accountNumber) {
        return this.serviceClient.getProductsAvailability(accountNumber);
    }
}
