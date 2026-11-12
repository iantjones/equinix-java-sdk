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

package com.eqixiac.equinix.operations.client;

import com.eqixiac.equinix.operations.model.OrderResponse;
import com.eqixiac.equinix.operations.model.LookupLocation;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitOrderRequest;
import com.eqixiac.equinix.operations.model.json.creators.WorkVisitUpdateRequest;

import java.util.List;

/**
 * Client interface for scheduling work visits.
 *
 * <p>Backed by the Work Visit v2 order API at {@code /colocations/v2/orders/workVisits}. Work visits
 * are scheduled and updated as orders; the resulting order is tracked through {@link Orders} and
 * {@link OrderHistory}, and a work visit is cancelled via {@link Orders#cancel(String, String)}.
 * Each order operation returns the generated order id (parsed from the {@code Location} header).</p>
 *
 * <p>Work Visit v1 ({@code /v1/orders/workvisit/...}) reached end of life on 2024-01-22 and is not
 * exposed. The locations where the current user may place work visit orders come from Lookup v2
 * ({@code GET /colocations/v2/locations?permissionCode=WORK_VISIT}): see {@link #listLocations()}
 * and {@link Lookups#listLocations(String, List, String, String, String, Boolean)}.</p>
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
     * <p>Maps to Lookup v2 {@code GET /colocations/v2/locations?permissionCode=WORK_VISIT}; the same
     * call as {@code lookups().listLocations("WORK_VISIT")}. It replaces the end-of-life Work Visit v1
     * {@code GET /v1/orders/workvisit/locations}.</p>
     *
     * @return the list of permitted locations
     */
    List<? extends LookupLocation> listLocations();

    /**
     * Lists the work visit locations for the given IBXs, optionally with cage, cabinet and account
     * details.
     *
     * <p>Maps to Lookup v2 {@code GET /colocations/v2/locations?permissionCode=WORK_VISIT}.</p>
     *
     * @param ibxs    the IBX codes to filter by, or {@code null}
     * @param details when {@code true}, returns cage, cabinet and account details, or {@code null} for the default
     * @return the list of permitted locations
     */
    List<? extends LookupLocation> listLocations(List<String> ibxs, Boolean details);
}
