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
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.AppSubscriptions;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaResourceClient;
import com.eqixiac.equinix.fabric.beta.model.AppSubscription;
import com.eqixiac.equinix.fabric.beta.model.AppSubscriptionPostRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AppSubscriptions} over the shared {@code FabricBeta} apiParams area.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public class AppSubscriptionsImpl implements AppSubscriptions {

    private final BetaClientFactory clients;

    /**
     * Creates the client.
     *
     * @param clients the shared internal client factory
     */
    public AppSubscriptionsImpl(BetaClientFactory clients) {
        this.clients = clients;
    }

    private <J> BetaResourceClient<J> client(String requestParent, Class<J> type) {
        return clients.client(requestParent, type);
    }

    @Override
    public AppSubscription create(AppSubscriptionPostRequest request) {
        return client("AppSubscriptions", AppSubscription.class).one("CreateAppSubscription", Map.of(), request);
    }

    @Override
    public AppSubscription getByUuid(String appSubscriptionId) {
        return client("AppSubscriptions", AppSubscription.class).one("GetAppSubscriptionByUuid", Map.of("appSubscriptionId", appSubscriptionId), null);
    }

    @Override
    public AppSubscription delete(String appSubscriptionId) {
        return client("AppSubscriptions", AppSubscription.class).one("DeleteAppSubscriptionByUuid", Map.of("appSubscriptionId", appSubscriptionId), null);
    }

    @Override
    public AppSubscription update(String appSubscriptionId, List<PatchOperation> operations) {
        return client("AppSubscriptions", AppSubscription.class).patch("UpdateAppSubscriptionByUuid", Map.of("appSubscriptionId", appSubscriptionId), operations, true);
    }

    @Override
    public PaginatedFilteredList<AppSubscription> search(FilterPropertyList filter, SortPropertyList sort) {
        return client("AppSubscriptions", AppSubscription.class).search("SearchAppSubscriptions", Map.of(), filter, sort);
    }

    @Override
    public PaginatedFilteredList<AppSubscription> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    @Override
    public PaginatedFilteredList<AppSubscription> search() {
        return search(null, null);
    }
}
