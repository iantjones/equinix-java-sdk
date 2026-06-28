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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines the condition that triggers a power alert. Used both when reading an existing power
 * alert configuration and when supplying the condition for a create or update request.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertCondition {

    @JsonProperty("conditionType")
    private String conditionType;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("threshold")
    private PowerAlertThreshold threshold;

    /**
     * Creates a power alert condition.
     *
     * @param conditionType the type of condition that triggers the alert (e.g. {@code EXCEEDS},
     *        {@code FALLS_BELOW})
     * @param eventType the power metric being monitored (e.g. {@code CAGE_DRAW})
     * @param threshold the threshold configuration for the condition
     */
    public PowerAlertCondition(String conditionType, String eventType, PowerAlertThreshold threshold) {
        this.conditionType = conditionType;
        this.eventType = eventType;
        this.threshold = threshold;
    }
}
