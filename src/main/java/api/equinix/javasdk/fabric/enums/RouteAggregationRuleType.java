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

package api.equinix.javasdk.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Values of the {@code type} property of the Fabric v4 {@code RouteAggregationRulesData} schema.
 */
public enum RouteAggregationRuleType {
    BGP_IPv4_PREFIX_AGGREGATION_RULE,
    UNKNOWN;

    @JsonCreator
    public static RouteAggregationRuleType fromString(String value) {
        try { return RouteAggregationRuleType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
