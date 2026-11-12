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

/**
 * The kind of event an IBX or network ticket notification describes, derived from the suffix of
 * {@code Task.Resource} (for example {@code DataCenter.Maintenance} or {@code Network.Incident}).
 */
public enum TicketCategory {
    /** Planned or unplanned maintenance. */
    MAINTENANCE,
    /** An incident. */
    INCIDENT,
    /** A data-center security incident ({@code DataCenter.SecurityIncident}). */
    SECURITY_INCIDENT,
    /** A data-center advisory ({@code DataCenter.Advisory}). */
    ADVISORY,
    /** Any other resource, e.g. the v2 {@code DataCenter.Network.Port} ticket form. */
    OTHER;

    /**
     * Derives the category from a {@code Task.Resource} value.
     *
     * @param resource the resource, may be {@code null}
     * @return the category, never {@code null}
     */
    public static TicketCategory fromResource(String resource) {
        String key = Wire.key(resource);
        if (key.endsWith(".securityincident")) {
            return SECURITY_INCIDENT;
        }
        if (key.endsWith(".incident")) {
            return INCIDENT;
        }
        if (key.endsWith(".maintenance")) {
            return MAINTENANCE;
        }
        if (key.endsWith(".advisory")) {
            return ADVISORY;
        }
        return OTHER;
    }
}
