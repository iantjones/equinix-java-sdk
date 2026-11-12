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
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.beta.model.AppDomain;
import com.eqixiac.equinix.fabric.beta.model.AppDomainAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppDomainPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Application Connect domains (6 operations).
 *
 * <p>Obtain it from {@code fabric.beta().appDomains()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface AppDomains {

    /**
     * Create Domain: {@code POST /fabric/v4/appDomains} (operation {@code createAppDomain}).
     * <p>This API provides capability to create user's App Domain</p>
     *
     * @param request the {@code AppDomainPostRequest} request body
     * @return the {@code AppDomain} the API returns
     */
    AppDomain create(AppDomainPostRequest request);

    /**
     * Get Domain: {@code GET /fabric/v4/appDomains/{appDomainId}} (operation {@code getAppDomainByUuid}).
     * <p>This API provides capability to retrieve user's Domain</p>
     *
     * @param appDomainId the {@code appDomainId} path parameter
     * @return the {@code AppDomain} the API returns
     */
    AppDomain getByUuid(String appDomainId);

    /**
     * Delete Domain: {@code DELETE /fabric/v4/appDomains/{appDomainId}} (operation {@code deleteAppDomainByUuid}).
     * <p>This API provides capability to delete user's Domain</p>
     *
     * @param appDomainId the {@code appDomainId} path parameter
     * @return the {@code AppDomain} the API returns
     */
    AppDomain delete(String appDomainId);

    /**
     * Update Domain: {@code PATCH /fabric/v4/appDomains/{appDomainId}} (operation {@code updateAppDomainByUuid}).
     * <p>This API provides capability to update user's Domain</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /description}.</p>
     *
     * @param appDomainId the {@code appDomainId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code AppDomain} the API returns
     */
    AppDomain update(String appDomainId, List<PatchOperation> operations);

    /**
     * Search Domains: {@code POST /fabric/v4/appDomains/search} (operation {@code searchAppDomains}).
     * <p>The API provides capability to get list of user's App Domains using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/deletedDateTime}, {@code /changeLog/updatedDateTime}, {@code /description}, {@code /name}, {@code /project/projectId}, {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}, {@code /uuid}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppDomain}
     */
    PaginatedFilteredList<AppDomain> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Domains: {@code POST /fabric/v4/appDomains/search} (operation {@code searchAppDomains}).
     * <p>The API provides capability to get list of user's App Domains using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppDomain}
     */
    PaginatedFilteredList<AppDomain> search(FilterPropertyList filter);

    /**
     * Search Domains: {@code POST /fabric/v4/appDomains/search} (operation {@code searchAppDomains}).
     * <p>The API provides capability to get list of user's App Domains using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code AppDomain}
     */
    PaginatedFilteredList<AppDomain> search();

    /**
     * Get Attached Links for Domain: {@code GET /fabric/v4/appDomains/{appDomainId}/appLinks} (operation {@code getAttachedAppLinksByAppDomainId}).
     * <p>This API provides capability to retrieve App Links attached to an App Domain</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code state} filters by state.</p>
     *
     * @param appDomainId the {@code appDomainId} path parameter
     * @param query optional {@code state}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppDomainAttachedAppLink}
     */
    PaginatedList<AppDomainAttachedAppLink> listAttachedAppLinks(String appDomainId, AttachmentQuery query);

    /**
     * Get Attached Links for Domain: {@code GET /fabric/v4/appDomains/{appDomainId}/appLinks} (operation {@code getAttachedAppLinksByAppDomainId}).
     * <p>This API provides capability to retrieve App Links attached to an App Domain</p>
     *
     * @param appDomainId the {@code appDomainId} path parameter
     * @return a lazily paged list of {@code AppDomainAttachedAppLink}
     */
    PaginatedList<AppDomainAttachedAppLink> listAttachedAppLinks(String appDomainId);
}
