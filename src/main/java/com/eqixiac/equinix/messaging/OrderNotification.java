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
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Order status notification for orders placed through the Equinix portal, the APIs or EMG
 * (Cross Connect, Smart Hands, Work Visit, Shipment, Trouble Ticket, ...): {@code Task.Resource} one
 * of the spec's order product groups ({@code SmartHands}, {@code Shipping}, {@code WorkVisit}, ...).
 *
 * <p>These inbound <em>Order Notifications</em> remain supported. They are distinct from EMG
 * <em>Order Management</em> (placing orders through EMG), which Equinix deprecated on 2024-09-30
 * and which this SDK does not implement.
 *
 * @param header  common header
 * @param body    the order status details
 * @param rawJson the envelope as received
 */
public record OrderNotification(EmgHeader header, Body body, String rawJson) implements EmgMessage {

    /**
     * Order notification body (spec {@code OrderNotificationBody}).
     *
     * @param requestorId     customer reference number given when the order was placed
     * @param servicerId      the Equinix order number
     * @param activityId      id of the order line item or sub-task
     * @param state           order state
     * @param description     status description or technician message
     * @param customerContact primary customer contact email
     * @param location        IBX where the order is fulfilled
     * @param serviceDetails  order-type-specific details (free-form; see {@link #serviceDetails()})
     * @param attachments     attachments, never {@code null}
     */
    public record Body(String requestorId, String servicerId, String activityId, State state,
                       String description, String customerContact, String location,
                       JsonNode serviceDetails, List<Attachment> attachments) {

        /** Defensively copies the mutable JSON tree and normalises {@code attachments}. */
        public Body {
            serviceDetails = serviceDetails == null || serviceDetails.isNull() ? null : serviceDetails.deepCopy();
            attachments = Wire.copy(attachments);
        }

        /**
         * Order-type-specific details. Their contents vary by order type and are not specified, so
         * they are exposed as a JSON tree; a defensive copy is returned.
         *
         * @return a copy of the details, or {@code null} if absent
         */
        @Override
        public JsonNode serviceDetails() {
            return serviceDetails == null ? null : serviceDetails.deepCopy();
        }
    }

    /**
     * An order attachment.
     *
     * @param id   attachment id
     * @param name file name
     * @param url  download URL, when available
     */
    public record Attachment(String id, String name, String url) {
    }

    /** Order state. */
    public enum State {
        /** Received and under review (wire {@code Open}). */
        OPEN,
        /** Work in progress (wire {@code InProgress}). */
        IN_PROGRESS,
        /** Completed (wire {@code Completed}). */
        COMPLETED,
        /** Cancelled (wire {@code Cancelled}). */
        CANCELLED,
        /** Equinix needs customer input or approval (wire {@code Pending Customer Input}). */
        PENDING_CUSTOMER_INPUT,
        /** Temporarily paused (wire {@code On Hold}). */
        ON_HOLD,
        /** A value this SDK version does not recognise. */
        UNKNOWN;

        /**
         * Tolerant lookup of a wire value.
         *
         * @param value the wire value
         * @return the constant, {@link #UNKNOWN} if unrecognised, {@code null} if {@code value} is null
         */
        @JsonCreator
        public static State fromWire(String value) {
            return Wire.lookup(State.class, value, UNKNOWN);
        }
    }
}
