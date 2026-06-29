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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.OrderResponse;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitOrderRequest;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitUpdateRequest;

/**
 * Client interface for scheduling work visits in the Equinix Customer Portal.
 *
 * <p>Backed by the Work Visits v2 order API at {@code /colocations/v2/orders/workVisits}. Work
 * visits are scheduled and updated as orders; the resulting order is tracked through {@link Orders}
 * and {@link OrderHistory}, and a work visit is cancelled via
 * {@link Orders#cancel(String, String)}. Each operation returns the generated order id (parsed
 * from the {@code Location} header).</p>
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
}
