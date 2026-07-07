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

package api.equinix.javasdk.fabric.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Webhook message format (the Fabric v4 {@code StreamSubscriptionSinkSetting.format} attribute:
 * {@code CLOUDEVENT} or {@code OPENTELEMETRY}; the API default is {@code CLOUDEVENT}).
 * {@link #UNKNOWN} is a read-side fallback for values added after this SDK release &mdash; never send it.
 *
 * @author ianjones
 */
public enum StreamSubscriptionSinkFormat {
    CLOUDEVENT,
    OPENTELEMETRY,
    UNKNOWN;

    /**
     * Deserializes a sink format leniently: an unrecognized value maps to {@link #UNKNOWN}
     * instead of failing the enclosing response.
     *
     * @param value the raw API value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static StreamSubscriptionSinkFormat fromString(String value) {
        try { return StreamSubscriptionSinkFormat.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}
