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

package com.eqixiac.equinix.ibxsmartview.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Power-hierarchy level type used by the DCIM power APIs. The wire form is lower case
 * ({@code ibx}, {@code cage}, {@code cabinet}, {@code circuit}) per the smartviewv2 spec.
 */
public enum PowerLevelType implements APIParam {
    IBX("ibx"),
    CAGE("cage"),
    CABINET("cabinet"),
    CIRCUIT("circuit"),
    UNKNOWN("unknown");

    private final String value;

    PowerLevelType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PowerLevelType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (PowerLevelType levelType : values()) {
            if (levelType.value.equalsIgnoreCase(value) || levelType.name().equalsIgnoreCase(value)) {
                return levelType;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
