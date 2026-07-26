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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Ship-to address details for an outbound shipment ({@code shipmentAddress} in the shipments v2
 * spec). All fields are optional. {@code countryCode} is an {@code ISO-3166 ALPHA-2} country code.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentAddress {

    @JsonProperty("carrierAccountNumber")
    private String carrierAccountNumber;

    @JsonProperty("shipToName")
    private String shipToName;

    @JsonProperty("addressLine1")
    private String addressLine1;

    @JsonProperty("addressLine2")
    private String addressLine2;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("zipCode")
    private String zipCode;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    public ShipmentAddress carrierAccountNumber(String carrierAccountNumber) {
        this.carrierAccountNumber = carrierAccountNumber;
        return this;
    }

    public ShipmentAddress shipToName(String shipToName) {
        this.shipToName = shipToName;
        return this;
    }

    public ShipmentAddress addressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
        return this;
    }

    public ShipmentAddress addressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
        return this;
    }

    public ShipmentAddress city(String city) {
        this.city = city;
        return this;
    }

    public ShipmentAddress state(String state) {
        this.state = state;
        return this;
    }

    public ShipmentAddress countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public ShipmentAddress zipCode(String zipCode) {
        this.zipCode = zipCode;
        return this;
    }

    public ShipmentAddress phoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
}
