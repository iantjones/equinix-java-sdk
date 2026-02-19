/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.MarketplaceSubscriptions;
import api.equinix.javasdk.fabric.client.internal.MarketplaceSubscriptionClient;
import api.equinix.javasdk.fabric.model.MarketplaceSubscription;
import api.equinix.javasdk.fabric.model.json.MarketplaceSubscriptionJson;

public class MarketplaceSubscriptionsImpl implements MarketplaceSubscriptions {

    private final Fabric serviceManager;
    private final MarketplaceSubscriptionClient<MarketplaceSubscription> serviceClient;

    public MarketplaceSubscriptionsImpl(MarketplaceSubscriptionClient<MarketplaceSubscription> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<MarketplaceSubscription> list() {
        Page<MarketplaceSubscription, MarketplaceSubscriptionJson> responsePage = serviceClient.list();
        PaginatedList<MarketplaceSubscription> list = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(list, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public MarketplaceSubscription getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }
}
