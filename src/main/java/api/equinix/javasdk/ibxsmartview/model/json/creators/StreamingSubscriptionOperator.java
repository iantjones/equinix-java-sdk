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

package api.equinix.javasdk.ibxsmartview.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.StreamingSubscriptionClientImpl;
import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import api.equinix.javasdk.ibxsmartview.enums.StreamingMessageType;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import api.equinix.javasdk.ibxsmartview.model.wrappers.StreamingSubscriptionWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        private String name;
        private String description;
        private ChannelType channelType;
        private Map<String, Object> channelDetails;
        private List<MessageConfig> messages;

        protected StreamingSubscriptionBuilder() {
            this.messages = new ArrayList<>();
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder withChannel(ChannelType channelType, Map<String, Object> channelDetails) {
            this.channelType = channelType;
            this.channelDetails = channelDetails;
            return this;
        }

        public StreamingSubscriptionOperator.StreamingSubscriptionBuilder addMessage(StreamingMessageType messageType, List<String> accountNumbers, List<String> ibxs) {
            this.messages.add(new MessageConfig(messageType, accountNumbers, ibxs));
            return this;
        }

        public StreamingSubscription create() {
            StreamingSubscriptionCreatorJson creatorJson = new StreamingSubscriptionCreatorJson(this);
            StreamingSubscriptionJson subscriptionJson = ((StreamingSubscriptionClientImpl) StreamingSubscriptionOperator.this.getServiceClient()).create(creatorJson);
            return new StreamingSubscriptionWrapper(subscriptionJson, StreamingSubscriptionOperator.this.getServiceClient());
        }
    }

    @Getter
    public static class MessageConfig {
        private final StreamingMessageType messageType;
        private final List<String> accountNumbers;
        private final List<String> ibxs;

        public MessageConfig(StreamingMessageType messageType, List<String> accountNumbers, List<String> ibxs) {
            this.messageType = messageType;
            this.accountNumbers = accountNumbers;
            this.ibxs = ibxs;
        }
    }
}
