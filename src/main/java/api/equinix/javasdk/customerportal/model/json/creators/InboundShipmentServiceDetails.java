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
import api.equinix.javasdk.customerportal.enums.ShipmentTransportType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Service details for an inbound v1 shipment order ({@code inboundServiceDetail} in the
 * shipments v1 spec). {@code estimatedDateTime} (ISO date-time, not in the past) and
 * {@code shipmentDetails} are required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboundShipmentServiceDetails {

    @JsonProperty("estimatedDateTime")
    private final String estimatedDateTime;

    @JsonProperty("shipmentDetails")
    private final ShipmentDetails shipmentDetails;

    @JsonProperty("deliverToCage")
    private final Boolean deliverToCage;

    @JsonProperty("inboundRequestDescription")
    private final String inboundRequestDescription;

    @JsonProperty("storageId")
    private final String storageId;

    private InboundShipmentServiceDetails(Builder builder) {
        this.estimatedDateTime = builder.estimatedDateTime;
        this.shipmentDetails = builder.shipmentDetails;
        this.deliverToCage = builder.deliverToCage;
        this.inboundRequestDescription = builder.inboundRequestDescription;
        this.storageId = builder.storageId;
    }

    /**
     * Returns a new builder for inbound shipment service details.
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
        private Boolean deliverToCage;
        private String inboundRequestDescription;
        private String storageId;

        private Builder(String estimatedDateTime, ShipmentDetails shipmentDetails) {
            this.estimatedDateTime = estimatedDateTime;
            this.shipmentDetails = shipmentDetails;
        }

        public Builder deliverToCage(Boolean deliverToCage) {
            this.deliverToCage = deliverToCage;
            return this;
        }

        public Builder inboundRequestDescription(String inboundRequestDescription) {
            this.inboundRequestDescription = inboundRequestDescription;
            return this;
        }

        public Builder storageId(String storageId) {
            this.storageId = storageId;
            return this;
        }

        public InboundShipmentServiceDetails build() {
            return new InboundShipmentServiceDetails(this);
        }
    }

    /**
     * Details of the inbound shipment itself ({@code inboundServiceDetail.shipmentDetails}).
     * {@code noOfBoxes} and {@code inboundType} are required; {@code otherCarrierName} applies
     * when {@code carrierName} is {@code OTHER}.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShipmentDetails {

        @JsonProperty("noOfBoxes")
        private final Integer noOfBoxes;

        @JsonProperty("inboundType")
        private final ShipmentTransportType inboundType;

        @JsonProperty("isOverSized")
        private Boolean isOverSized;

        @JsonProperty("description")
        private String description;

        @JsonProperty("trackingNumber")
        private List<String> trackingNumber;

        @JsonProperty("carrierName")
        private ShipmentCarrier carrierName;

        @JsonProperty("otherCarrierName")
        private String otherCarrierName;

        public ShipmentDetails(Integer noOfBoxes, ShipmentTransportType inboundType) {
            this.noOfBoxes = noOfBoxes;
            this.inboundType = inboundType;
        }

        public ShipmentDetails isOverSized(Boolean isOverSized) {
            this.isOverSized = isOverSized;
            return this;
        }

        public ShipmentDetails description(String description) {
            this.description = description;
            return this;
        }

        public ShipmentDetails trackingNumber(List<String> trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public ShipmentDetails carrierName(ShipmentCarrier carrierName) {
            this.carrierName = carrierName;
            return this;
        }

        public ShipmentDetails otherCarrierName(String otherCarrierName) {
            this.otherCarrierName = otherCarrierName;
            return this;
        }
    }
}
