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

package com.eqixiac.equinix.iam.enums;

import com.eqixiac.equinix.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * One of the three API aspects of a service action (IAM access v1 spec schema
 * {@code ServiceAspect}). Wire values are prefixed (e.g. {@code aspect:use}), so constants map to
 * the spec spelling via {@code getValue()}. {@link #UNKNOWN} is a read-side fallback for values
 * added after this SDK release — never send it.
 */
public enum ServiceAspect implements APIParam {
    USE("aspect:use"),
    SRV("aspect:srv"),
    OPS("aspect:ops"),
    UNKNOWN("unknown");

    private final String value;

    ServiceAspect(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ServiceAspect fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ServiceAspect aspect : values()) {
            if (aspect.value.equalsIgnoreCase(value) || aspect.name().equalsIgnoreCase(value)) {
                return aspect;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return value;
    }
}
