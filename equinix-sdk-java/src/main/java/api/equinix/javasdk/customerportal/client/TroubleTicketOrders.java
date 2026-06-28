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
import api.equinix.javasdk.customerportal.model.TroubleTicketOrderLocation;
import api.equinix.javasdk.customerportal.model.TroubleTicketType;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketOrderRequest;

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
     * Lists the IBX locations and cages where the current user may place trouble ticket orders.
     *
     * @return the list of permitted locations
     */
    List<? extends TroubleTicketOrderLocation> getLocations();

    /**
     * Submits a trouble ticket for an issue impacting your service.
     *
     * @param request the trouble ticket order body
     * @return the created order response carrying the generated order number
     */
    OrderResponse placeOrder(TroubleTicketOrderRequest request);
}
