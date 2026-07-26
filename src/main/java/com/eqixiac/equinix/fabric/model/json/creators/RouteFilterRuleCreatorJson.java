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

import com.eqixiac.equinix.fabric.enums.RouteFilterAction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class RouteFilterRuleCreatorJson {

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("action")
    private RouteFilterAction action;

    @JsonProperty("prefixMatch")
    private String prefixMatch;

    public RouteFilterRuleCreatorJson(RouteFilterRuleOperator.RouteFilterRuleBuilder routeFilterRuleBuilder) {
        this.prefix = routeFilterRuleBuilder.getPrefix();
        this.name = routeFilterRuleBuilder.getName();
        this.description = routeFilterRuleBuilder.getDescription();
        this.action = routeFilterRuleBuilder.getAction();
        this.prefixMatch = routeFilterRuleBuilder.getPrefixMatch();
    }

    /**
     * Constructs a route filter rule configuration body directly, for use with the bulk-create and
     * replace endpoints (which accept rule bodies outside the single-rule fluent builder flow).
     *
     * @param prefix the route prefix the rule matches
     * @param name the rule name
     * @param description the rule description
     * @param action the action to take on a match
     * @param prefixMatch the prefix match strategy (e.g. {@code exact}, {@code orlonger})
     */
    public RouteFilterRuleCreatorJson(String prefix, String name, String description,
                                      RouteFilterAction action, String prefixMatch) {
        this.prefix = prefix;
        this.name = name;
        this.description = description;
        this.action = action;
        this.prefixMatch = prefixMatch;
    }
}
