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

import api.equinix.javasdk.customerportal.model.EmailDetails;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseCancelRequest;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseNoteRequest;

/**
 * Client interface for raising and managing trouble tickets / support cases in the Equinix
 * Customer Portal.
 *
 * <p>Backed by the support v2 API at {@code /support/v2/tickets} (with the add-notes-by-case-number
 * operation falling back to the v1 path {@code /support/v1/tickets/{caseNumber}/notes}). A ticket is
 * raised with {@link #create(SupportCaseCreateRequest)} and retrieved by case or order number with
 * {@link #getByCaseOrOrderNumber(String)}; notes may be appended and the ticket cancelled.</p>
 */
public interface SupportCases {

    /**
     * Creates a trouble ticket / case.
     *
     * @param request the ticket create request body
     * @return the created case or order number (the {@code id} of the {@code TicketResponse} body)
     */
    String create(SupportCaseCreateRequest request);

    /**
     * Gets a trouble ticket / case by its case or order number.
     *
     * @param id the case or order number
     * @return the trouble ticket / case
     */
    SupportCase getByCaseOrOrderNumber(String id);

    /**
     * Adds a note to a trouble ticket / case identified by its case or order number.
     *
     * @param id      the case or order number
     * @param request the note request body
     * @return {@code true} if the note was added successfully
     */
    Boolean addNotesById(String id, SupportCaseNoteRequest request);

    /**
     * Cancels a trouble ticket / case identified by its case or order number.
     *
     * @param id      the case or order number
     * @param request the cancellation request body
     * @return {@code true} if the ticket was cancelled successfully
     */
    Boolean cancel(String id, SupportCaseCancelRequest request);

    /**
     * Adds a note to a case identified by its case number, via the support v1 API.
     *
     * @param caseNumber the case number
     * @param request    the note request body
     * @return {@code true} if the note was added successfully
     */
    Boolean addNotesByCaseNumber(String caseNumber, SupportCaseNoteRequest request);

    /**
     * Downloads the binary content of an attachment on a support case.
     *
     * <p>Maps to {@code GET /support/v2/tickets/attachment/download/{caseId}/{attachmentId}}.</p>
     *
     * @param caseId       the case or order number the attachment belongs to
     * @param attachmentId the attachment id
     * @return the raw attachment bytes
     */
    byte[] downloadAttachment(String caseId, String attachmentId);

    /**
     * Retrieves the full details of a single email associated with a support case.
     *
     * <p>Maps to {@code GET /support/v1/tickets/emailDetails/{emailId}/caseNumber/{caseNumber}}.</p>
     *
     * @param emailId    the email id
     * @param caseNumber the case number the email belongs to
     * @return the email details
     */
    EmailDetails getEmailDetails(String emailId, String caseNumber);
}
