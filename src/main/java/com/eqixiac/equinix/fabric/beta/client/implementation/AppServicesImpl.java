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

package com.eqixiac.equinix.fabric.beta.client.implementation;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.AppServices;
import com.eqixiac.equinix.fabric.beta.client.AttachmentQuery;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.AppService;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppServiceAttachedAppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppServicePostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AppServices} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class AppServicesImpl implements AppServices {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public AppServicesImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public AppService create(AppServicePostRequest request) {
        return client("AppServices", AppService.class).one("CreateAppService", Map.of(), request);
    }

    @Override
    public AppService getByUuid(String appServiceId) {
        return client("AppServices", AppService.class).one("GetAppServiceByUuid", Map.of("appServiceId", appServiceId), null);
    }

    @Override
    public AppService delete(String appServiceId) {
        return client("AppServices", AppService.class).one("DeleteAppServiceByUuid", Map.of("appServiceId", appServiceId), null);
    }

    @Override
    public AppService update(String appServiceId, List<PatchOperation> operations) {
        return client("AppServices", AppService.class).patch("UpdateAppServiceByUuid", Map.of("appServiceId", appServiceId), operations, true);
    }

    @Override
    public PaginatedFilteredList<AppService> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("AppServices", AppService.class).search("SearchAppServices", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppService> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<AppService> search() {
        return search(null, null);
    }

    @Override
    public PaginatedList<AppServiceAttachedAppLink> listAttachedAppLinks(String appServiceId, AttachmentQuery query) {
        return client("AppServices", AppServiceAttachedAppLink.class).list("GetAttachedAppLinksByAppServiceId", Map.of("appServiceId", appServiceId), query == null ? null : query.toQueryParams("state"));
    }

    @Override
    public PaginatedList<AppServiceAttachedAppLink> listAttachedAppLinks(String appServiceId) {
        return listAttachedAppLinks(appServiceId, null);
    }

    @Override
    public PaginatedList<AppServiceAttachedAppSubscription> listAttachedAppSubscriptions(String appServiceId, AttachmentQuery query) {
        return client("AppServices", AppServiceAttachedAppSubscription.class).list("GetAttachedAppSubscriptionsByAppServiceId", Map.of("appServiceId", appServiceId), query == null ? null : query.toQueryParams("state"));
    }

    @Override
    public PaginatedList<AppServiceAttachedAppSubscription> listAttachedAppSubscriptions(String appServiceId) {
        return listAttachedAppSubscriptions(appServiceId, null);
    }

    @Override
    public PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId, FilterPropertyList filter, SortPropertyList sort) {
        return client("AppServices", AppServiceAttachedAppSubscription.class).search("SearchAttachedAppSubscriptionsByAppServiceId", Map.of("appServiceId", appServiceId), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId, FilterPropertyList filter) {
        return searchAttachedAppSubscriptions(appServiceId, filter, null);
    }

    @Override
    public PaginatedFilteredList<AppServiceAttachedAppSubscription> searchAttachedAppSubscriptions(String appServiceId) {
        return searchAttachedAppSubscriptions(appServiceId, null, null);
    }
}
