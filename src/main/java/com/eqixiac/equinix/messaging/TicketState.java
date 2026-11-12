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
 * Lifecycle state of an IBX or network maintenance/incident ticket ({@code Body.State}).
 */
public enum TicketState {
    /** The maintenance or incident is confirmed (wire {@code CONFIRMED}). */
    CONFIRMED,
    /** The maintenance or incident is in progress (wire {@code IN-PROCESS}). */
    IN_PROCESS,
    /** The maintenance or incident is completed (wire {@code COMPLETED}). */
    COMPLETED,
    /** The maintenance or incident was cancelled (wire {@code CANCELLED}). */
    CANCELLED,
    /** A value this SDK version does not recognise. */
    UNKNOWN;

    /**
     * Case- and punctuation-insensitive lookup of a wire value.
     *
     * @param value the wire value
     * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
     */
    @JsonCreator
    public static TicketState fromWire(String value) {
        return Wire.lookup(TicketState.class, value, UNKNOWN);
    }
}
