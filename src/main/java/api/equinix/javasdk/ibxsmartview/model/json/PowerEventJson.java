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

package api.equinix.javasdk.ibxsmartview.model.json;

import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerEventAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerEventProcessing;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerEventJson implements PowerEvent {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("alertUid")
    private String alertUid;

    @JsonProperty("traceUid")
    private String traceUid;

    @JsonProperty("status")
    private AlertStatus status;

    @JsonProperty("asset")
    private PowerEventAsset asset;

    @JsonProperty("activeProcessing")
    private PowerEventProcessing activeProcessing;

    @JsonProperty("category")
    private String category;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("conditionType")
    private String conditionType;

    @JsonProperty("triggerValue")
    private String triggerValue;

    @JsonProperty("currentValue")
    private String currentValue;

    @JsonProperty("accountNo")
    private String accountNo;
}
