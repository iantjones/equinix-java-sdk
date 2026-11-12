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

package com.eqixiac.equinix.messaging.transport;

/**
 * One message received in peek-lock mode, together with its settlement operations. Exactly one
 * settlement method should be called per delivery; implementations may throw if the lock has been
 * lost, in which case the broker redelivers the message after the lock expires.
 */
public interface EmgDelivery {

    /**
     * The raw message body.
     *
     * @return the body bytes (UTF-8 JSON for EMG)
     */
    byte[] body();

    /**
     * The broker's message id, for logging. Not a secret; never the body.
     *
     * @return the broker message id, may be {@code null}
     */
    String messageId();

    /**
     * How many times the broker has delivered this message (1 on first delivery).
     *
     * @return the delivery count
     */
    long deliveryCount();

    /** Settles the message as processed: the broker deletes it. */
    void complete();

    /** Releases the lock without processing: the broker redelivers the message. */
    void abandon();

    /**
     * Moves the message to the dead-letter sub-queue, where it is kept for inspection and never
     * redelivered.
     *
     * @param reason      short machine-readable reason
     * @param description human-readable detail (must not contain payload or secrets)
     */
    void deadLetter(String reason, String description);
}
