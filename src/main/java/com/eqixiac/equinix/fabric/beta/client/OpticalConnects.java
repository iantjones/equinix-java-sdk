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
import com.eqixiac.equinix.fabric.beta.model.BulkOpticalConnectRequest;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectBulk;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectPostRequest;
import com.eqixiac.equinix.fabric.beta.model.OpticalConnectResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Optical Metro Connect services (4 operations).
 *
 * <p>Obtain it from {@code fabric.beta().opticalConnects()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface OpticalConnects {

    /**
     * Create Optical Metro Connect Service: {@code POST /fabric/v4/opticalConnects} (operation {@code createOpticalConnect}).
     * <p>Create a single Optical Metro Connect circuit.</p>
     *
     * @param request the {@code OpticalConnectPostRequest} request body
     * @return the {@code OpticalConnectResponse} the API returns
     */
    OpticalConnectResponse create(OpticalConnectPostRequest request);

    /**
     * Create Dual Diverse Optical Metro Connect Service: {@code POST /fabric/v4/opticalConnects/bulk} (operation {@code createBulkOpticalConnect}).
     * <p>Create a dual diverse pair of circuits on separate optical paths.</p>
     * <p>Unwraps the {@code OpticalConnectBulk} {@code data} envelope (the spec documents no paging for it).</p>
     *
     * @param request the {@code BulkOpticalConnectRequest} request body
     * @return the {@code OpticalConnectResponse} entries (empty when none)
     */
    List<OpticalConnectResponse> createDiverse(BulkOpticalConnectRequest request);

    /**
     * Get Optical Metro Connect Service: {@code GET /fabric/v4/opticalConnects/{opticalConnectId}} (operation {@code getOpticalConnectByUuid}).
     * <p>Get a single Optical Metro Connect by UUID.</p>
     *
     * @param opticalConnectId the {@code opticalConnectId} path parameter
     * @return the {@code OpticalConnectResponse} the API returns
     */
    OpticalConnectResponse getByUuid(String opticalConnectId);

    /**
     * Search Optical Metro Connect Services: {@code POST /fabric/v4/opticalConnects/search} (operation {@code searchOpticalConnect}).
     * <p>Get Optical Metro Connects matching the supplied criteria, with optional filtering, pagination and sorting.</p>
     * <p>Filter properties named by the spec: {@code /bandwidth}, {@code /name}, {@code /state}, {@code /type}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /aSide/location/ibxCode}, {@code /bandwidth}, {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}, {@code /type}, {@code /uuid}, {@code /zSide/location/ibxCode}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code OpticalConnectResponse}
     */
    PaginatedFilteredList<OpticalConnectResponse> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Optical Metro Connect Services: {@code POST /fabric/v4/opticalConnects/search} (operation {@code searchOpticalConnect}).
     * <p>Get Optical Metro Connects matching the supplied criteria, with optional filtering, pagination and sorting.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code OpticalConnectResponse}
     */
    PaginatedFilteredList<OpticalConnectResponse> search(FilterPropertyList filter);

    /**
     * Search Optical Metro Connect Services: {@code POST /fabric/v4/opticalConnects/search} (operation {@code searchOpticalConnect}).
     * <p>Get Optical Metro Connects matching the supplied criteria, with optional filtering, pagination and sorting.</p>
     *
     * @return a lazily paged list of {@code OpticalConnectResponse}
     */
    PaginatedFilteredList<OpticalConnectResponse> search();
}
