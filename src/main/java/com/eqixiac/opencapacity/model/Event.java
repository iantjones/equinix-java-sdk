/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * An event of a subscription, as a CloudEvents 1.0 envelope. Delivery is at least once:
 * de-duplicate on {@code id} and order by {@code data().sequence()}.
 *
 * @param specversion the CloudEvents version, {@code 1.0}
 * @param id the identifier, unique per event
 * @param source the provider's API base URL
 * @param type the event type, one of the constants of this class
 * @param subject the subscription identifier
 * @param time when the event occurred
 * @param datacontenttype the content type of {@code data}
 * @param data the payload
 */
public record Event(
        String specversion, String id, String source, String type, String subject, Instant time,
        String datacontenttype, EventData data) {

    /** The answer to the subscribed requirement changed or was refreshed. */
    public static final String ANSWER_UPDATED = "org.opencapacity.answer.updated";
    /** The signal a subscription follows changed state. */
    public static final String SIGNAL_UPDATED = "org.opencapacity.signal.updated";
    /** The subscription changed state. */
    public static final String SUBSCRIPTION_UPDATED = "org.opencapacity.subscription.updated";
    /** The provider asks a webhook endpoint to echo a challenge. */
    public static final String SUBSCRIPTION_VERIFICATION = "org.opencapacity.subscription.verification";

    /**
     * Says whether the event carries an answer.
     *
     * @return {@code true} for {@link #ANSWER_UPDATED}
     */
    @JsonIgnore
    public boolean isAnswerUpdate() {
        return ANSWER_UPDATED.equals(type);
    }

    /**
     * Returns the position of the event within its subscription.
     *
     * @return the sequence, or 0 if the event has none
     */
    public long sequence() {
        return data == null || data.sequence() == null ? 0 : data.sequence();
    }
}
