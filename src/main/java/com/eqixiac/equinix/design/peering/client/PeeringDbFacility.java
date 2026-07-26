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

package com.eqixiac.equinix.design.peering.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a data center facility from the PeeringDB API.
 *
 * <p>Contains physical location data including coordinates, address, and connectivity
 * metrics for a facility. When scoped to Equinix (org_id=2), represents an Equinix
 * International Business Exchange (IBX) data center. The {@code latitude} and
 * {@code longitude} fields enable geographic proximity matching to Equinix metros.</p>
 *
 * @author ianjones
 * @see PeeringDbNetFac
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbFacility {

    @JsonProperty("id")
    private int id;

    @JsonProperty("org_id")
    private int orgId;

    @JsonProperty("org_name")
    private String orgName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("aka")
    private String aka;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("country")
    private String country;

    @JsonProperty("zipcode")
    private String zipcode;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("clli")
    private String clli;

    @JsonProperty("net_count")
    private int netCount;

    @JsonProperty("ix_count")
    private int ixCount;

    @JsonProperty("region_continent")
    private String regionContinent;

    @JsonProperty("status")
    private String status;
}
