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
 * Visibility of a specification environment in a provider's customer-facing APIs
 * ({@code environment.yaml#/EnvironmentVisibility}); carried by
 * {@code Environment.environmentVisibility}, output only. Constant names equal the wire values.
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation).</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release, and the
 * value {@link EnvironmentRef} reports when no visibility was supplied; never send it.</p>
 *
 * @author ianjones
 */
public enum EnvironmentVisibility {

    /**
     * Limited visibility, as jointly defined by the two providers, to an individual customer or a
     * small number of customers.
     */
    ENVIRONMENT_VISIBILITY_PRIVATE,

    /** Visible to all customers of the provider with little to no restriction. */
    ENVIRONMENT_VISIBILITY_PUBLIC,

    /** Read-side fallback for a value this enum does not list, or for an absent value. */
    UNKNOWN;

    /**
     * Parses a wire value. Surrounding whitespace is trimmed and case is ignored.
     *
     * @param value the wire value, for example {@code ENVIRONMENT_VISIBILITY_PUBLIC}; may be null
     * @return the matching constant, or {@link #UNKNOWN} when {@code value} is null, blank or not listed
     */
    @JsonCreator
    public static EnvironmentVisibility fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return EnvironmentVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
