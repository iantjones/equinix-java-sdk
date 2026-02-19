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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketOperator;

/**
 * Client interface for managing trouble tickets in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and create trouble tickets for reporting
 * and tracking infrastructure issues.
 */
public interface TroubleTickets {

    /**
     * Lists all trouble tickets for the current account.
     *
     * @return a paginated list of trouble tickets
     */
    PaginatedList<TroubleTicket> list();

    /**
     * Retrieves a specific trouble ticket by its unique identifier.
     *
     * @param uuid the unique identifier of the trouble ticket
     * @return the matching trouble ticket
     */
    TroubleTicket getByUuid(String uuid);

    /**
     * Returns a builder for defining a new trouble ticket.
     *
     * @return a new TroubleTicketBuilder instance
     */
    TroubleTicketOperator.TroubleTicketBuilder define();
}
