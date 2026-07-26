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
 * An asset element within an asset-list template, including alarm and resiliency status.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetForAssetAPI {

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("alarmStatus")
    private String alarmStatus;

    @JsonProperty("resiliencyStatus")
    private String resiliencyStatus;

    @JsonProperty("alarmLastTriggeredTime")
    private String alarmLastTriggeredTime;

    @JsonProperty("alarmLastClearedTime")
    private String alarmLastClearedTime;

    @JsonProperty("primaryParentAsset")
    private String primaryParentAsset;

    @JsonProperty("alternateParentAsset")
    private String alternateParentAsset;
}
