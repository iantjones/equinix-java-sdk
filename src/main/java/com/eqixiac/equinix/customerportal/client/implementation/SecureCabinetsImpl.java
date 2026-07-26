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
import com.eqixiac.equinix.customerportal.client.SecureCabinets;
import com.eqixiac.equinix.customerportal.client.internal.SecureCabinetClient;
import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.ProductAvailability;
import com.eqixiac.equinix.customerportal.model.json.creators.SecureCabinetOrderRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecureCabinetsImpl implements SecureCabinets {

    private final SecureCabinetClient serviceClient;

    private final CustomerPortal serviceManager;

    public OrderResponse createOrder(SecureCabinetOrderRequest request) {
        return this.serviceClient.createOrder(request);
    }

    public List<? extends ProductAvailability> getProductsAvailability(String accountNumber) {
        return this.serviceClient.getProductsAvailability(accountNumber);
    }
}
