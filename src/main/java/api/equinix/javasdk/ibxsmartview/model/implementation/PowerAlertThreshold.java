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
 * Threshold configuration for a power alert condition. Used both when reading an existing power
 * alert configuration and when supplying the condition for a create or update request.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertThreshold {

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("value")
    private String value;

    @JsonProperty("min")
    private Object min;

    @JsonProperty("max")
    private Object max;

    /**
     * Creates a simple single-value threshold.
     *
     * @param unit the unit of measurement for the threshold value (e.g. {@code %})
     * @param value the threshold value that triggers the alert
     */
    public PowerAlertThreshold(String unit, String value) {
        this.unit = unit;
        this.value = value;
    }
}
