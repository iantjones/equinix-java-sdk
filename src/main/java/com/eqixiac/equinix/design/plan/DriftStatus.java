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


package com.eqixiac.equinix.design.plan;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/** How a planned resource compares with what is live. */
public enum DriftStatus {
    /** Not found live: reconcile would create it. */
    MISSING,
    /** Found live and every compared field matches. */
    PRESENT_MATCHING,
    /** Found live with differing fields (listed with the drift). */
    PRESENT_DIFFERENT,
    /** Its live counterpart could not be read (see the diff's notes); neither present nor missing is known. */
    UNREADABLE,
    /** The element is not created through the API (an Interconnect); not compared. */
    NOT_EXECUTABLE,
    /** Read-side fallback for a value this release does not know. */
    @JsonEnumDefaultValue
    UNKNOWN
}
