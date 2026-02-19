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

package api.equinix.javasdk.messaging.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.messaging.client.internal.implementation.SubscriptionClientImpl;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class SubscriptionWrapper extends ResourceImpl<Subscription> implements Subscription {

    @Delegate(excludes = SubscriptionMutability.class)
    private SubscriptionJson jsonObject;
    @Getter
    private final Pageable<Subscription> serviceClient;

    public SubscriptionWrapper(SubscriptionJson subscriptionJson, Pageable<Subscription> serviceClient) {
        this.jsonObject = subscriptionJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((SubscriptionClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((SubscriptionClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface SubscriptionMutability {
        Boolean delete();
        void refresh();
    }
}
