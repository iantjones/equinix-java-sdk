/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.beta.model.CreateLoa;
import com.eqixiac.equinix.fabric.beta.model.CreateLoaNote;
import com.eqixiac.equinix.fabric.beta.model.LoaActionRequest;
import com.eqixiac.equinix.fabric.beta.model.LoaActionResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaConsumer;
import com.eqixiac.equinix.fabric.beta.model.LoaConsumersResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaNoteDetails;
import com.eqixiac.equinix.fabric.beta.model.LoaNotesResponse;
import com.eqixiac.equinix.fabric.beta.model.LoaResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Digital Letters of Authorization (LOAs) (10 operations).
 *
 * <p>Obtain it from {@code fabric.beta().loas()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface Loas {

    /**
     * Create Loa: {@code POST /fabric/v4/loas} (operation {@code createLoa}).
     * <p>The API provides capability to create a new Loa</p>
     *
     * @param request the {@code CreateLoa} request body
     * @return the {@code LoaResponse} the API returns
     */
    LoaResponse create(CreateLoa request);

    /**
     * Get Loa: {@code GET /fabric/v4/loas/{loaId}} (operation {@code getLoaByUuid}).
     * <p>The API provides capability to get Loa details by Loa ID</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @return the {@code LoaResponse} the API returns
     */
    LoaResponse getByUuid(String loaId);

    /**
     * Update Loa: {@code PATCH /fabric/v4/loas/{loaId}} (operation {@code updateLoaByUuid}).
     * <p>The API provides capability to update Loa details by Loa ID</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /demarcationPoint/cageUniqueSpaceId}, {@code /demarcationPoint/connectorType}, {@code /demarcationPoint/patchPanelId}, {@code /demarcationPoint/patchPanelPortA}, {@code /demarcationPoint/patchPanelPortB}, {@code /description}, {@code /expirationDateTime}, {@code /issuer/email}, {@code /issuer/orgName}, {@code /location/ibxCode}, {@code /name}, {@code /requestor/email}, {@code /requestor/orgName}.</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code LoaResponse} the API returns
     */
    LoaResponse update(String loaId, List<PatchOperation> operations);

    /**
     * Create LOA Action: {@code POST /fabric/v4/loas/{loaId}/actions} (operation {@code performLoaAction}).
     * <p>The API provides capability to perform actions on Loa</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param request the {@code LoaActionRequest} request body
     * @return the {@code LoaActionResponse} the API returns
     */
    LoaActionResponse performAction(String loaId, LoaActionRequest request);

    /**
     * Get Loa Action by Action ID: {@code GET /fabric/v4/loas/{loaId}/actions/{actionId}} (operation {@code getLoaActionsByUuid}).
     * <p>This API provides capability to fetch action details</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param actionId the {@code actionId} path parameter
     * @return the {@code LoaActionResponse} the API returns
     */
    LoaActionResponse getAction(String loaId, String actionId);

    /**
     * Search Loas: {@code POST /fabric/v4/loas/search} (operation {@code searchLoa}).
     * <p>The API provides capability to get list of user's Loa using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /authorizedProductType}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /demarcationPoint/cageUniqueSpaceId}, {@code /demarcationPoint/connectorType}, {@code /demarcationPoint/patchPanelId}, {@code /demarcationPoint/patchPanelPortA}, {@code /demarcationPoint/patchPanelPortB}, {@code /expirationDateTime}, {@code /issuer/email}, {@code /issuer/orgId}, {@code /issuer/orgName}, {@code /location/ibxCode}, {@code /name}, {@code /requestor/email}, {@code /requestor/orgId}, {@code /requestor/orgName}, {@code /state}, {@code /type}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /expirationDateTime}, {@code /name}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code LoaResponse}
     */
    PaginatedFilteredList<LoaResponse> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Loas: {@code POST /fabric/v4/loas/search} (operation {@code searchLoa}).
     * <p>The API provides capability to get list of user's Loa using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code LoaResponse}
     */
    PaginatedFilteredList<LoaResponse> search(FilterPropertyList filter);

    /**
     * Search Loas: {@code POST /fabric/v4/loas/search} (operation {@code searchLoa}).
     * <p>The API provides capability to get list of user's Loa using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code LoaResponse}
     */
    PaginatedFilteredList<LoaResponse> search();

    /**
     * Search Loa Actions: {@code POST /fabric/v4/loas/{loaId}/actions/search} (operation {@code searchLoaAction}).
     * <p>The API provides capability to get list of user's Loa Actions using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /state}, {@code /type}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}.</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code LoaActionResponse}
     */
    PaginatedFilteredList<LoaActionResponse> searchActions(String loaId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Loa Actions: {@code POST /fabric/v4/loas/{loaId}/actions/search} (operation {@code searchLoaAction}).
     * <p>The API provides capability to get list of user's Loa Actions using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code LoaActionResponse}
     */
    PaginatedFilteredList<LoaActionResponse> searchActions(String loaId, FilterPropertyList filter);

    /**
     * Search Loa Actions: {@code POST /fabric/v4/loas/{loaId}/actions/search} (operation {@code searchLoaAction}).
     * <p>The API provides capability to get list of user's Loa Actions using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @return a lazily paged list of {@code LoaActionResponse}
     */
    PaginatedFilteredList<LoaActionResponse> searchActions(String loaId);

    /**
     * Get Loa Consumers: {@code GET /fabric/v4/loas/{loaId}/consumers} (operation {@code getLoaConsumersByLoaId}).
     * <p>The API provides capability to get Loa consumers by Loa ID</p>
     * <p>Unwraps the {@code LoaConsumersResponse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @return the {@code LoaConsumer} entries (empty when none)
     */
    List<LoaConsumer> getConsumers(String loaId);

    /**
     * Get Loa Notes: {@code GET /fabric/v4/loas/{loaId}/notes} (operation {@code getLoaNotesByUuid}).
     * <p>The API provides capability to get Loa notes by Loa ID</p>
     * <p>Unwraps the {@code LoaNotesResponse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @return the {@code LoaNoteDetails} entries (empty when none)
     */
    List<LoaNoteDetails> getNotes(String loaId);

    /**
     * Create Loa Note: {@code POST /fabric/v4/loas/{loaId}/notes} (operation {@code createLoaNoteByLoaId}).
     * <p>The API provides capability to create Loa note by Loa ID</p>
     *
     * @param loaId the {@code loaId} path parameter
     * @param request the {@code CreateLoaNote} request body
     * @return the {@code LoaNoteDetails} the API returns
     */
    LoaNoteDetails createNote(String loaId, CreateLoaNote request);
}
