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
import com.eqixiac.equinix.fabric.beta.client.AppDomains;
import com.eqixiac.equinix.fabric.beta.client.AttachmentQuery;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.AppDomain;
import com.eqixiac.equinix.fabric.beta.model.AppDomainAttachedAppLink;
import com.eqixiac.equinix.fabric.beta.model.AppDomainPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AppDomains} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class AppDomainsImpl implements AppDomains {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public AppDomainsImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public AppDomain create(AppDomainPostRequest request) {
        return client("AppDomains", AppDomain.class).one("CreateAppDomain", Map.of(), request);
    }

    @Override
    public AppDomain getByUuid(String appDomainId) {
        return client("AppDomains", AppDomain.class).one("GetAppDomainByUuid", Map.of("appDomainId", appDomainId), null);
    }

    @Override
    public AppDomain delete(String appDomainId) {
        return client("AppDomains", AppDomain.class).one("DeleteAppDomainByUuid", Map.of("appDomainId", appDomainId), null);
    }

    @Override
    public AppDomain update(String appDomainId, List<PatchOperation> operations) {
        return client("AppDomains", AppDomain.class).patch("UpdateAppDomainByUuid", Map.of("appDomainId", appDomainId), operations, true);
    }

    @Override
    public PaginatedFilteredList<AppDomain> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("AppDomains", AppDomain.class).search("SearchAppDomains", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppDomain> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<AppDomain> search() {
        return search(null, null);
    }

    @Override
    public PaginatedList<AppDomainAttachedAppLink> listAttachedAppLinks(String appDomainId, AttachmentQuery query) {
        return client("AppDomains", AppDomainAttachedAppLink.class).list("GetAttachedAppLinksByAppDomainId", Map.of("appDomainId", appDomainId), query == null ? null : query.toQueryParams("state"));
    }

    @Override
    public PaginatedList<AppDomainAttachedAppLink> listAttachedAppLinks(String appDomainId) {
        return listAttachedAppLinks(appDomainId, null);
    }
}
