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

import java.util.List;

/**
 * Billing notification: sent when an order is closed and Equinix starts billing (resellers also
 * receive it for their customers' orders, since R2024.7). Shape: the spec's
 * {@code BillingNotificationBody}; the spec routes {@code Task.Resource} {@code CROSSCONNECT} /
 * {@code CrossConnect} here, and the parser also routes any order-family resource whose body carries
 * {@code LineDetails} (the documented sample uses the misspelt resource {@code CrossConnnect}).
 *
 * @param header  common header
 * @param body    the billing details
 * @param rawJson the envelope as received
 */
public record BillingNotification(EmgHeader header, Body body, String rawJson) implements EmgMessage {

    /**
     * Billing notification body.
     *
     * @param requestorId customer reference number (if any)
     * @param servicerId  the Equinix order number
     * @param state       order state; the only documented value is {@code Closed}
     * @param description summary from the billing system
     * @param lineDetails billed line details, never {@code null}
     */
    public record Body(String requestorId, String servicerId, String state, String description,
                       List<LineDetail> lineDetails) {

        /** Normalises {@code lineDetails} to an unmodifiable list without {@code null} elements. */
        public Body {
            lineDetails = Wire.copy(lineDetails);
        }
    }

    /**
     * One billed line.
     *
     * @param id          unique billing line id
     * @param ibx         IBX (data center) code
     * @param status      line status; the only documented value is {@code Billed}
     * @param billedLines billed line item ids, never {@code null}
     */
    public record LineDetail(String id, String ibx, String status, List<String> billedLines) {

        /** Normalises {@code billedLines} to an unmodifiable list without {@code null} elements. */
        public LineDetail {
            billedLines = Wire.copy(billedLines);
        }
    }
}
