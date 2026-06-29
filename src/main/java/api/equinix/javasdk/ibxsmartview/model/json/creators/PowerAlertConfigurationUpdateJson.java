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

package api.equinix.javasdk.ibxsmartview.model.json.creators;

import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertCondition;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertRecipient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Request body for updating an existing power alert configuration. Maps to the
 * {@code UpdateAlertConfigurationCommand} schema of the IBX SmartView power-events API. The
 * {@code alertConfigurationUid} is required; the remaining fields are applied when present.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowerAlertConfigurationUpdateJson {

    @JsonProperty("alertConfigurationUid")
    private String alertConfigurationUid;

    @JsonProperty("state")
    private String state;

    @JsonProperty("condition")
    private PowerAlertCondition condition;

    @JsonProperty("recipients")
    private List<PowerAlertRecipient> recipients;

    @JsonProperty("assets")
    private Map<String, List<PowerAlertConfigurationAsset>> assets;
}
