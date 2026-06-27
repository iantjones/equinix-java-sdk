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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ShipmentClientImpl;
import api.equinix.javasdk.customerportal.enums.CarrierType;
import api.equinix.javasdk.customerportal.enums.ShipmentType;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.json.ShipmentJson;
import api.equinix.javasdk.customerportal.model.wrappers.ShipmentWrapper;
import lombok.Getter;

public class ShipmentOperator extends ResourceImpl<Shipment> {

    @Getter
    private final Pageable<Shipment> serviceClient;

    public ShipmentOperator(Pageable<Shipment> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ShipmentBuilder create() {
        return new ShipmentBuilder();
    }

    @Getter
    public class ShipmentBuilder {
        private ShipmentType type;
        private CarrierType carrierType;
        private String trackingNumber;
        private String ibxCode;
        private String accountNumber;
        private String description;
        private String senderName;
        private String senderCompany;
        private String senderAddress;
        private String recipientName;
        private String recipientCompany;
        private Integer numberOfBoxes;
        private String estimatedArrivalDate;

        protected ShipmentBuilder() {
        }

        public ShipmentBuilder type(ShipmentType type) {
            this.type = type;
            return this;
        }

        public ShipmentBuilder carrierType(CarrierType carrierType) {
            this.carrierType = carrierType;
            return this;
        }

        public ShipmentBuilder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public ShipmentBuilder ibxCode(String ibxCode) {
            this.ibxCode = ibxCode;
            return this;
        }

        public ShipmentBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public ShipmentBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ShipmentBuilder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public ShipmentBuilder senderCompany(String senderCompany) {
            this.senderCompany = senderCompany;
            return this;
        }

        public ShipmentBuilder senderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
            return this;
        }

        public ShipmentBuilder recipientName(String recipientName) {
            this.recipientName = recipientName;
            return this;
        }

        public ShipmentBuilder recipientCompany(String recipientCompany) {
            this.recipientCompany = recipientCompany;
            return this;
        }

        public ShipmentBuilder numberOfBoxes(Integer numberOfBoxes) {
            this.numberOfBoxes = numberOfBoxes;
            return this;
        }

        public ShipmentBuilder estimatedArrivalDate(String estimatedArrivalDate) {
            this.estimatedArrivalDate = estimatedArrivalDate;
            return this;
        }

        public Shipment create() {
            ShipmentCreatorJson creatorJson = new ShipmentCreatorJson(this);
            ShipmentJson shipmentJson = ((ShipmentClientImpl) ShipmentOperator.this.getServiceClient()).create(creatorJson);
            return new ShipmentWrapper(shipmentJson, ShipmentOperator.this.getServiceClient());
        }
    }
}
