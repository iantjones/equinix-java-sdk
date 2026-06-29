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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.enums.FabricPortSpeed;
import api.equinix.javasdk.customerportal.model.ProductAvailability;
import api.equinix.javasdk.customerportal.model.implementation.AcCircuitConfig;
import api.equinix.javasdk.customerportal.model.implementation.CabinetDimensionsInfo;
import api.equinix.javasdk.customerportal.model.implementation.PduConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * JSON model for a secure cabinet {@code ProductsAvailability} element.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductAvailabilityJson implements ProductAvailability {

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("maximumNumberOfCabinetsToOrder")
    private Integer maximumNumberOfCabinetsToOrder;

    @JsonProperty("minimumDrawCapacityPerCabinet")
    private Double minimumDrawCapacityPerCabinet;

    @JsonProperty("maximumDrawCapacityPerCabinet")
    private Double maximumDrawCapacityPerCabinet;

    @JsonProperty("cabinetDimensions")
    private CabinetDimensionsInfo cabinetDimensions;

    @JsonProperty("acCircuitConfiguration")
    private AcCircuitConfig acCircuitConfiguration;

    @JsonProperty("pduConfiguration")
    private PduConfig pduConfiguration;

    @JsonProperty("fabricPortSpeed")
    private FabricPortSpeed fabricPortSpeed;
}
