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

import api.equinix.javasdk.ibxsmartview.model.implementation.Channel;
import api.equinix.javasdk.ibxsmartview.model.implementation.MessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed request body for creating/updating a streaming subscription
 * ({@code SubscriptionRequest} in the spec). Carries the typed {@link MessageType} and
 * the single delivery {@link Channel}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamingSubscriptionCreatorJson {

    @JsonProperty("messageType")
    private final MessageType messageType;

    @JsonProperty("channel")
    private final Channel channel;

    public StreamingSubscriptionCreatorJson(StreamingSubscriptionOperator.StreamingSubscriptionBuilder builder) {
        this.messageType = builder.getMessageType();
        this.channel = builder.getChannel();
    }
}
