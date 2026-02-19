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
import api.equinix.javasdk.fabric.client.StreamSubscriptions;
import api.equinix.javasdk.fabric.client.internal.StreamSubscriptionClient;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.StreamSubscriptionJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionOperator;
import api.equinix.javasdk.fabric.model.wrappers.StreamSubscriptionWrapper;

public class StreamSubscriptionsImpl implements StreamSubscriptions {

    private final Fabric serviceManager;

    private final StreamSubscriptionClient<StreamSubscription> serviceClient;

    public StreamSubscriptionsImpl(StreamSubscriptionClient<StreamSubscription> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<StreamSubscription> list(String streamId) {
        Page<StreamSubscription, StreamSubscriptionJson> responsePage = this.serviceClient.list(streamId);
        PaginatedList<StreamSubscription> streamSubscriptionList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, StreamSubscriptionWrapper::new);
        return new PaginatedList<>(streamSubscriptionList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public StreamSubscription getByUuid(String streamId, String uuid) {
        StreamSubscriptionJson streamSubscriptionJson = this.serviceClient.getByUuid(streamId, uuid);
        return new StreamSubscriptionWrapper(streamSubscriptionJson, this.serviceClient);
    }

    public StreamSubscriptionOperator.StreamSubscriptionBuilder define(String streamId) {
        return new StreamSubscriptionOperator(this.serviceClient).create(streamId);
    }
}
