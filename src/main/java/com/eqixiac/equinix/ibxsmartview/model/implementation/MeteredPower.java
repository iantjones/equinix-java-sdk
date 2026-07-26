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
 * A streaming metered power reading for an IBX cage, including the account it is billed
 * to, the originating tag and the measured value with its unit.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeteredPower {

    @JsonProperty("streamId")
    private String streamId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("asset")
    private MeteredPowerAssetDetails asset;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("cageSerialNo")
    private String cageSerialNo;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("tag")
    private MeteredPowerTagDetails tag;

    @JsonProperty("reading")
    private MeteredPowerValueWithUnit reading;

    @JsonProperty("readingTime")
    private String readingTime;

    @JsonProperty("dataQuality")
    private String dataQuality;
}
