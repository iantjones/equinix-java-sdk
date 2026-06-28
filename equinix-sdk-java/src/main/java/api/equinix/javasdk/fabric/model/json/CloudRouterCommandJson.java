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
import api.equinix.javasdk.fabric.enums.CloudRouterCommandState;
import api.equinix.javasdk.fabric.enums.CloudRouterCommandType;
import api.equinix.javasdk.fabric.model.CloudRouterCommand;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public final class CloudRouterCommandJson implements CloudRouterCommand {

    @Getter static TypeReference<List<CloudRouterCommandJson>> listTypeRef = new TypeReference<>() {};

    @Getter static TypeReference<Page<CloudRouterCommand, CloudRouterCommandJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private CloudRouterCommandType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private CloudRouterCommandState state;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("request")
    private CloudRouterCommandRequest request;

    @JsonProperty("response")
    private Map<String, Object> response;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
