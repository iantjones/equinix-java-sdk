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

package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * IBX detail for the order history locations endpoint
 * ({@code ibx-detail-for-locations-endpoint}): the IBX {@code code} and its {@code metro},
 * {@code region}, {@code country}, {@code city}, {@code state}, {@code address} and
 * {@code postalCode}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IbxDetail {

    @JsonProperty("code")
    private String code;

    @JsonProperty("metro")
    private String metro;

    @JsonProperty("region")
    private String region;

    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("address")
    private String address;

    @JsonProperty("postalCode")
    private String postalCode;
}
