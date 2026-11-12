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

package com.eqixiac.equinix.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether an IBX or network maintenance/incident affects the customer's services
 * ({@code Body.Impact}; introduced in EMG release R2024.1, replacing the generic "OUTAGE" wording).
 */
public enum ServiceImpact {
    /** The activity or incident affects the customer's services. */
    SERVICE_IMPACTING,
    /** The activity or incident does not disrupt the customer's services (e.g. redundancy holds). */
    NON_SERVICE_IMPACTING,
    /** A value this SDK version does not recognise. */
    UNKNOWN;

    /**
     * Case- and punctuation-insensitive lookup of a wire value.
     *
     * @param value the wire value
     * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
     */
    @JsonCreator
    public static ServiceImpact fromWire(String value) {
        return Wire.lookup(ServiceImpact.class, value, UNKNOWN);
    }
}
