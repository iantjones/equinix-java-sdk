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
 * Values of the Fabric v4 {@code EnvironmentActionStateEnum} schema: the {@code state} property of
 * {@code EnvironmentActionResponse}. The catalog describes the enum as
 * "ACTIVE - already used , INACTIVE - not used".
 *
 * <p>Per that description the state reports usage of the activation key, not progress of the
 * action: {@link #INACTIVE} is returned for a key that has not been used and {@link #ACTIVE} for
 * a key that has. The catalog does not define which event marks a key as used, and publishes no
 * response example.</p>
 *
 * <p><b>Beta</b>: {@code EnvironmentActionResponse} carries the catalog's Beta marker.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release.</p>
 */
public enum EnvironmentActionState {
    /** The activation key has already been used. */
    ACTIVE,
    /** The activation key has not been used. */
    INACTIVE,
    UNKNOWN;

    @JsonCreator
    public static EnvironmentActionState fromString(String value) {
        try { return EnvironmentActionState.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
