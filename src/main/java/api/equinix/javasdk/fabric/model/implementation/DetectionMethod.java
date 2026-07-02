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
import api.equinix.javasdk.fabric.enums.DetectionMethodType;
import api.equinix.javasdk.fabric.enums.DetectionMethodOperand;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * How a stream alert rule detects an alert condition (the Fabric v4
 * {@code DetectionMethodResponse} schema): a {@code THRESHOLD} or {@code OUTLIER} check
 * over a metric window, with warning/critical thresholds and an ABOVE/BELOW operand.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectionMethod {

    @JsonProperty("type")
    private DetectionMethodType type;

    @JsonProperty("windowSize")
    private String windowSize;

    @JsonProperty("operand")
    private DetectionMethodOperand operand;

    @JsonProperty("warningThreshold")
    private String warningThreshold;

    @JsonProperty("criticalThreshold")
    private String criticalThreshold;
}
