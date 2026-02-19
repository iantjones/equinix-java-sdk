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

package api.equinix.javasdk.ibxsmartview.model.json;

import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.implementation.Channel;
import api.equinix.javasdk.ibxsmartview.model.implementation.SubscriptionMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class StreamingSubscriptionJson implements Serializable {

    @Getter static TypeReference<List<StreamingSubscriptionJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<StreamingSubscriptionJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("subscriptionId")
    private String subscriptionId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("messages")
    private List<SubscriptionMessage> messages;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("updatedDate")
    private String updatedDate;
}
