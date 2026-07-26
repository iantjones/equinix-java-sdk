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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An asset included in a power alert configuration. Used both when reading an existing power
 * alert configuration and when supplying assets for a create or update request.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertConfigurationAsset {

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetName")
    private String assetName;

    /**
     * Creates a configuration asset.
     *
     * @param assetId the unique identifier of the asset (e.g. cage, cabinet, or circuit ID)
     * @param assetName the display name of the asset
     */
    public PowerAlertConfigurationAsset(String assetId, String assetName) {
        this.assetId = assetId;
        this.assetName = assetName;
    }
}
