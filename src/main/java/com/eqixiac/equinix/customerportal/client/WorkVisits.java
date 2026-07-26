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
import com.eqixiac.equinix.customerportal.model.WorkVisitLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.WorkVisitUpdateRequest;

import java.util.List;

/**
 * Client interface for scheduling work visits in the Equinix Customer Portal.
 *
 * <p>Combines the work visit order APIs at {@code /colocations/v2/orders/workVisits} and
 * {@code /v1/orders/workvisit}. Work visits are scheduled and updated as orders; the resulting
 * order is tracked through {@link Orders} and {@link OrderHistory}, and a work visit is cancelled
 * via {@link Orders#cancel(String, String)}. Each order operation returns the generated order id
 * (parsed from the {@code Location} header). The IBX locations where the current user may place
 * work visit orders are available via {@code listLocations()}.</p>
 */
public interface WorkVisits {

    /**
     * Schedules a work visit.
     *
     * <p>Maps to {@code POST /colocations/v2/orders/workVisits} ({@code Schedule Work Visit Services}).</p>
     *
     * @param request the work visit order request body
     * @return the order submission result carrying the generated order id
     */
    OrderResponse order(WorkVisitOrderRequest request);

    /**
     * Updates a pending work visit order.
     *
     * <p>Maps to {@code PATCH /colocations/v2/orders/workVisits/{orderId}}
     * ({@code Update a work visit order}).</p>
     *
     * @param orderId the identifier of the work visit order
     * @param request the update request body
     * @return the order submission result carrying the order id
     */
    OrderResponse update(String orderId, WorkVisitUpdateRequest request);

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place work visit
     * orders.
     *
     * @return the list of permitted locations
     */
    List<? extends WorkVisitLocation> listLocations();

    /**
     * Lists the IBX locations, cages and cabinets where the current user may place work visit
     * orders, optionally filtered.
     *
     * <p>Maps to {@code GET /v1/orders/workvisit/locations} ({@code getLocation}).</p>
     *
     * @param detail when {@code true}, returns detailed permission with cages and cabinets, or {@code null} for the default
     * @param ibxs   a comma-separated list of IBX codes to filter by (e.g. {@code AM1,AM2}), or {@code null}
     * @param cages  a comma-separated list of cage ids to filter by (e.g. {@code AM1:02:002MC1}), or {@code null}
     * @return the list of permitted locations
     */
    List<? extends WorkVisitLocation> listLocations(Boolean detail, String ibxs, String cages);
}
