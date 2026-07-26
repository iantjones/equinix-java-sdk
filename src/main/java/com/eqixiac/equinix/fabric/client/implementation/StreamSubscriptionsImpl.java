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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.StreamSubscriptions;
import com.eqixiac.equinix.fabric.client.internal.StreamSubscriptionClient;
import com.eqixiac.equinix.fabric.model.StreamSubscription;
import com.eqixiac.equinix.fabric.model.json.StreamSubscriptionJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamSubscriptionOperator;
import com.eqixiac.equinix.fabric.model.wrappers.StreamSubscriptionWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StreamSubscriptionsImpl implements StreamSubscriptions {

    private final StreamSubscriptionClient<StreamSubscription> serviceClient;

    public PaginatedList<StreamSubscription> list(String streamId) {
        Page<StreamSubscriptionJson> responsePage = this.serviceClient.list(streamId);
        PaginatedList<StreamSubscription> streamSubscriptionList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, StreamSubscriptionWrapper::new);
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
