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

package api.equinix.javasdk.messaging.client.implementation;

import api.equinix.javasdk.Messaging;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.client.Subscriptions;
import api.equinix.javasdk.messaging.client.internal.SubscriptionClient;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import api.equinix.javasdk.messaging.model.json.creators.SubscriptionOperator;
import api.equinix.javasdk.messaging.model.wrappers.SubscriptionWrapper;

public class SubscriptionsImpl implements Subscriptions {

    private final Messaging serviceManager;

    private final SubscriptionClient<Subscription> serviceClient;

    public SubscriptionsImpl(SubscriptionClient<Subscription> serviceClient, Messaging serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Subscription> list() {
        Page<Subscription, SubscriptionJson> responsePage = this.serviceClient.list();
        PaginatedList<Subscription> subscriptionList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, SubscriptionWrapper::new);
        return new PaginatedList<>(subscriptionList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Subscription getByUuid(String uuid) {
        SubscriptionJson subscriptionJson = this.serviceClient.getByUuid(uuid);
        return new SubscriptionWrapper(subscriptionJson, this.serviceClient);
    }

    public SubscriptionOperator.SubscriptionBuilder define() {
        return new SubscriptionOperator(this.serviceClient).create();
    }
}
