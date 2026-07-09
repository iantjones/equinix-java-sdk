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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The alert configuration that produced a system alert ({@code ConfigurationReadModel} in the spec).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertConfiguration {

    @JsonProperty("thresholdValue")
    private String thresholdValue;

    @JsonProperty("thresholdValueDisplayName")
    private String thresholdValueDisplayName;

    @JsonProperty("unitOfMeasurement")
    private String unitOfMeasurement;

    @JsonProperty("thresholdType")
    private String thresholdType;

    @JsonProperty("conditionName")
    private String conditionName;

    @JsonProperty("customerVisible")
    private Boolean customerVisible;

    @JsonProperty("configurationVersion")
    private String configurationVersion;
}
