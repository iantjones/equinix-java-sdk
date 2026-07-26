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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.PortType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessPointTypeConfigPort {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("location")
    private LocationSummary location;

    @JsonProperty("sellerRegion")
    private String sellerRegion;

    @JsonProperty("sellerRegionDescription")
    private String sellerRegionDescription;

    @JsonProperty("crossConnectId")
    private String crossConnectId;

    public AccessPointTypeConfigPort(String uuid, PortType type) {
        this.uuid = uuid;
        this.type = type;
    }
}