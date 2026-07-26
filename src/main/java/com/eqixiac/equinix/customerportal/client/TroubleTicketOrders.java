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
import com.eqixiac.equinix.customerportal.model.TroubleTicketOrderLocation;
import com.eqixiac.equinix.customerportal.model.TroubleTicketType;
import com.eqixiac.equinix.customerportal.model.json.creators.TroubleTicketOrderRequest;

import java.util.List;

/**
 * Client interface for submitting trouble tickets in the Equinix Customer Portal.
 *
 * <p>Backed by the Trouble Ticket v1 API at {@code /v1/orders/troubleticket}. Reference data is
 * available via {@link #getTypes()} (problem categories and their codes) and {@link #getLocations()}
 * (IBX/cage locations the current user may raise tickets for); {@link #placeOrder} submits a ticket
 * for an issue impacting service.</p>
 */
public interface TroubleTicketOrders {

    /**
     * Lists all Equinix trouble ticket problem categories and their corresponding codes.
     *
     * @return the list of supported trouble ticket types
     */
    List<? extends TroubleTicketType> getTypes();

    /**
     * Lists Equinix trouble ticket problem categories filtered by a single category.
     *
     * <p>Maps to {@code GET /v1/orders/troubleticket/types} with the {@code category} query
     * parameter (e.g. {@code Cross Connect}, {@code Network}, {@code Power}, {@code Environment},
     * {@code Hardware}, {@code Security}, {@code Managed Services}, {@code Smartview}).</p>
     *
     * @param category the trouble ticket category to filter by, or {@code null} for all
     * @return the list of supported trouble ticket types
     */
    List<? extends TroubleTicketType> getTypes(String category);

    /**
     * Lists the IBX locations and cages where the current user may place trouble ticket orders.
     *
     * @return the list of permitted locations
     */
    List<? extends TroubleTicketOrderLocation> getLocations();

    /**
     * Lists the IBX locations and cages where the current user may place trouble ticket orders,
     * filtered by the supplied parameters.
     *
     * <p>Maps to {@code GET /v1/orders/troubleticket/locations} with the {@code detail},
     * {@code ibxs} and {@code cages} query parameters.</p>
     *
     * @param detail whether to include detailed cage/account information, or {@code null}
     * @param ibxs   the IBX codes to filter by, or {@code null}
     * @param cages  the cage ids to filter by, or {@code null}
     * @return the list of permitted locations
     */
    List<? extends TroubleTicketOrderLocation> getLocations(Boolean detail, String ibxs, String cages);

    /**
     * Submits a trouble ticket for an issue impacting your service.
     *
     * @param request the trouble ticket order body
     * @return the created order response carrying the generated order number
     */
    OrderResponse placeOrder(TroubleTicketOrderRequest request);
}
