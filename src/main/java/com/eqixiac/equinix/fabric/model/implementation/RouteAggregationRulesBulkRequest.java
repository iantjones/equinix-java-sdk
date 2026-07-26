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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for creating route aggregation rules in bulk
 * ({@code POST /routeAggregations/{routeAggregationId}/routeAggregationRules/bulk}). Wraps the rule
 * configurations in the {@code data} array expected by the API.
 */
@Getter
public class RouteAggregationRulesBulkRequest {

    @JsonProperty("data")
    private final List<RouteAggregationRuleCreatorJson> data;

    public RouteAggregationRulesBulkRequest(List<RouteAggregationRuleCreatorJson> data) {
        this.data = data;
    }
}
