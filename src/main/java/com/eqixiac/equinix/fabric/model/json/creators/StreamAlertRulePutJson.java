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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.StreamAlertRuleType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.Map;

@Setter(AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamAlertRulePutJson {

    @JsonProperty("type")
    private StreamAlertRuleType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("metricSelector")
    private Map<String, Object> metricSelector;

    @JsonProperty("resourceSelector")
    private Map<String, Object> resourceSelector;

    @JsonProperty("detectionMethod")
    private Map<String, Object> detectionMethod;

    public StreamAlertRulePutJson(StreamAlertRuleOperator.StreamAlertRuleUpdater updater) {
        this.type = updater.getType();
        this.name = updater.getName();
        this.description = updater.getDescription();
        this.enabled = updater.getEnabled();
        this.metricSelector = updater.getMetricSelector();
        this.resourceSelector = updater.getResourceSelector();
        this.detectionMethod = updater.getDetectionMethod();
    }
}
