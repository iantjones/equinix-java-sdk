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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.ShipmentCarrier;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Details for updating an inbound shipment ({@code InboundShipmentsUpdate} in the shipments v2
 * spec). All fields are optional; supply only those being changed. Note that
 * {@code CUSTOMER_CARRIER} is not a valid carrier on update. Supply this to
 * {@link ShipmentUpdateRequest.Builder#details(Object)}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboundShipmentUpdateDetails {

    @JsonProperty("carrier")
    private ShipmentCarrier carrier;

    @JsonProperty("carrierTrackingNumbers")
    private List<String> carrierTrackingNumbers;

    @JsonProperty("carrierName")
    private String carrierName;

    @JsonProperty("numberOfBoxes")
    private Integer numberOfBoxes;

    @JsonProperty("cageDelivery")
    private Boolean cageDelivery;

    public InboundShipmentUpdateDetails carrier(ShipmentCarrier carrier) {
        this.carrier = carrier;
        return this;
    }

    public InboundShipmentUpdateDetails carrierTrackingNumbers(List<String> carrierTrackingNumbers) {
        this.carrierTrackingNumbers = carrierTrackingNumbers;
        return this;
    }

    public InboundShipmentUpdateDetails carrierName(String carrierName) {
        this.carrierName = carrierName;
        return this;
    }

    public InboundShipmentUpdateDetails numberOfBoxes(Integer numberOfBoxes) {
        this.numberOfBoxes = numberOfBoxes;
        return this;
    }

    public InboundShipmentUpdateDetails cageDelivery(Boolean cageDelivery) {
        this.cageDelivery = cageDelivery;
        return this;
    }
}
