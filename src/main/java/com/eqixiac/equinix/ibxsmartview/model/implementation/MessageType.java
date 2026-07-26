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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The set of message types a streaming subscription delivers. Each property is a list of the
 * corresponding message-type filters; only the populated message types are subscribed to.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageType {

    @JsonProperty("asset")
    private List<AssetMessageType> asset;

    @JsonProperty("environmental")
    private List<EnvironmentalMessageType> environmental;

    @JsonProperty("power")
    private List<PowerMessageType> power;

    @JsonProperty("meteredPower")
    private List<MeteredPower> meteredPower;

    @JsonProperty("systemAlert")
    private List<SystemAlertMessageType> systemAlert;

    @JsonProperty("customAlert")
    private List<CustomAlertMessageType> customAlert;

    @Builder
    public MessageType(List<AssetMessageType> asset, List<EnvironmentalMessageType> environmental,
                       List<PowerMessageType> power, List<MeteredPower> meteredPower,
                       List<SystemAlertMessageType> systemAlert, List<CustomAlertMessageType> customAlert) {
        this.asset = asset;
        this.environmental = environmental;
        this.power = power;
        this.meteredPower = meteredPower;
        this.systemAlert = systemAlert;
        this.customAlert = customAlert;
    }
}
