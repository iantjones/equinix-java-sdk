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

import api.equinix.javasdk.customerportal.model.BetaTermsAgreement;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.DigitalLoaChange;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.PrivateBetaPermission;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PrivateBetaAccessRequest;

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
     * Searches Digital LOA documents by the supplied search criteria (first page, default ordering).
     *
     * @param request the search criteria body
     * @return the matching Digital LOA documents
     */
    List<? extends DigitalLoa> search(DigitalLoaSearchRequest request);

    /**
     * Searches Digital LOA documents by the supplied search criteria, paging and sorting the results.
     *
     * @param request the search criteria body
     * @param offset  the index of the first item returned (zero-based), or {@code null} for the default
     * @param limit   the maximum number of items returned per page, or {@code null} for the default
     * @param sort    the sort fields (e.g. {@code /expiryDateTime}, {@code -/expiryDateTime}), or {@code null}
     * @return the matching Digital LOA documents
     */
    List<? extends DigitalLoa> search(DigitalLoaSearchRequest request, Integer offset, Integer limit, List<String> sort);

    /**
     * Modifies a Digital LOA document by applying the supplied patch documents.
     *
     * @param uuid       the Digital LOA document identifier
     * @param operations the JSON patch documents to apply
     * @return the modified Digital LOA
     */
    DigitalLoa update(String uuid, List<Map<String, Object>> operations);

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

    /**
     * Lists the customer organizations available in the given IBX data center that the current user
     * may use as a Digital LOA requestor.
     *
     * <p>Private beta endpoint ({@code GET /diloa/v1/organizations}). The {@code location.ibx} query
     * parameter is required by the API.</p>
     *
     * @param ibx the IBX data center identifier (e.g. {@code AM11}); required
     * @return the customer organizations
     */
    List<? extends LoaCustomerOrganization> listOrganizations(String ibx);

    /**
     * Lists the customer organizations available in the given IBX data center, filtered by product
     * type, that the current user may use as a Digital LOA requestor.
     *
     * <p>Private beta endpoint ({@code GET /diloa/v1/organizations}). The {@code location.ibx} query
     * parameter is required by the API; {@code product.type} is optional.</p>
     *
     * @param ibx          the IBX data center identifier (e.g. {@code AM11}); required
     * @param productTypes the product types to filter by (e.g. {@code CROSS_CONNECT}), or {@code null}
     * @return the customer organizations
     */
    List<? extends LoaCustomerOrganization> listOrganizations(String ibx, List<String> productTypes);

    /**
     * Returns whether the current user is permitted to use the Digital LOA application in its
     * private beta phase ({@code GET /diloa/v1/privateBetaAccess}).
     *
     * @return the private beta permission
     */
    PrivateBetaPermission isPrivateBetaAllowed();

    /**
     * Submits a request for Digital LOA private beta access
     * ({@code POST /diloa/v1/privateBetaAccess}).
     *
     * @param request the private beta access request body
     * @return {@code true} if the request was submitted successfully
     */
    Boolean createPrivateBetaAccessRequest(PrivateBetaAccessRequest request);

    /**
     * Returns the current user's acceptance of the Digital LOA private beta terms
     * ({@code GET /diloa/v1/betaTermsAgreement}).
     *
     * @return the beta terms agreement
     */
    BetaTermsAgreement getBetaTermsAgreement();

    /**
     * Records the current user's acceptance of the Digital LOA private beta terms
     * ({@code PUT /diloa/v1/betaTermsAgreement}).
     *
     * @param agreementAccepted whether the beta terms are accepted
     * @return the updated beta terms agreement
     */
    BetaTermsAgreement updateBetaTermsAgreement(Boolean agreementAccepted);
}
