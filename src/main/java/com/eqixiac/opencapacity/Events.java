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

package com.eqixiac.opencapacity;

import com.eqixiac.opencapacity.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Objects;

/** Reads the events that a provider delivers to a webhook or a stream. */
public final class Events {

    private Events() {
    }

    /**
     * Reads one event from its JSON form. Verify the signature of a webhook delivery with
     * {@link WebhookSignature#verify} before you call this method.
     *
     * @param json the event as the provider sent it
     * @return the event
     * @throws OpenCapacityException if the text is not an event
     */
    public static Event parse(String json) {
        Objects.requireNonNull(json, "json");
        try {
            Event event = Transport.MAPPER.readValue(json, Event.class);
            if (event == null || event.type() == null) {
                throw new OpenCapacityException("The text is not an OpenCapacity event: it has no type.", 0, null);
            }
            return event;
        } catch (JsonProcessingException unreadable) {
            throw new OpenCapacityException(
                    "The text is not an OpenCapacity event: " + unreadable.getOriginalMessage(), unreadable);
        }
    }

    /**
     * Returns the body your webhook endpoint sends, with status 200, in response to a verification
     * event.
     *
     * @param verification an event of type {@link Event#SUBSCRIPTION_VERIFICATION}
     * @return the JSON body that echoes the challenge
     * @throws IllegalArgumentException if the event is not a verification event
     */
    public static String challengeResponse(Event verification) {
        if (!Event.SUBSCRIPTION_VERIFICATION.equals(verification.type())
                || verification.data() == null || verification.data().challenge() == null) {
            throw new IllegalArgumentException("Not a verification event: " + verification.type());
        }
        try {
            return Transport.MAPPER.writeValueAsString(Map.of("challenge", verification.data().challenge()));
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
