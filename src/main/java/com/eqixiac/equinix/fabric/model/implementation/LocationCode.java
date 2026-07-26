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

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A simplified location (the Fabric v4 {@code SimplifiedLocation} /
 * {@code SimplifiedLocationWithoutIBX} schemas): metro code plus the metro's URI, name
 * and region.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationCode {

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("metroHref")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String metroHref;

    @JsonProperty("metroName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String metroName;

    @JsonProperty("region")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Region region;

    /**
     * IBX identifier; populated on pricing responses (spec schema {@code PriceLocation})
     * and, deprecated, on {@code SimplifiedLocation} reads.
     */
    @JsonProperty("ibx")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String ibx;

    /**
     * Creates a location keyed by metro code (for METRO-scoped resources).
     *
     * @param metroCode the metro code
     */
    public LocationCode(MetroCode metroCode) {
        this.metroCode = metroCode;
    }

    /**
     * Creates a location keyed by metro code and/or region (REGIONAL-scope networks are
     * located by region rather than metro code).
     *
     * @param metroCode the metro code, or {@code null} for region-scoped locations
     * @param region the region, or {@code null} for metro-scoped locations
     */
    public LocationCode(MetroCode metroCode, Region region) {
        this.metroCode = metroCode;
        this.region = region;
    }
}
