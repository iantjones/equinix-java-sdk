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
 * Details for creating an outbound shipment ({@code OutboundShipments_create} in the shipments v2
 * spec). Only {@code carrier} is required; all other fields are optional.
 * {@code declaredValueCurrency} is an {@code ISO-4217} currency code. Supply this to
 * {@link ShipmentOrderRequest#builder(String, String, String, Object)}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundShipmentDetails {

    @JsonProperty("carrier")
    private final ShipmentCarrier carrier;

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

    @JsonProperty("shipmentLabelRequired")
    private Boolean shipmentLabelRequired;

    @JsonProperty("shipmentAttachmentId")
    private String shipmentAttachmentId;

    @JsonProperty("shipmentAddress")
    private ShipmentAddress shipmentAddress;

    private OutboundShipmentDetails(Builder builder) {
        this.carrier = builder.carrier;
        this.carrierTrackingNumbers = builder.carrierTrackingNumbers;
        this.carrierName = builder.carrierName;
        this.numberOfBoxes = builder.numberOfBoxes;
        this.declaredValue = builder.declaredValue;
        this.declaredValueCurrency = builder.declaredValueCurrency;
        this.requirePickup = builder.requirePickup;
        this.insureShipment = builder.insureShipment;
        this.shipmentLabelRequired = builder.shipmentLabelRequired;
        this.shipmentAttachmentId = builder.shipmentAttachmentId;
        this.shipmentAddress = builder.shipmentAddress;
    }

    /**
     * Returns a new builder for outbound shipment create details.
     *
     * @param carrier the delivering carrier (required)
     * @return a new builder
     */
    public static Builder builder(ShipmentCarrier carrier) {
        return new Builder(carrier);
    }

    public static class Builder {
        private final ShipmentCarrier carrier;
        private List<String> carrierTrackingNumbers;
        private String carrierName;
        private Integer numberOfBoxes;
        private Double declaredValue;
        private String declaredValueCurrency;
        private Boolean requirePickup;
        private Boolean insureShipment;
        private Boolean shipmentLabelRequired;
        private String shipmentAttachmentId;
        private ShipmentAddress shipmentAddress;

        private Builder(ShipmentCarrier carrier) {
            this.carrier = carrier;
        }

        public Builder carrierTrackingNumbers(List<String> carrierTrackingNumbers) {
            this.carrierTrackingNumbers = carrierTrackingNumbers;
            return this;
        }

        public Builder carrierName(String carrierName) {
            this.carrierName = carrierName;
            return this;
        }

        public Builder numberOfBoxes(Integer numberOfBoxes) {
            this.numberOfBoxes = numberOfBoxes;
            return this;
        }

        public Builder declaredValue(Double declaredValue) {
            this.declaredValue = declaredValue;
            return this;
        }

        public Builder declaredValueCurrency(String declaredValueCurrency) {
            this.declaredValueCurrency = declaredValueCurrency;
            return this;
        }

        public Builder requirePickup(Boolean requirePickup) {
            this.requirePickup = requirePickup;
            return this;
        }

        public Builder insureShipment(Boolean insureShipment) {
            this.insureShipment = insureShipment;
            return this;
        }

        public Builder shipmentLabelRequired(Boolean shipmentLabelRequired) {
            this.shipmentLabelRequired = shipmentLabelRequired;
            return this;
        }

        public Builder shipmentAttachmentId(String shipmentAttachmentId) {
            this.shipmentAttachmentId = shipmentAttachmentId;
            return this;
        }

        public Builder shipmentAddress(ShipmentAddress shipmentAddress) {
            this.shipmentAddress = shipmentAddress;
            return this;
        }

        public OutboundShipmentDetails build() {
            return new OutboundShipmentDetails(this);
        }
    }
}
