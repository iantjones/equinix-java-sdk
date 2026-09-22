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
 * Provisioning state of a specification feature ({@code common.yaml#/ProvisioningState}); carried
 * by {@code Feature.provisioningState}. Constant names equal the wire values.
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation). Features are negotiated provider-to-provider; a customer does not create or
 * read them directly. The nearest Fabric v4 analogue is {@code RoutingProtocolState}.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release; never send
 * it.</p>
 *
 * @author ianjones
 */
public enum ProvisioningState {

    /** The feature is created and its parameters are reserved. */
    PROVISIONING_STATE_PENDING,

    /**
     * Configuration has propagated to the provider's infrastructure; the customer may send
     * production traffic between providers using the feature.
     */
    PROVISIONING_STATE_FINAL,

    /** Read-side fallback for a value this enum does not list. */
    UNKNOWN;

    /**
     * Parses a wire value. Surrounding whitespace is trimmed and case is ignored.
     *
     * @param value the wire value, for example {@code PROVISIONING_STATE_FINAL}; may be null
     * @return the matching constant, or {@link #UNKNOWN} when {@code value} is null, blank or not listed
     */
    @JsonCreator
    public static ProvisioningState fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return ProvisioningState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
