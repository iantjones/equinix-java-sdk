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
 * Equinix Managed Services trouble-ticket or service-request notification: {@code Task.Resource}
 * {@code TroubleTicket.ManagedService} or {@code ServiceRequest.ManagedService} (spec
 * {@code ManagedServicesBody}).
 *
 * @param header  common header
 * @param body    the ticket/request details
 * @param rawJson the envelope as received
 */
public record ManagedServiceNotification(EmgHeader header, Body body, String rawJson) implements EmgMessage {

    /**
     * Managed Services notification body.
     *
     * @param accountNumber customer account number
     * @param resource      {@code TroubleTicket.ManagedService} or {@code ServiceRequest.ManagedService}
     * @param verb          the action performed
     * @param message       the ticket/request details
     */
    public record Body(String accountNumber, String resource, EmgVerb verb, Message message) {
    }

    /**
     * Ticket/request details.
     *
     * @param requestorId          requestor id (wire {@code RequestorID})
     * @param customerContact      customer contact email
     * @param description          detailed description
     * @param shortDescription     brief description
     * @param impact               impact level
     * @param urgency              urgency level
     * @param supplierTicketNumber external supplier ticket reference
     */
    public record Message(String requestorId, String customerContact, String description,
                          String shortDescription, Level impact, Level urgency, String supplierTicketNumber) {
    }

    /** Impact or urgency level. */
    public enum Level {
        /** Low. */
        LOW,
        /** Medium. */
        MEDIUM,
        /** High. */
        HIGH,
        /** Critical. */
        CRITICAL,
        /** A value this SDK version does not recognise. */
        UNKNOWN;

        /**
         * Tolerant lookup of a wire value.
         *
         * @param value the wire value
         * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
         */
        @JsonCreator
        public static Level fromWire(String value) {
            return Wire.lookup(Level.class, value, UNKNOWN);
        }
    }
}
