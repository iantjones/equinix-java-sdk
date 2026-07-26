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

package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.core.enums.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Data-center location of a Digital LOA patch panel (diLOA v1 {@code Location} schema): the IBX
 * identifier, its {@code region} ({@code AMER}, {@code APAC} or {@code EMEA}), metro name/code,
 * country code and street address.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaLocation {

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("metroName")
    private String metroName;

    @JsonProperty("metroCode")
    private String metroCode;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("address")
    private String address;
}
