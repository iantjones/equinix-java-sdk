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

package api.equinix.javasdk.messaging.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.messaging.client.internal.implementation.SubscriptionClientImpl;
import api.equinix.javasdk.messaging.enums.SubscriptionType;
import api.equinix.javasdk.messaging.model.Subscription;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import api.equinix.javasdk.messaging.model.wrappers.SubscriptionWrapper;
import lombok.Getter;

import java.util.List;

public class SubscriptionOperator extends ResourceImpl<Subscription> {

    @Getter
    private final Pageable<Subscription> serviceClient;

    public SubscriptionOperator(Pageable<Subscription> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public SubscriptionBuilder create() {
        return new SubscriptionBuilder();
    }

    @Getter
    public class SubscriptionBuilder {
        private String name;
        private SubscriptionType type;
        private List<String> eventTypes;

        public SubscriptionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SubscriptionBuilder type(SubscriptionType type) {
            this.type = type;
            return this;
        }

        public SubscriptionBuilder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        public Subscription create() {
            SubscriptionCreatorJson creatorJson = new SubscriptionCreatorJson(this);
            SubscriptionJson subscriptionJson = ((SubscriptionClientImpl) SubscriptionOperator.this.getServiceClient()).create(creatorJson);
            return new SubscriptionWrapper(subscriptionJson, SubscriptionOperator.this.getServiceClient());
        }
    }
}
