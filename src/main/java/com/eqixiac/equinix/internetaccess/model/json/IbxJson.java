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

package com.eqixiac.equinix.internetaccess.model.json;

import com.eqixiac.equinix.internetaccess.enums.Region;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.implementation.GeoCoordinates;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Read-only JSON model for an {@link Ibx} returned by the Equinix Internet Access (EIA)
 * product-availability lookups — the v2 list ({@code GET /internetAccess/v2/ibxs}) and the v1
 * single-IBX get ({@code GET /internetAccess/v1/ibxs/{ibx}}). Implements {@link Ibx} directly,
 * so no wrapper is required.
 *
 * <p>The v1 single-IBX response names the data-center code field {@code ibx} (the v2 list uses
 * {@code ibxCode}); both bind to {@link #ibxCode} via {@link JsonAlias}.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IbxJson implements Ibx {

    @JsonProperty("href")
    private String href;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("countryName")
    private String countryName;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("metroCode")
    private String metroCode;

    @JsonProperty("metroName")
    private String metroName;

    @JsonProperty("ibxCode")
    @JsonAlias("ibx")
    private String ibxCode;

    @JsonProperty("geoCoordinates")
    private GeoCoordinates geoCoordinates;
}
