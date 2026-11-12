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
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.beta.model.CreateCrossConnect;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionRequest;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectActionResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectAttachmentResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectAttachmentsResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectDiverse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectNoteResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectScheduleAvailability;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectScheduleResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectServiceResponse;
import com.eqixiac.equinix.fabric.beta.model.CrossConnectServicesResponse;
import com.eqixiac.equinix.fabric.beta.model.DiverseCrossConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.SchedulesResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Fabric v4 cross connects (real physical orders) (12 operations).
 *
 * <p>Obtain it from {@code fabric.beta().crossConnects()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface CrossConnects {

    /**
     * Create Cross Connect: {@code POST /fabric/v4/crossConnects} (operation {@code createCrossConnect}).
     * <p>The API provides capability to create a new Cross Connect.</p>
     *
     * @param request the {@code CreateCrossConnect} request body
     * @return the {@code CrossConnectResponse} the API returns
     */
    CrossConnectResponse create(CreateCrossConnect request);

    /**
     * Create Diverse Cross Connect: {@code POST /fabric/v4/crossConnects/bulk} (operation {@code createDiverseCrossConnect}).
     * <p>The API provides capability to create several Cross Connects in a single order - a diverse pair where the PRIMARY and the SECONDARY Cross Connect are placed at the same time, or multiple Cross Connects by sending one entry per Cross Connect.</p>
     * <p>Unwraps the {@code CrossConnectDiverse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param request the {@code DiverseCrossConnectRequest} request body
     * @return the {@code CrossConnectResponse} entries (empty when none)
     */
    List<CrossConnectResponse> createDiverse(DiverseCrossConnectRequest request);

    /**
     * Search Cross Connects: {@code POST /fabric/v4/crossConnects/search} (operation {@code searchCrossConnects}).
     * <p>The API provides capability to get list of user's Cross Connects using search criteria, including optional filtering, pagination and sorting.</p>
     * <p>Filter properties named by the spec: {@code /aSide/demarcationPoint/patchPanelId}, {@code /aSide/demarcationPoint/patchPanelPortA}, {@code /aSide/demarcationPoint/patchPanelPortB}, {@code /aSide/location/ibxCode}, {@code /account/accountNumber}, {@code /bmmrType}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /circuitId}, {@code /name}, {@code /order/customerReferenceId}, {@code /order/orderNumber}, {@code /redundancy/group}, {@code /redundancy/priority}, {@code /serialNumber}, {@code /state}, {@code /type}, {@code /uuid}, {@code /zSide/demarcationPoint/patchPanelId}, {@code /zSide/demarcationPoint/patchPanelPortA}, {@code /zSide/demarcationPoint/patchPanelPortB}, {@code /zSide/loa/uuid}, {@code /zSide/location/ibxCode}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code CrossConnectResponse}
     */
    PaginatedFilteredList<CrossConnectResponse> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Cross Connects: {@code POST /fabric/v4/crossConnects/search} (operation {@code searchCrossConnects}).
     * <p>The API provides capability to get list of user's Cross Connects using search criteria, including optional filtering, pagination and sorting.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code CrossConnectResponse}
     */
    PaginatedFilteredList<CrossConnectResponse> search(FilterPropertyList filter);

    /**
     * Search Cross Connects: {@code POST /fabric/v4/crossConnects/search} (operation {@code searchCrossConnects}).
     * <p>The API provides capability to get list of user's Cross Connects using search criteria, including optional filtering, pagination and sorting.</p>
     *
     * @return a lazily paged list of {@code CrossConnectResponse}
     */
    PaginatedFilteredList<CrossConnectResponse> search();

    /**
     * Get Cross Connect: {@code GET /fabric/v4/crossConnects/{crossConnectId}} (operation {@code getCrossConnectByUuid}).
     * <p>The API provides capability to get Cross Connect details by Cross Connect ID.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return the {@code CrossConnectResponse} the API returns
     */
    CrossConnectResponse getByUuid(String crossConnectId);

    /**
     * Get Cross Connect Schedules: {@code GET /fabric/v4/crossConnects/{crossConnectId}/schedules} (operation {@code getCrossConnectSchedulesByUuid}).
     * <p>The API provides capability to get the schedules of a Cross Connect by Cross Connect ID.</p>
     * <p>Unwraps the {@code SchedulesResponse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return the {@code CrossConnectScheduleResponse} entries (empty when none)
     */
    List<CrossConnectScheduleResponse> getSchedules(String crossConnectId);

    /**
     * Get Cross Connect Services: {@code GET /fabric/v4/crossConnects/{crossConnectId}/services} (operation {@code getCrossConnectServicesByUuid}).
     * <p>The API provides capability to get the services ordered alongside a Cross Connect by Cross Connect ID.</p>
     * <p>Unwraps the {@code CrossConnectServicesResponse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return the {@code CrossConnectServiceResponse} entries (empty when none)
     */
    List<CrossConnectServiceResponse> getServices(String crossConnectId);

    /**
     * Get Cross Connect Attachments: {@code GET /fabric/v4/crossConnects/{crossConnectId}/attachments} (operation {@code getCrossConnectAttachmentsByUuid}).
     * <p>The API provides capability to get the documents attached to a Cross Connect by Cross Connect ID.</p>
     * <p>Unwraps the {@code CrossConnectAttachmentsResponse} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return the {@code CrossConnectAttachmentResponse} entries (empty when none)
     */
    List<CrossConnectAttachmentResponse> getAttachments(String crossConnectId);

    /**
     * Get Cross Connect Notes: {@code GET /fabric/v4/crossConnects/{crossConnectId}/notes} (operation {@code getCrossConnectNotesByUuid}).
     * <p>The API provides capability to get the notes added to a Cross Connect by Cross Connect ID.</p>
     * <p>Paged with {@code offset}/{@code limit}.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return a lazily paged list of {@code CrossConnectNoteResponse}
     */
    PaginatedList<CrossConnectNoteResponse> listNotes(String crossConnectId);

    /**
     * Cross Connect Actions: {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions} (operation {@code performCrossConnectAction}).
     * <p>The API provides capability to perform an action on a Cross Connect that is in the PENDING_ACTION state. The applicable action types are listed in the allowedActions of the Cross Connect.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @param request the {@code CrossConnectActionRequest} request body
     * @return the {@code CrossConnectActionResponse} the API returns
     */
    CrossConnectActionResponse performAction(String crossConnectId, CrossConnectActionRequest request);

    /**
     * Get Cross Connect Action by Action ID: {@code GET /fabric/v4/crossConnects/{crossConnectId}/actions/{actionId}} (operation {@code getCrossConnectActionByUuid}).
     * <p>The API provides capability to fetch Cross Connect action details by Action ID.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @param actionId the {@code actionId} path parameter
     * @return the {@code CrossConnectActionResponse} the API returns
     */
    CrossConnectActionResponse getAction(String crossConnectId, String actionId);

    /**
     * Search Cross Connect Actions: {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions/search} (operation {@code searchCrossConnectAction}).
     * <p>The API provides capability to get list of Cross Connect Actions using search criteria, including optional filtering, pagination and sorting.</p>
     * <p>Filter properties named by the spec: {@code /state}, {@code /type}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code CrossConnectActionResponse}
     */
    PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Cross Connect Actions: {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions/search} (operation {@code searchCrossConnectAction}).
     * <p>The API provides capability to get list of Cross Connect Actions using search criteria, including optional filtering, pagination and sorting.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code CrossConnectActionResponse}
     */
    PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId, FilterPropertyList filter);

    /**
     * Search Cross Connect Actions: {@code POST /fabric/v4/crossConnects/{crossConnectId}/actions/search} (operation {@code searchCrossConnectAction}).
     * <p>The API provides capability to get list of Cross Connect Actions using search criteria, including optional filtering, pagination and sorting.</p>
     *
     * @param crossConnectId the {@code crossConnectId} path parameter
     * @return a lazily paged list of {@code CrossConnectActionResponse}
     */
    PaginatedFilteredList<CrossConnectActionResponse> searchActions(String crossConnectId);

    /**
     * Search Cross Connect Schedules: {@code POST /fabric/v4/crossConnectSchedules/search} (operation {@code searchCrossConnectSchedules}).
     * <p>The API provides capability to search the Cross Connect schedule windows offered at an IBX, including optional filtering, pagination and sorting.</p>
     * <p>Filter properties named by the spec: {@code /aSide/location/ibxCode}, {@code /code}, {@code /type}, {@code /zSide/location/ibxCode}.</p>
     * <p>Sort properties named by the spec: {@code /code}, {@code /endDateTime}, {@code /startDateTime}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code CrossConnectScheduleAvailability}
     */
    PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Cross Connect Schedules: {@code POST /fabric/v4/crossConnectSchedules/search} (operation {@code searchCrossConnectSchedules}).
     * <p>The API provides capability to search the Cross Connect schedule windows offered at an IBX, including optional filtering, pagination and sorting.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code CrossConnectScheduleAvailability}
     */
    PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules(FilterPropertyList filter);

    /**
     * Search Cross Connect Schedules: {@code POST /fabric/v4/crossConnectSchedules/search} (operation {@code searchCrossConnectSchedules}).
     * <p>The API provides capability to search the Cross Connect schedule windows offered at an IBX, including optional filtering, pagination and sorting.</p>
     *
     * @return a lazily paged list of {@code CrossConnectScheduleAvailability}
     */
    PaginatedFilteredList<CrossConnectScheduleAvailability> searchSchedules();
}
