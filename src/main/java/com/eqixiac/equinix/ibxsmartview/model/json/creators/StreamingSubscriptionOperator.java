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

package com.eqixiac.equinix.ibxsmartview.model.json.creators;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.StreamingSubscriptionClientImpl;
import com.eqixiac.equinix.ibxsmartview.model.StreamingSubscription;
import com.eqixiac.equinix.ibxsmartview.model.implementation.Channel;
import com.eqixiac.equinix.ibxsmartview.model.implementation.MessageType;
import com.eqixiac.equinix.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.eqixiac.equinix.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;
import lombok.Getter;

public class StreamingSubscriptionOperator extends ResourceImpl<StreamingSubscription> {

    @Getter
    private final Pageable<StreamingSubscription> serviceClient;

    public StreamingSubscriptionOperator(Pageable<StreamingSubscription> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public StreamingSubscriptionBuilder create() {
        return new StreamingSubscriptionBuilder();
    }

    @Getter
    public class StreamingSubscriptionBuilder {

        private MessageType messageType;
        private Channel channel;

        protected StreamingSubscriptionBuilder() {
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder withMessageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder withChannel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public StreamingSubscription create() {
            StreamingSubscriptionCreatorJson creatorJson = new StreamingSubscriptionCreatorJson(this);
            StreamingSubscriptionJson subscriptionJson = ((StreamingSubscriptionClientImpl) StreamingSubscriptionOperator.this.getServiceClient()).create(creatorJson);
            return new StreamingSubscriptionWrapper(subscriptionJson, StreamingSubscriptionOperator.this.getServiceClient());
        }
    }
}
