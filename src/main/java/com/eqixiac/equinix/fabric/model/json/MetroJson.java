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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.enums.MetroType;
import com.eqixiac.equinix.fabric.model.implementation.GeoZone;
import com.eqixiac.equinix.fabric.model.implementation.MetroService;
import com.eqixiac.equinix.fabric.model.implementation.MetroSummary;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetroJson extends MetroSummary {


    @JsonProperty("type")
    private MetroType type;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("country")
    private String country;

    @JsonProperty("equinixAsn")
    private Long equinixAsn;

    @JsonProperty("localVCBandwidthMax")
    private Long localVCBandwidthMax;

    @JsonProperty("geoCoordinates")
    private GeoCoordinate geoCoordinates;

    @JsonProperty("connectedMetros")
    private List<ConnectedMetro> connectedMetros;

    @JsonProperty("services")
    private List<MetroService> services;

    @JsonProperty("geoScopes")
    private List<GeoScopeType> geoScopes;

    @JsonProperty("geoZones")
    private List<GeoZone> geoZones;
}
