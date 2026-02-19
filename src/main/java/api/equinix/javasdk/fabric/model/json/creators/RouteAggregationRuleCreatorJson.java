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
}
