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
 * Values of the Fabric v4 {@code InternetAccessBgpAuthType} schema (spec 4.34.0): whether a BGP
 * authentication key is configured on an EIA routing protocol. It replaces reading the
 * spec-deprecated response property {@code bgpAuthKey}.
 */
public enum EiaBgpAuthType {
    /** A BGP authentication key is configured. */
    AUTH_KEY,
    /** No BGP authentication key is configured (the spec default). */
    NONE,
    /** Read-side fallback for values added after this release. Never send it. */
    UNKNOWN;

    /**
     * Maps a wire value, falling back to {@link #UNKNOWN}.
     *
     * @param value the wire value
     * @return the constant
     */
    @JsonCreator
    public static EiaBgpAuthType fromString(String value) {
        try { return EiaBgpAuthType.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
