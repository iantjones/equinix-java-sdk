/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.enums;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Action State
 *
 * <p>Values from the spec; {@link #UNKNOWN} is the read-side fallback for values added later.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public enum LoaActionState {

    /** Wire value {@code COMPLETED}. */
    @JsonProperty("COMPLETED")
    COMPLETED("COMPLETED"),

    /** Wire value {@code FAILED}. */
    @JsonProperty("FAILED")
    FAILED("FAILED"),

    /** Wire value {@code PENDING}. */
    @JsonProperty("PENDING")
    PENDING("PENDING"),

    /** Read-side fallback for any value this SDK version does not know. */
    @JsonEnumDefaultValue
    UNKNOWN("UNKNOWN");

    private final String value;

    LoaActionState(String value) {
        this.value = value;
    }

    /**
     * Returns the wire value of this constant.
     *
     * @return the value exactly as the API sends it
     */
    public String getValue() {
        return value;
    }
}
