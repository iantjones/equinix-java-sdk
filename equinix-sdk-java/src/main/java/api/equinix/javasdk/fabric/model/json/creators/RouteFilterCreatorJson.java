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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.RouteFilterAction;
import api.equinix.javasdk.fabric.enums.RouteFilterType;
import api.equinix.javasdk.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class RouteFilterCreatorJson {

    @JsonProperty("type")
    private RouteFilterType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("notMatchedRuleAction")
    private RouteFilterAction notMatchedRuleAction;

    public RouteFilterCreatorJson(RouteFilterOperator.RouteFilterBuilder routeFilterBuilder) {
        this.type = routeFilterBuilder.getType();
        this.name = routeFilterBuilder.getName();
        this.description = routeFilterBuilder.getDescription();
        this.project = routeFilterBuilder.getProject();
        this.notMatchedRuleAction = routeFilterBuilder.getNotMatchedRuleAction();
    }
}
