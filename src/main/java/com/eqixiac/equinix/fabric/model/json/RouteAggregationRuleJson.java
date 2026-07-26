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
import com.eqixiac.equinix.fabric.enums.RouteAggregationRuleType;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.enums.RouteAggregationRuleState;
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteAggregationRuleJson {

    @Getter static TypeReference<List<RouteAggregationRuleJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private RouteAggregationRuleType type;

    @JsonProperty("state")
    private RouteAggregationRuleState state;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("description")
    private String description;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("change")
    private Change change;
}
