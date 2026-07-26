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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionState;
import com.eqixiac.equinix.fabric.enums.StreamSubscriptionType;
import com.eqixiac.equinix.fabric.model.StreamSubscription;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.StreamSink;
import com.eqixiac.equinix.fabric.model.implementation.StreamSubscriptionOperation;
import com.eqixiac.equinix.fabric.model.implementation.StreamSubscriptionSelector;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamSubscriptionJson {

    @Getter static TypeReference<List<StreamSubscriptionJson>> listTypeRef = new TypeReference<>() {};

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

    @JsonProperty("metricSelector")
    private StreamSubscriptionSelector metricSelector;

    @JsonProperty("eventSelector")
    private StreamSubscriptionSelector eventSelector;

    @JsonProperty("sink")
    private StreamSink sink;

    @JsonProperty("operation")
    private StreamSubscriptionOperation operation;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
