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

import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.json.creators.TicketCancelRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketNoteRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TicketUpdateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.TroubleTicketCreateRequest;

/**
 * Client interface for managing trouble tickets in the Equinix Customer Portal.
 *
 * <p>Backed by the Tickets v2 API at {@code /v2/tickets}. Tickets are created, retrieved by id,
 * updated (notification contacts), annotated with notes and cancelled. There is no collection
 * listing endpoint.</p>
 */
public interface TroubleTickets {

    /**
     * Creates a trouble ticket.
     *
     * <p>Maps to {@code POST /v2/tickets} ({@code Create a Ticket}). The created ticket id is
     * returned from the {@code Location} response header.</p>
     *
     * @param request the create request body
     * @return the created ticket id
     */
    String create(TroubleTicketCreateRequest request);

    /**
     * Retrieves a specific trouble ticket by its identifier.
     *
     * <p>Maps to {@code GET /v2/tickets/{id}} ({@code Retrieve a ticket}).</p>
     *
     * @param id the identifier of the ticket
     * @return the matching ticket
     */
    TroubleTicket getByUuid(String id);

    /**
     * Updates a trouble ticket's notification contacts.
     *
     * <p>Maps to {@code PATCH /v2/tickets/{id}} ({@code Update a ticket}).</p>
     *
     * @param id      the identifier of the ticket
     * @param request the update request body
     * @return {@code true} if the update was accepted
     */
    Boolean update(String id, TicketUpdateRequest request);

    /**
     * Adds a note to a trouble ticket.
     *
     * <p>Maps to {@code POST /v2/tickets/{id}/notes} ({@code Add notes to ticket}).</p>
     *
     * @param id      the identifier of the ticket
     * @param request the note request body
     * @return {@code true} if the note was accepted
     */
    Boolean addNote(String id, TicketNoteRequest request);

    /**
     * Cancels a trouble ticket.
     *
     * <p>Maps to {@code POST /v2/tickets/{id}/cancel} ({@code Cancel a ticket}).</p>
     *
     * @param id      the identifier of the ticket
     * @param request the cancel request body
     * @return {@code true} if the cancellation was accepted
     */
    Boolean cancel(String id, TicketCancelRequest request);
}
