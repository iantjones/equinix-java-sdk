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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stream subscription sink settings (e.g. Splunk index, ServiceNow source, webhook format).
 *
 * <p>Prefer {@code builder()} over the positional constructor — six of the seven parameters
 * are {@code String}s, so builder construction is self-documenting and transposition-proof.</p>
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamSinkSetting {

    @JsonProperty("eventIndex")
    private String eventIndex;

    @JsonProperty("metricIndex")
    private String metricIndex;

    @JsonProperty("source")
    private String source;

    @JsonProperty("applicationKey")
    private String applicationKey;

    @JsonProperty("eventUri")
    private String eventUri;

    @JsonProperty("metricUri")
    private String metricUri;

    /** Webhook message format (spec {@code StreamSubscriptionSinkSetting.format}: {@code CLOUDEVENT} or {@code OPENTELEMETRY}). */
    @JsonProperty("format")
    private StreamSubscriptionSinkFormat format;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (six same-typed {@code String} parameters)
     * rather than by field declaration order.
     *
     * @param eventIndex     the Splunk event index
     * @param metricIndex    the Splunk metric index
     * @param source         the ServiceNow source
     * @param applicationKey the application key
     * @param eventUri       the event URI
     * @param metricUri      the metric URI
     * @param format         the webhook message format ({@code CLOUDEVENT} or {@code OPENTELEMETRY})
     */
    @Builder
    public StreamSinkSetting(String eventIndex, String metricIndex, String source, String applicationKey,
                             String eventUri, String metricUri, StreamSubscriptionSinkFormat format) {
        this.eventIndex = eventIndex;
        this.metricIndex = metricIndex;
        this.source = source;
        this.applicationKey = applicationKey;
        this.eventUri = eventUri;
        this.metricUri = metricUri;
        this.format = format;
    }
}
