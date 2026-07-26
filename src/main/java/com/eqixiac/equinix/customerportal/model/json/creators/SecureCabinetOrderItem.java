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
 * Cabinet configuration on a secure cabinet order ({@code OrderItem} schema). All fields are
 * required by the API: {@code drawCapacity} (combined power draw in kVA, in 0.5 increments),
 * {@code fabricPort} (include a primary Fabric port), {@code numberOfCabinets},
 * {@code cabinetDimensions} and {@code pdus} (install Equinix-recommended PDUs).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecureCabinetOrderItem {

    @JsonProperty("drawCapacity")
    private final Double drawCapacity;

    @JsonProperty("fabricPort")
    private final Boolean fabricPort;

    @JsonProperty("numberOfCabinets")
    private final Integer numberOfCabinets;

    @JsonProperty("cabinetDimensions")
    private final CabinetDimensions cabinetDimensions;

    @JsonProperty("pdus")
    private final Boolean pdus;

    /**
     * Creates a secure cabinet order item.
     *
     * @param drawCapacity      the combined power draw of all cabinets, in kVA (required)
     * @param fabricPort        whether a primary Fabric port should be included (required)
     * @param numberOfCabinets  the number of ordered cabinets (required)
     * @param cabinetDimensions the cabinet dimensions (required)
     * @param pdus              whether Equinix-recommended PDUs should be installed (required)
     */
    public SecureCabinetOrderItem(Double drawCapacity, Boolean fabricPort, Integer numberOfCabinets,
                                  CabinetDimensions cabinetDimensions, Boolean pdus) {
        this.drawCapacity = drawCapacity;
        this.fabricPort = fabricPort;
        this.numberOfCabinets = numberOfCabinets;
        this.cabinetDimensions = cabinetDimensions;
        this.pdus = pdus;
    }
}
