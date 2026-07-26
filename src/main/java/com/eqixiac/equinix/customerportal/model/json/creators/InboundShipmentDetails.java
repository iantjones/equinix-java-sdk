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

import com.eqixiac.equinix.customerportal.enums.ShipmentCarrier;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Details for creating an inbound shipment ({@code InboundShipments_create} in the shipments v2
 * spec). {@code carrier}, {@code carrierTrackingNumbers} and {@code numberOfBoxes} are required;
 * {@code carrierName} (used when {@code carrier} is {@code OTHER}) and {@code cageDelivery} are
 * optional. Supply this to {@link ShipmentOrderRequest#builder(String, String, String, Object)}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboundShipmentDetails {

    @JsonProperty("carrier")
    private final ShipmentCarrier carrier;

    @JsonProperty("carrierTrackingNumbers")
    private final List<String> carrierTrackingNumbers;

    @JsonProperty("numberOfBoxes")
    private final Integer numberOfBoxes;

    @JsonProperty("carrierName")
    private String carrierName;

    @JsonProperty("cageDelivery")
    private Boolean cageDelivery;

    private InboundShipmentDetails(Builder builder) {
        this.carrier = builder.carrier;
        this.carrierTrackingNumbers = builder.carrierTrackingNumbers;
        this.numberOfBoxes = builder.numberOfBoxes;
        this.carrierName = builder.carrierName;
        this.cageDelivery = builder.cageDelivery;
    }

    /**
     * Returns a new builder for inbound shipment create details.
     *
     * @param carrier                the delivering carrier (required)
     * @param carrierTrackingNumbers the shipment tracking numbers (required)
     * @param numberOfBoxes          the number of boxes to be received (required)
     * @return a new builder
     */
    public static Builder builder(ShipmentCarrier carrier, List<String> carrierTrackingNumbers,
                                  Integer numberOfBoxes) {
        return new Builder(carrier, carrierTrackingNumbers, numberOfBoxes);
    }

    public static class Builder {
        private final ShipmentCarrier carrier;
        private final List<String> carrierTrackingNumbers;
        private final Integer numberOfBoxes;
        private String carrierName;
        private Boolean cageDelivery;

        private Builder(ShipmentCarrier carrier, List<String> carrierTrackingNumbers, Integer numberOfBoxes) {
            this.carrier = carrier;
            this.carrierTrackingNumbers = carrierTrackingNumbers;
            this.numberOfBoxes = numberOfBoxes;
        }

        public Builder carrierName(String carrierName) {
            this.carrierName = carrierName;
            return this;
        }

        public Builder cageDelivery(Boolean cageDelivery) {
            this.cageDelivery = cageDelivery;
            return this;
        }

        public InboundShipmentDetails build() {
            return new InboundShipmentDetails(this);
        }
    }
}
