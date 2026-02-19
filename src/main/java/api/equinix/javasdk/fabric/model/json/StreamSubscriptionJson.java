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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionState;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionType;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.StreamSink;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class StreamSubscriptionJson implements Serializable {

    @Getter static TypeReference<Page<StreamSubscription, StreamSubscriptionJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<StreamSubscriptionJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<StreamSubscriptionJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private StreamSubscriptionType type;

    @JsonProperty("state")
    private StreamSubscriptionState state;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("sink")
    private StreamSink sink;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
