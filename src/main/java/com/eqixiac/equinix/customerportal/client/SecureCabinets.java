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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.customerportal.model.OrderResponse;
import com.eqixiac.equinix.customerportal.model.ProductAvailability;
import com.eqixiac.equinix.customerportal.model.json.creators.SecureCabinetOrderRequest;

import java.util.List;

/**
 * Client interface for ordering secure cabinets in the Equinix Customer Portal.
 *
 * <p>Backed by the Secure Cabinet v1 API at {@code /securecabinet/v1}. Secure cabinets are not
 * listed or retrieved through this API; they are ordered via {@link #createOrder} (the resulting
 * order is then tracked through {@link Orders} and {@link OrderHistory}), and the orderable
 * configuration for an account is discovered through {@link #getProductsAvailability(String)}.</p>
 */
public interface SecureCabinets {

    /**
     * Submits a secure cabinet order.
     *
     * <p>Maps to {@code POST /securecabinet/v1/orders} ({@code createOrder}).</p>
     *
     * @param request the secure cabinet order request body
     * @return the order submission result carrying the generated order id
     */
    OrderResponse createOrder(SecureCabinetOrderRequest request);

    /**
     * Lists the secure cabinet product availability (cabinet capacity and power configuration)
     * for the given account.
     *
     * <p>Maps to {@code GET /securecabinet/v1/availability/{accountNumber}}
     * ({@code getProductsAvailability}).</p>
     *
     * @param accountNumber the account number
     * @return the list of product availabilities per IBX
     */
    List<? extends ProductAvailability> getProductsAvailability(String accountNumber);
}
