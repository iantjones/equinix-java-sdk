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
import com.eqixiac.equinix.fabric.beta.model.AppProcessor;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;

/**
 * Application Connect processors (2 operations).
 *
 * <p>Obtain it from {@code fabric.beta().appProcessors()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface AppProcessors {

    /**
     * Get Processor: {@code GET /fabric/v4/appProcessors/{appProcessorId}} (operation {@code getAppProcessorByUuid}).
     * <p>This API provides capability to retrieve user's App Processor</p>
     *
     * @param appProcessorId the {@code appProcessorId} path parameter
     * @return the {@code AppProcessor} the API returns
     */
    AppProcessor getByUuid(String appProcessorId);

    /**
     * Search Processors: {@code POST /fabric/v4/appProcessors/search} (operation {@code searchAppProcessors}).
     * <p>The API provides capability to get list of user's App Processors using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /bandwidth}, {@code /changeLog/createdDateTime}, {@code /changeLog/deletedDateTime}, {@code /changeLog/updatedDateTime}, {@code /description}, {@code /ipv4Subnet}, {@code /name}, {@code /project/projectId}, {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}, {@code /uuid}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppProcessor}
     */
    PaginatedFilteredList<AppProcessor> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Processors: {@code POST /fabric/v4/appProcessors/search} (operation {@code searchAppProcessors}).
     * <p>The API provides capability to get list of user's App Processors using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppProcessor}
     */
    PaginatedFilteredList<AppProcessor> search(FilterPropertyList filter);

    /**
     * Search Processors: {@code POST /fabric/v4/appProcessors/search} (operation {@code searchAppProcessors}).
     * <p>The API provides capability to get list of user's App Processors using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code AppProcessor}
     */
    PaginatedFilteredList<AppProcessor> search();
}
