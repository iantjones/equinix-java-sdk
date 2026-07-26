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

import com.eqixiac.equinix.fabric.model.AgentActivity;
import com.eqixiac.equinix.fabric.model.implementation.AgentActivityMetadata;
import com.eqixiac.equinix.fabric.model.implementation.AgentRef;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AgentActivityJson implements AgentActivity {

    @Getter static TypeReference<List<AgentActivityJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("agent")
    private AgentRef agent;

    @JsonProperty("status")
    private String status;

    @JsonProperty("metadata")
    private AgentActivityMetadata metadata;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
