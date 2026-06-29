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
 * Details for updating an outbound shipment ({@code OutboundShipmentsUpdate} in the shipments v2
 * spec). All fields are optional; supply only those being changed. Note that
 * {@code CUSTOMER_CARRIER} is not a valid carrier on update. {@code declaredValueCurrency} is an
 * {@code ISO-4217} currency code. Supply this to
 * {@link ShipmentUpdateRequest.Builder#details(Object)}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundShipmentUpdateDetails {

    @JsonProperty("carrier")
    private ShipmentCarrier carrier;

    @JsonProperty("carrierTrackingNumbers")
    private List<String> carrierTrackingNumbers;

    @JsonProperty("carrierName")
    private String carrierName;

    @JsonProperty("numberOfBoxes")
    private Integer numberOfBoxes;

    @JsonProperty("declaredValue")
    private Double declaredValue;

    @JsonProperty("declaredValueCurrency")
    private String declaredValueCurrency;

    @JsonProperty("requirePickup")
    private Boolean requirePickup;

    @JsonProperty("insureShipment")
    private Boolean insureShipment;

    @JsonProperty("shipmentAttachmentId")
    private String shipmentAttachmentId;

    @JsonProperty("shipmentAddress")
    private ShipmentAddress shipmentAddress;

    public OutboundShipmentUpdateDetails carrier(ShipmentCarrier carrier) {
        this.carrier = carrier;
        return this;
    }

    public OutboundShipmentUpdateDetails carrierTrackingNumbers(List<String> carrierTrackingNumbers) {
        this.carrierTrackingNumbers = carrierTrackingNumbers;
        return this;
    }

    public OutboundShipmentUpdateDetails carrierName(String carrierName) {
        this.carrierName = carrierName;
        return this;
    }

    public OutboundShipmentUpdateDetails numberOfBoxes(Integer numberOfBoxes) {
        this.numberOfBoxes = numberOfBoxes;
        return this;
    }

    public OutboundShipmentUpdateDetails declaredValue(Double declaredValue) {
        this.declaredValue = declaredValue;
        return this;
    }

    public OutboundShipmentUpdateDetails declaredValueCurrency(String declaredValueCurrency) {
        this.declaredValueCurrency = declaredValueCurrency;
        return this;
    }

    public OutboundShipmentUpdateDetails requirePickup(Boolean requirePickup) {
        this.requirePickup = requirePickup;
        return this;
    }

    public OutboundShipmentUpdateDetails insureShipment(Boolean insureShipment) {
        this.insureShipment = insureShipment;
        return this;
    }

    public OutboundShipmentUpdateDetails shipmentAttachmentId(String shipmentAttachmentId) {
        this.shipmentAttachmentId = shipmentAttachmentId;
        return this;
    }

    public OutboundShipmentUpdateDetails shipmentAddress(ShipmentAddress shipmentAddress) {
        this.shipmentAddress = shipmentAddress;
        return this;
    }
}
