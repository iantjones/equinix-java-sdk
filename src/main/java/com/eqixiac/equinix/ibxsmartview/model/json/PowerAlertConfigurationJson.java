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

package com.eqixiac.equinix.ibxsmartview.model.json;

import com.eqixiac.equinix.ibxsmartview.enums.AlertConfigurationState;
import com.eqixiac.equinix.ibxsmartview.model.PowerAlertConfiguration;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertCondition;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertCreator;
import com.eqixiac.equinix.ibxsmartview.model.implementation.PowerAlertRecipient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertConfigurationJson implements PowerAlertConfiguration {

    @JsonProperty("alertConfigurationUid")
    private String alertConfigurationUid;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("state")
    private AlertConfigurationState state;

    @JsonProperty("section")
    private String section;

    @JsonProperty("source")
    private String source;

    @JsonProperty("condition")
    private PowerAlertCondition condition;

    @JsonProperty("recipients")
    private List<PowerAlertRecipient> recipients;

    @JsonProperty("creator")
    private PowerAlertCreator creator;

    @JsonProperty("createdOn")
    private String createdOn;

    @JsonProperty("updatedOn")
    private String updatedOn;

    @JsonProperty("assets")
    private Map<String, List<PowerAlertConfigurationAsset>> assets;
}
