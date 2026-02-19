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
import api.equinix.javasdk.fabric.enums.RouteAggregationState;
import api.equinix.javasdk.fabric.enums.RouteAggregationType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class RouteAggregationJson implements Serializable {

    @Getter static TypeReference<Page<RouteAggregation, RouteAggregationJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<RouteAggregationJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<RouteAggregationJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private RouteAggregationType type;

    @JsonProperty("state")
    private RouteAggregationState state;

    @JsonProperty("description")
    private String description;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("change")
    private Change change;

    @JsonProperty("rulesCount")
    private Integer rulesCount;

    @JsonProperty("connectionsCount")
    private Integer connectionsCount;
}
