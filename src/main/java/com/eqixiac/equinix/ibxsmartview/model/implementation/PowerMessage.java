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
 * A streaming power message for an IBX cabinet, carrying a comprehensive set of power
 * measurements (real, apparent, contractual, current, power factor and seven-day peaks)
 * each paired with its unit.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerMessage {

    @JsonProperty("streamId")
    private String streamId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("asset")
    private PowerAssetDetails asset;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("cabinet")
    private String cabinet;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("description")
    private String description;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("circuitType")
    private String circuitType;

    @JsonProperty("realPower")
    private PowerValueWithUnit realPower;

    @JsonProperty("apparentPower")
    private PowerValueWithUnit apparentPower;

    @JsonProperty("contractualPower")
    private PowerValueWithUnit contractualPower;

    @JsonProperty("current")
    private PowerValueWithUnit current;

    @JsonProperty("powerFactor")
    private PowerValueWithUnit powerFactor;

    @JsonProperty("soldCurrent")
    private PowerValueWithUnit soldCurrent;

    @JsonProperty("soldPower")
    private PowerValueWithUnit soldPower;

    @JsonProperty("powerConsumptionToContractual")
    private PowerValueWithUnit powerConsumptionToContractual;

    @JsonProperty("cabinetRating")
    private PowerValueWithUnit cabinetRating;

    @JsonProperty("peakLastSevenDays")
    private PowerValueWithUnit peakLastSevenDays;

    @JsonProperty("peakLastSevenDaysRatio")
    private PowerValueWithUnit peakLastSevenDaysRatio;

    @JsonProperty("peakLastSevenDaysContractualPower")
    private PowerValueWithUnit peakLastSevenDaysContractualPower;

    @JsonProperty("peakLastSevenDaysTime")
    private String peakLastSevenDaysTime;

    @JsonProperty("lastUpdated")
    private String lastUpdated;

    @JsonProperty("readingTime")
    private String readingTime;
}
