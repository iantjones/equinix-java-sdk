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

/**
 * Lifecycle state of a power alert configuration (spec schema {@code AlertConfigurationReadModel},
 * property {@code state}). {@link #UNKNOWN} is a read-side fallback for values added after this SDK
 * release — never send it.
 */
public enum AlertConfigurationState implements APIParam {
    ACTIVE,
    PAUSED,
    DELETED,
    UNKNOWN;

    @JsonCreator
    public static AlertConfigurationState fromString(String value) {
        try {
            return AlertConfigurationState.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
