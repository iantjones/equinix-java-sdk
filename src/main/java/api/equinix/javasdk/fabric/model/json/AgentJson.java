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
import api.equinix.javasdk.fabric.model.Agent;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.AgentConfiguration;
import api.equinix.javasdk.fabric.model.implementation.AgentTemplateRef;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentJson {

    @Getter static TypeReference<List<AgentJson>> listTypeRef = new TypeReference<>() {};

    @Getter static TypeReference<Page<Agent, AgentJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private String state;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("agentTemplate")
    private AgentTemplateRef agentTemplate;

    @JsonProperty("configuration")
    private AgentConfiguration configuration;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
