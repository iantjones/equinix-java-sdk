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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.StreamSubscriptionClientImpl;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.StreamSubscriptionJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class StreamSubscriptionWrapper extends ResourceImpl<StreamSubscription> implements StreamSubscription {

    @Delegate(excludes = StreamSubscriptionMutability.class)
    private StreamSubscriptionJson jsonObject;
    @Getter
    private final Pageable<StreamSubscription> serviceClient;

    public StreamSubscriptionWrapper(StreamSubscriptionJson streamSubscriptionJson, Pageable<StreamSubscription> serviceClient) {
        this.jsonObject = streamSubscriptionJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete(String streamId) {
        this.jsonObject = ((StreamSubscriptionClientImpl)this.serviceClient).delete(streamId, this.getUuid());
        return true;
    }

    public void refresh(String streamId) {
        this.jsonObject = ((StreamSubscriptionClientImpl)this.serviceClient).refresh(streamId, this.getUuid());
    }

    private interface StreamSubscriptionMutability {
        Boolean delete(String streamId);
        void refresh(String streamId);
    }
}
