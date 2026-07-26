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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class RouteAggregationRuleCreatorJson {

    @JsonProperty("name")
    private String name;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("description")
    private String description;

    public RouteAggregationRuleCreatorJson(RouteAggregationRuleOperator.RouteAggregationRuleBuilder routeAggregationRuleBuilder) {
        this.name = routeAggregationRuleBuilder.getName();
        this.prefix = routeAggregationRuleBuilder.getPrefix();
        this.description = routeAggregationRuleBuilder.getDescription();
    }

    /**
     * Constructs a route aggregation rule configuration body directly, for use with the bulk-create
     * and replace endpoints (which accept rule bodies outside the single-rule fluent builder flow).
     *
     * @param name the rule name
     * @param prefix the route prefix the rule aggregates
     * @param description the rule description
     */
    public RouteAggregationRuleCreatorJson(String name, String prefix, String description) {
        this.name = name;
        this.prefix = prefix;
        this.description = description;
    }
}
