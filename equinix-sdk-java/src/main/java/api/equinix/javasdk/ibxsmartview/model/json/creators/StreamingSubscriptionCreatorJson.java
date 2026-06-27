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

import api.equinix.javasdk.ibxsmartview.enums.StreamingMessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Setter(AccessLevel.PRIVATE)
public class StreamingSubscriptionCreatorJson {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("channel")
    private ChannelCreator channel;

    @JsonProperty("messages")
    private List<MessageCreator> messages;

    public StreamingSubscriptionCreatorJson(StreamingSubscriptionOperator.StreamingSubscriptionBuilder builder) {
        this.name = builder.getName();
        this.description = builder.getDescription();
        this.channel = new ChannelCreator(builder.getChannelType(), builder.getChannelDetails());
        this.messages = builder.getMessages().stream()
                .map(m -> new MessageCreator(m.getMessageType(), m.getAccountNumbers(), m.getIbxs()))
                .collect(Collectors.toList());
    }

    @Getter
    @Setter(AccessLevel.PRIVATE)
    static class ChannelCreator {

        @JsonProperty("channelType")
        private Object channelType;

        @JsonProperty("channelDetails")
        private Map<String, Object> channelDetails;

        ChannelCreator(Object channelType, Map<String, Object> channelDetails) {
            this.channelType = channelType;
            this.channelDetails = channelDetails;
        }
    }

    @Getter
    @Setter(AccessLevel.PRIVATE)
    static class MessageCreator {

        @JsonProperty("messageType")
        private StreamingMessageType messageType;

        @JsonProperty("accountNumbers")
        private List<String> accountNumbers;

        @JsonProperty("ibxs")
        private List<String> ibxs;

        MessageCreator(StreamingMessageType messageType, List<String> accountNumbers, List<String> ibxs) {
            this.messageType = messageType;
            this.accountNumbers = accountNumbers;
            this.ibxs = ibxs;
        }
    }
}
