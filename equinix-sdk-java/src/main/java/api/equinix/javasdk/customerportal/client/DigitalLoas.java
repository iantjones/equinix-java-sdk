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

import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.DigitalLoaChange;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;

import java.util.List;
import java.util.Map;

/**
 * Client interface for managing Digital Letters of Authorization (Digital LOAs) in the Equinix
 * Customer Portal.
 *
 * <p>Backed by the diLOA v1 API at {@code /diloa/v1/digitalLoas}. A Digital LOA is created with
 * {@link #create(DigitalLoaCreateRequest)}, retrieved with {@link #findByUuid(String)} or
 * {@link #search(DigitalLoaSearchRequest)}, modified with {@link #patch(String, List)}, actioned
 * with {@link #performAction(String, Map)} and invalidated with {@link #cancel(String)}. The audit
 * trail of changes is available via {@link #findChangesByLoaUuid(String)} and
 * {@link #findChangeByUuid(String, String)}.</p>
 */
public interface DigitalLoas {

    /**
     * Creates a Digital LOA document.
     *
     * @param request the create request body
     * @return the created Digital LOA
     */
    DigitalLoa create(DigitalLoaCreateRequest request);

    /**
     * Gets a Digital LOA document by its uuid.
     *
     * @param uuid the Digital LOA document identifier
     * @return the Digital LOA
     */
    DigitalLoa findByUuid(String uuid);

    /**
     * Searches Digital LOA documents by the supplied search criteria.
     *
     * @param request the search criteria body
     * @return the matching Digital LOA documents
     */
    List<? extends DigitalLoa> search(DigitalLoaSearchRequest request);

    /**
     * Modifies a Digital LOA document by applying the supplied patch documents.
     *
     * @param uuid       the Digital LOA document identifier
     * @param operations the JSON patch documents to apply
     * @return the modified Digital LOA
     */
    DigitalLoa patch(String uuid, List<Map<String, Object>> operations);

    /**
     * Cancels (invalidates) a Digital LOA document.
     *
     * @param uuid the Digital LOA document identifier
     * @return {@code true} if the Digital LOA was cancelled successfully
     */
    Boolean cancel(String uuid);

    /**
     * Performs an action on a Digital LOA document.
     *
     * @param uuid   the Digital LOA document identifier
     * @param action the action request body (the {@code type} of action is required)
     * @return the Digital LOA after the action was performed
     */
    DigitalLoa performAction(String uuid, Map<String, Object> action);

    /**
     * Creates a Digital LOA request.
     *
     * @param request the Digital LOA request body
     * @return {@code true} if the request was submitted successfully
     */
    Boolean createRequest(Map<String, Object> request);

    /**
     * Lists the change records for a Digital LOA document.
     *
     * @param uuid the Digital LOA document identifier
     * @return the Digital LOA change records
     */
    List<? extends DigitalLoaChange> findChangesByLoaUuid(String uuid);

    /**
     * Gets a specific change record for a Digital LOA document.
     *
     * @param uuid       the Digital LOA document identifier
     * @param changeUuid the Digital LOA change identifier
     * @return the Digital LOA change record
     */
    DigitalLoaChange findChangeByUuid(String uuid, String changeUuid);
}
