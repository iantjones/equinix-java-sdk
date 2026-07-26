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
 * Request body for submitting a pending inbound shipment (pending storage) order
 * ({@code POST /v1/orders/shipment/pendingStorage}, {@code pending-storage-request} in the
 * shipments v1 spec).
 *
 * <p>{@code shipmentDetails} and {@code contacts} are both required. Each detail entry moves one
 * stored shipment (by {@code storageId}) into a cage under an account.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingStorageOrderRequest {

    @JsonProperty("shipmentDetails")
    private final List<Detail> shipmentDetails;

    @JsonProperty("contacts")
    private final List<ContactInfo> contacts;

    /**
     * Creates a pending storage order request.
     *
     * @param shipmentDetails the stored shipments to deliver (required)
     * @param contacts        the ordering and notification contacts (required)
     */
    public PendingStorageOrderRequest(List<Detail> shipmentDetails, List<ContactInfo> contacts) {
        this.shipmentDetails = shipmentDetails;
        this.contacts = contacts;
    }

    /**
     * One stored shipment to deliver ({@code pending-storage-request.shipmentDetails} item).
     * {@code storageId}, {@code cage}, {@code accountNumber} and {@code deliverToCage} are
     * required.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Detail {

        @JsonProperty("storageId")
        private final String storageId;

        @JsonProperty("cage")
        private final String cage;

        @JsonProperty("accountNumber")
        private final String accountNumber;

        @JsonProperty("deliverToCage")
        private final Boolean deliverToCage;

        @JsonProperty("additionalDetails")
        private String additionalDetails;

        @JsonProperty("customerReferenceNumber")
        private String customerReferenceNumber;

        @JsonProperty("purchaseInformation")
        private String purchaseInformation;

        public Detail(String storageId, String cage, String accountNumber, Boolean deliverToCage) {
            this.storageId = storageId;
            this.cage = cage;
            this.accountNumber = accountNumber;
            this.deliverToCage = deliverToCage;
        }

        public Detail additionalDetails(String additionalDetails) {
            this.additionalDetails = additionalDetails;
            return this;
        }

        public Detail customerReferenceNumber(String customerReferenceNumber) {
            this.customerReferenceNumber = customerReferenceNumber;
            return this;
        }

        public Detail purchaseInformation(String purchaseInformation) {
            this.purchaseInformation = purchaseInformation;
            return this;
        }
    }
}
