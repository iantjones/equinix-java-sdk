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

import java.util.List;

/**
 * Request body for submitting an inbound shipment order
 * ({@code POST /v1/orders/shipment/inbound}, {@code inboundShipmentRequest} in the shipments v1
 * spec).
 *
 * <p>{@code ibxLocation}, {@code contacts} and {@code serviceDetails} are required. Ordering and
 * notification contacts are registered customer-portal users referenced by user name; a technical
 * contact is optional and specified inline. Attachments reference previously uploaded files by id
 * and name.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboundShipmentOrderRequest {

    @JsonProperty("ibxLocation")
    private final IbxLocation ibxLocation;

    @JsonProperty("contacts")
    private final List<ContactInfo> contacts;

    @JsonProperty("serviceDetails")
    private final InboundShipmentServiceDetails serviceDetails;

    @JsonProperty("customerReferenceNumber")
    private final String customerReferenceNumber;

    @JsonProperty("purchaseOrder")
    private final ShipmentPurchaseOrder purchaseOrder;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    private InboundShipmentOrderRequest(Builder builder) {
        this.ibxLocation = builder.ibxLocation;
        this.contacts = builder.contacts;
        this.serviceDetails = builder.serviceDetails;
        this.customerReferenceNumber = builder.customerReferenceNumber;
        this.purchaseOrder = builder.purchaseOrder;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for an inbound shipment order request.
     *
     * @param ibxLocation    the IBX/cage location (required)
     * @param contacts       the ordering, notification and optional technical contacts (required)
     * @param serviceDetails the inbound shipment service details (required)
     * @return a new builder
     */
    public static Builder builder(IbxLocation ibxLocation, List<ContactInfo> contacts,
                                  InboundShipmentServiceDetails serviceDetails) {
        return new Builder(ibxLocation, contacts, serviceDetails);
    }

    public static class Builder {
        private final IbxLocation ibxLocation;
        private final List<ContactInfo> contacts;
        private final InboundShipmentServiceDetails serviceDetails;
        private String customerReferenceNumber;
        private ShipmentPurchaseOrder purchaseOrder;
        private List<OrderAttachment> attachments;

        private Builder(IbxLocation ibxLocation, List<ContactInfo> contacts,
                        InboundShipmentServiceDetails serviceDetails) {
            this.ibxLocation = ibxLocation;
            this.contacts = contacts;
            this.serviceDetails = serviceDetails;
        }

        public Builder customerReferenceNumber(String customerReferenceNumber) {
            this.customerReferenceNumber = customerReferenceNumber;
            return this;
        }

        public Builder purchaseOrder(ShipmentPurchaseOrder purchaseOrder) {
            this.purchaseOrder = purchaseOrder;
            return this;
        }

        public Builder attachments(List<OrderAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public InboundShipmentOrderRequest build() {
            return new InboundShipmentOrderRequest(this);
        }
    }
}
