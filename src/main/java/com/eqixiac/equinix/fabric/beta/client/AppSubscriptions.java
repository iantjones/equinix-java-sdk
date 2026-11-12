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
import com.eqixiac.equinix.fabric.beta.model.AppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppSubscriptionPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Application Connect subscriptions (5 operations).
 *
 * <p>Obtain it from {@code fabric.beta().appSubscriptions()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface AppSubscriptions {

    /**
     * Create Subscription: {@code POST /fabric/v4/appSubscriptions} (operation {@code createAppSubscription}).
     * <p>This API provides capability to create user's Subscription</p>
     *
     * @param request the {@code AppSubscriptionPostRequest} request body
     * @return the {@code AppSubscription} the API returns
     */
    AppSubscription create(AppSubscriptionPostRequest request);

    /**
     * Get Subscription: {@code GET /fabric/v4/appSubscriptions/{appSubscriptionId}} (operation {@code getAppSubscriptionByUuid}).
     * <p>This API provides capability to retrieve user's Subscription</p>
     *
     * @param appSubscriptionId the {@code appSubscriptionId} path parameter
     * @return the {@code AppSubscription} the API returns
     */
    AppSubscription getByUuid(String appSubscriptionId);

    /**
     * Delete Subscription: {@code DELETE /fabric/v4/appSubscriptions/{appSubscriptionId}} (operation {@code deleteAppSubscriptionByUuid}).
     * <p>This API provides capability to delete user's Subscription</p>
     *
     * @param appSubscriptionId the {@code appSubscriptionId} path parameter
     * @return the {@code AppSubscription} the API returns
     */
    AppSubscription delete(String appSubscriptionId);

    /**
     * Update Subscription: {@code PATCH /fabric/v4/appSubscriptions/{appSubscriptionId}} (operation {@code updateAppSubscriptionByUuid}).
     * <p>This API provides capability to update user's Subscription</p>
     * <p>Sent as {@code application/json-patch+json}, per the spec.</p>
     *
     * @param appSubscriptionId the {@code appSubscriptionId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code AppSubscription} the API returns
     */
    AppSubscription update(String appSubscriptionId, List<PatchOperation> operations);

    /**
     * Search Subscriptions: {@code POST /fabric/v4/appSubscriptions/search} (operation {@code searchAppSubscriptions}).
     * <p>The API provides capability to get list of user's App Subscriptions using search criteria, including optional filtering, pagination and sorting</p>
     * <p>Filter properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/deletedDateTime}, {@code /changeLog/updatedDateTime}, {@code /project/projectId}, {@code /source/appLink/uuid}, {@code /source/ipSubnets}, {@code /state}, {@code /target/appService/uuid}, {@code /target/geoScope}, {@code /target/prioritization}, {@code /uuid}.</p>
     * <p>Sort properties named by the spec: {@code /changeLog/createdDateTime}, {@code /changeLog/updatedDateTime}, {@code /state}, {@code /uuid}.</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code AppSubscription}
     */
    PaginatedFilteredList<AppSubscription> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search Subscriptions: {@code POST /fabric/v4/appSubscriptions/search} (operation {@code searchAppSubscriptions}).
     * <p>The API provides capability to get list of user's App Subscriptions using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code AppSubscription}
     */
    PaginatedFilteredList<AppSubscription> search(FilterPropertyList filter);

    /**
     * Search Subscriptions: {@code POST /fabric/v4/appSubscriptions/search} (operation {@code searchAppSubscriptions}).
     * <p>The API provides capability to get list of user's App Subscriptions using search criteria, including optional filtering, pagination and sorting</p>
     *
     * @return a lazily paged list of {@code AppSubscription}
     */
    PaginatedFilteredList<AppSubscription> search();
}
