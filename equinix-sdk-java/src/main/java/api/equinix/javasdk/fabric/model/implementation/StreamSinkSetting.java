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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stream subscription sink settings (e.g. Splunk index, ServiceNow source, webhook format).
 */
@Getter
@Builder
@NoArgsConstructor
@lombok.AllArgsConstructor
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

    @JsonProperty("format")
    private String format;
}
