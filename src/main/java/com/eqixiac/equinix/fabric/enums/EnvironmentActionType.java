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

/**
 * Values of the Fabric v4 {@code EnvironmentActionTypeEnum} schema: the {@code type} property of
 * {@code EnvironmentActionRequest} and {@code EnvironmentActionResponse}. The catalog fetched
 * 2026-09-21 defines one value.
 *
 * <p><b>Beta</b>: the owning operation {@code serviceProfileEnvironmentAction}
 * ({@code POST /fabric/v4/serviceProfiles/{serviceProfileId}/environments/{environmentId}/actions})
 * carries the catalog's Beta marker.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release. Never send
 * it.</p>
 */
public enum EnvironmentActionType {
    /** Checks an activation key against a provider environment. */
    VALIDATE_ACTIVATION_KEY,
    UNKNOWN;

    @JsonCreator
    public static EnvironmentActionType fromString(String value) {
        try { return EnvironmentActionType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
