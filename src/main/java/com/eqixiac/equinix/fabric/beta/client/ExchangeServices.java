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
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceActionRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceActionResponse;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceCreateRequest;
import com.eqixiac.equinix.fabric.beta.model.ExchangeServiceResponse;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Internet Exchange (IX) services (5 operations).
 *
 * <p>Obtain it from {@code fabric.beta().exchangeServices()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface ExchangeServices {

    /**
     * Create Internet Exchange Service: {@code POST /fabric/v4/exchangeServices} (operation {@code createExchangeService}).
     * <p>This API provides capability to create Internet Exchange Service</p>
     *
     * @param request the {@code ExchangeServiceCreateRequest} request body
     * @return the {@code ExchangeServiceResponse} the API returns
     */
    ExchangeServiceResponse create(ExchangeServiceCreateRequest request);

    /**
     * Get Internet Exchange Service: {@code GET /fabric/v4/exchangeServices/{exchangeServiceId}} (operation {@code getExchangeServiceById}).
     * <p>The API provides capability to get Internet Exchange Service</p>
     *
     * @param exchangeServiceId the {@code exchangeServiceId} path parameter
     * @return the {@code ExchangeServiceResponse} the API returns
     */
    ExchangeServiceResponse getByUuid(String exchangeServiceId);

    /**
     * Update Internet Exchange Service: {@code PATCH /fabric/v4/exchangeServices/{exchangeServiceId}} (operation {@code updateExchangeServiceById}).
     * <p>The API provides capability to update Internet Exchange Service</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /description}, {@code /name}.</p>
     *
     * @param exchangeServiceId the {@code exchangeServiceId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code ExchangeServiceResponse} the API returns
     */
    ExchangeServiceResponse update(String exchangeServiceId, List<PatchOperation> operations);

    /**
     * Search Internet Exchange Services: {@code POST /fabric/v4/exchangeServices/search} (operation {@code searchExchangeService}).
     * <p>The API provides capability to get list of user's Internet Exchange Service using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /bandwidth}, {@code /changelog/createdDateTime}, {@code /changelog/deletedDateTime}, {@code /changelog/updatedDateTime}, {@code /location/metroCode}, {@code /name}, {@code /project/projectId}, {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code ExchangeServiceResponse}
     */
    PaginatedFilteredList<ExchangeServiceResponse> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Internet Exchange Services: {@code POST /fabric/v4/exchangeServices/search} (operation {@code searchExchangeService}).
     * <p>The API provides capability to get list of user's Internet Exchange Service using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code ExchangeServiceResponse}
     */
    PaginatedFilteredList<ExchangeServiceResponse> search(FilterPropertyList filter);

    /**
     * Search Internet Exchange Services: {@code POST /fabric/v4/exchangeServices/search} (operation {@code searchExchangeService}).
     * <p>The API provides capability to get list of user's Internet Exchange Service using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code ExchangeServiceResponse}
     */
    PaginatedFilteredList<ExchangeServiceResponse> search();

    /**
     * Create Internet Exchange Service Action: {@code POST /fabric/v4/exchangeServices/{exchangeServiceId}/actions} (operation {@code createExchangeServiceAction}).
     * <p>This API provides capability to perform actions on Internet Exchange Service</p>
     *
     * @param exchangeServiceId the {@code exchangeServiceId} path parameter
     * @param request the {@code ExchangeServiceActionRequest} request body
     * @return the {@code ExchangeServiceActionResponse} the API returns
     */
    ExchangeServiceActionResponse performAction(String exchangeServiceId, ExchangeServiceActionRequest request);
}
