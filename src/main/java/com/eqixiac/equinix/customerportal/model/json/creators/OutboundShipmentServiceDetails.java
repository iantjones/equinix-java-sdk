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
import com.eqixiac.equinix.customerportal.enums.ShipmentTransportType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Service details for an outbound v1 shipment order ({@code outboundServiceDetail} in the
 * shipments v1 spec). {@code estimatedDateTime} (ISO date-time, not in the past) and
 * {@code shipmentDetails} are required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutboundShipmentServiceDetails {

    @JsonProperty("estimatedDateTime")
    private final String estimatedDateTime;

    @JsonProperty("shipmentDetails")
    private final ShipmentDetails shipmentDetails;

    @JsonProperty("outboundRequestDescription")
    private final String outboundRequestDescription;

    private OutboundShipmentServiceDetails(Builder builder) {
        this.estimatedDateTime = builder.estimatedDateTime;
        this.shipmentDetails = builder.shipmentDetails;
        this.outboundRequestDescription = builder.outboundRequestDescription;
    }

    /**
     * Returns a new builder for outbound shipment service details.
     *
     * @param estimatedDateTime the estimated shipment date/time (ISO date-time) (required)
     * @param shipmentDetails   the shipment details (required)
     * @return a new builder
     */
    public static Builder builder(String estimatedDateTime, ShipmentDetails shipmentDetails) {
        return new Builder(estimatedDateTime, shipmentDetails);
    }

    public static class Builder {
        private final String estimatedDateTime;
        private final ShipmentDetails shipmentDetails;
        private String outboundRequestDescription;

        private Builder(String estimatedDateTime, ShipmentDetails shipmentDetails) {
            this.estimatedDateTime = estimatedDateTime;
            this.shipmentDetails = shipmentDetails;
        }

        public Builder outboundRequestDescription(String outboundRequestDescription) {
            this.outboundRequestDescription = outboundRequestDescription;
            return this;
        }

        public OutboundShipmentServiceDetails build() {
            return new OutboundShipmentServiceDetails(this);
        }
    }

    /**
     * Details of the outbound shipment itself ({@code outboundServiceDetail.shipmentDetails}).
     * {@code outboundType} is required. {@code otherCarrierName} applies when
     * {@code carrierName} is {@code OTHER}; {@code uploadedLabel} references a previously
     * uploaded shipping label attachment when {@code labelExists} is {@code true}.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShipmentDetails {

        @JsonProperty("outboundType")
        private final ShipmentTransportType outboundType;

        @JsonProperty("carrierName")
        private ShipmentCarrier carrierName;

        @JsonProperty("trackingNumber")
        private String trackingNumber;

        @JsonProperty("noOfBoxes")
        private Integer noOfBoxes;

        @JsonProperty("declaredValue")
        private String declaredValue;

        @JsonProperty("labelExists")
        private Boolean labelExists;

        @JsonProperty("shipToAddress")
        private ShipToAddress shipToAddress;

        @JsonProperty("otherCarrierName")
        private String otherCarrierName;

        @JsonProperty("uploadedLabel")
        private OrderAttachment uploadedLabel;

        @JsonProperty("insureShipment")
        private Boolean insureShipment;

        @JsonProperty("pickUpFromCageSuite")
        private Boolean pickUpFromCageSuite;

        @JsonProperty("description")
        private String description;

        public ShipmentDetails(ShipmentTransportType outboundType) {
            this.outboundType = outboundType;
        }

        public ShipmentDetails carrierName(ShipmentCarrier carrierName) {
            this.carrierName = carrierName;
            return this;
        }

        public ShipmentDetails trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public ShipmentDetails noOfBoxes(Integer noOfBoxes) {
            this.noOfBoxes = noOfBoxes;
            return this;
        }

        public ShipmentDetails declaredValue(String declaredValue) {
            this.declaredValue = declaredValue;
            return this;
        }

        public ShipmentDetails labelExists(Boolean labelExists) {
            this.labelExists = labelExists;
            return this;
        }

        public ShipmentDetails shipToAddress(ShipToAddress shipToAddress) {
            this.shipToAddress = shipToAddress;
            return this;
        }

        public ShipmentDetails otherCarrierName(String otherCarrierName) {
            this.otherCarrierName = otherCarrierName;
            return this;
        }

        public ShipmentDetails uploadedLabel(OrderAttachment uploadedLabel) {
            this.uploadedLabel = uploadedLabel;
            return this;
        }

        public ShipmentDetails insureShipment(Boolean insureShipment) {
            this.insureShipment = insureShipment;
            return this;
        }

        public ShipmentDetails pickUpFromCageSuite(Boolean pickUpFromCageSuite) {
            this.pickUpFromCageSuite = pickUpFromCageSuite;
            return this;
        }

        public ShipmentDetails description(String description) {
            this.description = description;
            return this;
        }
    }

    /**
     * The destination address of an outbound shipment
     * ({@code outboundServiceDetail.shipmentDetails.shipToAddress}). All fields are required by
     * the shipments v1 spec.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShipToAddress {

        @JsonProperty("name")
        private final String name;

        @JsonProperty("address")
        private final String address;

        @JsonProperty("city")
        private final String city;

        @JsonProperty("state")
        private final String state;

        @JsonProperty("country")
        private final String country;

        @JsonProperty("zipCode")
        private final String zipCode;

        @JsonProperty("phoneNumber")
        private final String phoneNumber;

        @JsonProperty("carrierAccountNumber")
        private final String carrierAccountNumber;

        public ShipToAddress(String name, String address, String city, String state, String country,
                             String zipCode, String phoneNumber, String carrierAccountNumber) {
            this.name = name;
            this.address = address;
            this.city = city;
            this.state = state;
            this.country = country;
            this.zipCode = zipCode;
            this.phoneNumber = phoneNumber;
            this.carrierAccountNumber = carrierAccountNumber;
        }
    }
}
