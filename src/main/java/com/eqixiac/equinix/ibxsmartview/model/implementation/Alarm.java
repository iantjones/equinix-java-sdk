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

package com.eqixiac.equinix.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A streaming alarm message describing a triggered condition for an IBX asset,
 * including its severity, threshold, current value and lifecycle status.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm {

    @JsonProperty("streamId")
    private String streamId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("region")
    private String region;

    @JsonProperty("metro")
    private String metro;

    @JsonProperty("country")
    private String country;

    @JsonProperty("conditionName")
    private String conditionName;

    @JsonProperty("severity")
    private Integer severity;

    @JsonProperty("type")
    private String type;

    @JsonProperty("heartbeat")
    private Boolean heartbeat;

    @JsonProperty("triggerRule")
    private String triggerRule;

    @JsonProperty("definitionId")
    private String definitionId;

    @JsonProperty("currentValue")
    private AlarmCurrentValueDetails currentValue;

    @JsonProperty("asset")
    private AlarmAssetDetails asset;

    @JsonProperty("tag")
    private AlarmTagDetails tag;

    @JsonProperty("status")
    private AlarmStatusDetails status;

    @JsonProperty("threshold")
    private AlarmThresholdDetails threshold;

    @JsonProperty("triggeredTime")
    private String triggeredTime;

    @JsonProperty("processedTime")
    private String processedTime;

    @JsonProperty("normalProcessedTime")
    private String normalProcessedTime;

    @JsonProperty("normalTriggeredTime")
    private String normalTriggeredTime;

    @JsonProperty("dataQuality")
    private String dataQuality;
}
