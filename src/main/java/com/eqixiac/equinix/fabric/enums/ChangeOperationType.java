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

package com.eqixiac.equinix.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Values of the {@code op} property of the Fabric v4 change-operation schemas
 * ({@code CloudRouterChangeOperation}, {@code RoutingProtocolChangeOperation},
 * {@code RouteFiltersChangeOperation}, {@code RouteFilterRulesChangeOperation},
 * {@code RouteAggregationsChangeOperation}, {@code RouteAggregationRulesChangeOperation},
 * {@code NetworkChangeOperation}). The wire values are lowercase JSON-Patch verbs.
 */
public enum ChangeOperationType {
    ADD("add"),
    REPLACE("replace"),
    REMOVE("remove"),
    UNKNOWN("unknown");

    private final String value;

    ChangeOperationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ChangeOperationType fromString(String value) {
        if (value != null) {
            for (ChangeOperationType op : values()) {
                if (op.value.equalsIgnoreCase(value)) {
                    return op;
                }
            }
        }
        return UNKNOWN;
    }
}
