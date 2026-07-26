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
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectDeinstallRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.CrossConnectUpdateRequest;

/**
 * Client interface for ordering cross-connects in the Equinix Customer Portal.
 *
 * <p>Backed by the Cross Connect v2 order API at {@code /colocations/v2/orders/crossConnects}.
 * Cross-connects are ordered, updated and deinstalled as orders; the resulting order is tracked
 * through {@link Orders} and {@link OrderHistory}. Each operation returns the generated order id
 * (parsed from the {@code Location} response header).</p>
 */
public interface CrossConnects {

    /**
     * Places a cross-connect installation order.
     *
     * <p>Maps to {@code POST /colocations/v2/orders/crossConnects} ({@code Order cross connects}).</p>
     *
     * @param request the cross-connect order request body
     * @return the order submission result carrying the generated order id
     */
    OrderResponse order(CrossConnectOrderRequest request);

    /**
     * Updates a pending cross-connect order (notification contacts).
     *
     * <p>Maps to {@code PATCH /colocations/v2/orders/crossConnects/{orderId}}
     * ({@code Update a Cross Connect Order}).</p>
     *
     * @param orderId the identifier of the cross-connect order
     * @param request the update request body
     * @return the order submission result carrying the order id
     */
    OrderResponse update(String orderId, CrossConnectUpdateRequest request);

    /**
     * Places a cross-connect deinstallation order.
     *
     * <p>Maps to {@code POST /colocations/v2/orders/crossConnects/deinstall}
     * ({@code Place cross connect deinstallation order}).</p>
     *
     * @param request the deinstallation request body
     * @return the order submission result carrying the generated order id
     */
    OrderResponse deinstall(CrossConnectDeinstallRequest request);
}
