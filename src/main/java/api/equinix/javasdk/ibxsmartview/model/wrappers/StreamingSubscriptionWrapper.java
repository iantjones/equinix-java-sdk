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

package api.equinix.javasdk.ibxsmartview.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.StreamingSubscriptionClientImpl;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class StreamingSubscriptionWrapper extends ResourceImpl<StreamingSubscription> implements StreamingSubscription {

    @Delegate(excludes = StreamingSubscriptionMutability.class)
    private StreamingSubscriptionJson jsonObject;
    @Getter
    private final Pageable<StreamingSubscription> serviceClient;

    public StreamingSubscriptionWrapper(StreamingSubscriptionJson streamingSubscriptionJson, Pageable<StreamingSubscription> serviceClient) {
        this.jsonObject = streamingSubscriptionJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((StreamingSubscriptionClientImpl)this.serviceClient).delete(this.getSubscriptionId());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((StreamingSubscriptionClientImpl)this.serviceClient).refresh(this.getSubscriptionId());
    }

    private interface StreamingSubscriptionMutability {
        Boolean delete();
        void refresh();
    }
}
