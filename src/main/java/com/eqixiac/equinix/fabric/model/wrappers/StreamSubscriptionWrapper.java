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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.StreamSubscriptionClientImpl;
import com.eqixiac.equinix.fabric.model.StreamSubscription;
import com.eqixiac.equinix.fabric.model.json.StreamSubscriptionJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamSubscriptionOperator;
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

    public StreamSubscriptionOperator.StreamSubscriptionBuilder update(String streamId) {
        return new StreamSubscriptionOperator(this.serviceClient).update(streamId, this.jsonObject);
    }

    public Boolean delete(String streamId) {
        this.jsonObject = ((StreamSubscriptionClientImpl)this.serviceClient).delete(streamId, this.getUuid());
        return true;
    }

    public void refresh(String streamId) {
        this.jsonObject = ((StreamSubscriptionClientImpl)this.serviceClient).refresh(streamId, this.getUuid());
    }

    private interface StreamSubscriptionMutability {
        StreamSubscriptionOperator.StreamSubscriptionBuilder update(String streamId);
        Boolean delete(String streamId);
        void refresh(String streamId);
    }
}
