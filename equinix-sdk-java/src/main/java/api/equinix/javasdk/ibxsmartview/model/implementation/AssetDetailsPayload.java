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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Asset details payload including the asset's tag points (AssetDetails in the spec).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDetailsPayload {

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("userPrefTimeZone")
    private String userPrefTimeZone;

    @JsonProperty("tags")
    private List<TagPointDataArray> tags;

    @JsonProperty("lastMaintenanceDate")
    private String lastMaintenanceDate;

    @JsonProperty("manufacturerName")
    private String manufacturerName;

    @JsonProperty("equipmentModelNumber")
    private String equipmentModelNumber;

    @JsonProperty("equipmentSerialNumber")
    private String equipmentSerialNumber;

    @JsonProperty("alarmLastTriggeredTime")
    private String alarmLastTriggeredTime;

    @JsonProperty("alarmLastProcessedTime")
    private String alarmLastProcessedTime;
}
