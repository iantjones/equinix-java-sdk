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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Address of an access point (spec 4.34.0 {@code Address}, sent on {@code AccessPoint.address},
 * for example by the last-mile {@code Colo2Sp-LMaaS-Primary} example).
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessPointAddress {

    @JsonProperty("id")
    private String id;

    @JsonProperty("streetNr")
    private String streetNr;

    @JsonProperty("streetName")
    private String streetName;

    @JsonProperty("city")
    private String city;

    @JsonProperty("stateOrProvince")
    private String stateOrProvince;

    @JsonProperty("postCode")
    private String postCode;

    @JsonProperty("country")
    private String country;

    /**
     * Builder constructor; use {@code builder()}.
     *
     * @param id the address id
     * @param streetNr the street number
     * @param streetName the street name
     * @param city the city
     * @param stateOrProvince the state or province
     * @param postCode the postal code
     * @param country the country
     */
    @Builder
    private AccessPointAddress(String id, String streetNr, String streetName, String city,
                               String stateOrProvince, String postCode, String country) {
        this.id = id;
        this.streetNr = streetNr;
        this.streetName = streetName;
        this.city = city;
        this.stateOrProvince = stateOrProvince;
        this.postCode = postCode;
        this.country = country;
    }
}
