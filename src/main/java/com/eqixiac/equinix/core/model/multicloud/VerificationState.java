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

package com.eqixiac.equinix.core.model.multicloud;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Verification state of a specification connection ({@code common.yaml#/VerificationState});
 * carried by {@code Connection.verificationState}, output only. Constant names equal the wire
 * values.
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation). The nearest Fabric v4 analogue is the per-side status pair on a connection
 * operation ({@code providerStatus} / {@code equinixStatus}); the two are not interchangeable.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release; never send
 * it.</p>
 *
 * @author ianjones
 */
public enum VerificationState {

    /** Verification is pending; the connection is not ready for production traffic. */
    VERIFICATION_STATE_UNVERIFIED,

    /** Verification is complete; the connection is ready for production traffic. */
    VERIFICATION_STATE_VERIFIED,

    /** Read-side fallback for a value this enum does not list. */
    UNKNOWN;

    /**
     * Parses a wire value. Surrounding whitespace is trimmed and case is ignored.
     *
     * @param value the wire value, for example {@code VERIFICATION_STATE_VERIFIED}; may be null
     * @return the matching constant, or {@link #UNKNOWN} when {@code value} is null, blank or not listed
     */
    @JsonCreator
    public static VerificationState fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return VerificationState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
