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

import com.eqixiac.equinix.customerportal.enums.NegotiationAction;
import com.eqixiac.equinix.customerportal.model.Order;
import com.eqixiac.equinix.customerportal.model.OrderNegotiation;
import com.eqixiac.equinix.customerportal.model.json.creators.AttachmentReference;

import java.util.List;

/**
 * Client interface for managing colocation orders in the Equinix Customer Portal.
 *
 * <p>Backed by the Orders v2 API at {@code /colocations/v2/orders/{orderId}}. Orders cannot be
 * created or listed through this API (use {@link OrderHistory} for historical listings); they are
 * retrieved by id and acted upon through negotiations, notes, and cancellation.</p>
 */
public interface Orders {

    /**
     * Retrieves a specific order by its unique identifier.
     *
     * @param orderId the identifier of the order
     * @return the matching order
     */
    Order getByUuid(String orderId);

    /**
     * Retrieves a specific order by its unique identifier, scoped to the supplied IBXs.
     *
     * <p>Maps to {@code GET /colocations/v2/orders/{orderId}} with the {@code ibxs} query
     * parameter.</p>
     *
     * @param orderId the identifier of the order
     * @param ibxs    the IBX codes to scope the order to, or {@code null}/empty for all
     * @return the matching order
     */
    Order getByUuid(String orderId, List<String> ibxs);

    /**
     * Retrieves the negotiation messages for an order, each proposing an alternative
     * date and time for fulfilling the order request.
     *
     * @param orderId the identifier of the order
     * @return the list of negotiation messages
     */
    List<? extends OrderNegotiation> getNegotiations(String orderId);

    /**
     * Replies to an order negotiation, approving or cancelling the proposed schedule.
     *
     * @param orderId     the identifier of the order
     * @param action      the action to perform (APPROVE, APPROVE_NON_EXPEDITE, or CANCEL)
     * @param referenceId the reference id of the activity or order line
     * @return {@code true} if the reply was accepted
     */
    Boolean replyNegotiation(String orderId, NegotiationAction action, String referenceId);

    /**
     * Replies to an order negotiation, approving or cancelling the proposed schedule, with
     * an optional reason (used when cancelling).
     *
     * @param orderId     the identifier of the order
     * @param action      the action to perform (APPROVE, APPROVE_NON_EXPEDITE, or CANCEL)
     * @param referenceId the reference id of the activity or order line
     * @param reason      the reason for the action (used when cancelling)
     * @return {@code true} if the reply was accepted
     */
    Boolean replyNegotiation(String orderId, NegotiationAction action, String referenceId, String reason);

    /**
     * Adds a note to an order.
     *
     * @param orderId the identifier of the order
     * @param text    the text of the note
     * @return {@code true} if the note was accepted
     */
    Boolean addNote(String orderId, String text);

    /**
     * Adds a note to an order with an optional reference id (for two-way notes) and attachments.
     *
     * @param orderId     the identifier of the order
     * @param text        the text of the note
     * @param referenceId the reference id associated with the note, or {@code null}
     * @param attachments references to previously uploaded attachments, or {@code null}
     * @return {@code true} if the note was accepted
     */
    Boolean addNote(String orderId, String text, String referenceId, List<AttachmentReference> attachments);

    /**
     * Cancels an order.
     *
     * @param orderId the identifier of the order
     * @param reason  the reason for cancellation
     * @return {@code true} if the cancellation was accepted
     */
    Boolean cancel(String orderId, String reason);

    /**
     * Cancels an order, optionally restricting cancellation to specific order lines and
     * attaching supporting documentation.
     *
     * @param orderId     the identifier of the order
     * @param reason      the reason for cancellation
     * @param attachments references to previously uploaded attachments, or {@code null}
     * @param lineIds     the order line ids to cancel, or {@code null} to cancel the whole order
     * @return {@code true} if the cancellation was accepted
     */
    Boolean cancel(String orderId, String reason, List<AttachmentReference> attachments, List<String> lineIds);
}
