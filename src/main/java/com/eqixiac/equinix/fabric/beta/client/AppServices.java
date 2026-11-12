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
import com.eqixiac.equinix.fabric.beta.model.AppService;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppServicePostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Application Connect services (8 operations).
 *
 * <p>Obtain it from {@code fabric.beta().appServices()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface AppServices {

    /**
     * Create Service: {@code POST /fabric/v4/appServices} (operation {@code createAppService}).
     * <p>This API provides capability to create user's Service</p>
     *
     * @param request the {@code AppServicePostRequest} request body
     * @return the {@code AppService} the API returns
     */
    AppService create(AppServicePostRequest request);

    /**
     * Get Service: {@code GET /fabric/v4/appServices/{appServiceId}} (operation {@code getAppServiceByUuid}).
     * <p>This API provides capability to retrieve user's Service</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @return the {@code AppService} the API returns
     */
    AppService getByUuid(String appServiceId);

    /**
     * Delete Service: {@code DELETE /fabric/v4/appServices/{appServiceId}} (operation {@code deleteAppServiceByUuid}).
     * <p>This API provides capability to delete user's Service</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @return the {@code AppService} the API returns
     */
    AppService delete(String appServiceId);

    /**
     * Update Service: {@code PATCH /fabric/v4/appServices/{appServiceId}} (operation {@code updateAppServiceByUuid}).
     * <p>This API provides capability to update user's Service</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec. Patchable paths named by the spec: {@code /description}, {@code /name}, {@code /sourceDomains}.</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code AppService} the API returns
     */
    AppService update(String appServiceId, List<PatchOperation> operations);

    /**
     * Search Services: {@code POST /fabric/v4/appServices/search} (operation {@code searchAppServices}).
     * <p>The API provides capability to get list of user's App Services using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/deletedDateTime}, {@code /changeLog/updatedDateTime}, {@code /description}, {@code /endpoint}, {@code /name}, {@code /project/projectId}, {@code /sourceDomains}, {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /name}, {@code /state}, {@code /uuid}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppService}
     */
    PaginatedFilteredList<AppService> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Services: {@code POST /fabric/v4/appServices/search} (operation {@code searchAppServices}).
     * <p>The API provides capability to get list of user's App Services using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppService}
     */
    PaginatedFilteredList<AppService> search(FilterPropertyList filter);

    /**
     * Search Services: {@code POST /fabric/v4/appServices/search} (operation {@code searchAppServices}).
     * <p>The API provides capability to get list of user's App Services using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code AppService}
     */
    PaginatedFilteredList<AppService> search();

    /**
     * Get Attached Links for Service: {@code GET /fabric/v4/appServices/{appServiceId}/appLinks} (operation {@code getAttachedAppLinksByAppServiceId}).
     * <p>This API provides capability to retrieve App Links attached to an App Service</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code state} filters by state.</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @param query optional {@code state}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppServiceAttachedAppLink}
     */
    PaginatedList<AppServiceAttachedAppLink> listAttachedAppLinks(String appServiceId, AttachmentQuery query);

    /**
     * Get Attached Links for Service: {@code GET /fabric/v4/appServices/{appServiceId}/appLinks} (operation {@code getAttachedAppLinksByAppServiceId}).
     * <p>This API provides capability to retrieve App Links attached to an App Service</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @return a lazily paged list of {@code AppServiceAttachedAppLink}
     */
    PaginatedList<AppServiceAttachedAppLink> listAttachedAppLinks(String appServiceId);

    /**
     * Get Attached Subscriptions for Service: {@code GET /fabric/v4/appServices/{appServiceId}/appSubscriptions} (operation {@code getAttachedAppSubscriptionsByAppServiceId}).
     * <p>This API provides capability to retrieve App Subscriptions attached to an App Service</p>
     * <p>Paged with {@code offset}/{@code limit}; {@code state} filters by state.</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @param query optional {@code state}/{@code order}/{@code style} query parameters ({@code null} for none)
     * @return a lazily paged list of {@code AppServiceAttachedAppSubscription}
     */
    PaginatedList<AppServiceAttachedAppSubscription> listAttachedAppSubscriptions(String appServiceId, AttachmentQuery query);

    /**
     * Get Attached Subscriptions for Service: {@code GET /fabric/v4/appServices/{appServiceId}/appSubscriptions} (operation {@code getAttachedAppSubscriptionsByAppServiceId}).
     * <p>This API provides capability to retrieve App Subscriptions attached to an App Service</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @return a lazily paged list of {@code AppServiceAttachedAppSubscription}
     */
    PaginatedList<AppServiceAttachedAppSubscription> listAttachedAppSubscriptions(String appServiceId);

    /**
     * Search Attached Subscriptions: {@code POST /fabric/v4/appServices/{appServiceId}/appSubscriptions/search} (operation {@code searchAttachedAppSubscriptionsByAppServiceId}).
     * <p>The API provides capability to get list of App Subscriptions attached to an App Service using search criteria</p>
     * <p>Filter properties named by the spec: {@code /state}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /state}, {@code /uuid}.</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppServiceAttachedAppSubscription}
     */
    PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Attached Subscriptions: {@code POST /fabric/v4/appServices/{appServiceId}/appSubscriptions/search} (operation {@code searchAttachedAppSubscriptionsByAppServiceId}).
     * <p>The API provides capability to get list of App Subscriptions attached to an App Service using search criteria</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppServiceAttachedAppSubscription}
     */
    PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId, FilterPropertyList filter);

    /**
     * Search Attached Subscriptions: {@code POST /fabric/v4/appServices/{appServiceId}/appSubscriptions/search} (operation {@code searchAttachedAppSubscriptionsByAppServiceId}).
     * <p>The API provides capability to get list of App Subscriptions attached to an App Service using search criteria</p>
     *
     * @param appServiceId the {@code appServiceId} path parameter
     * @return a lazily paged list of {@code AppServiceAttachedAppSubscription}
     */
    PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId);
}
