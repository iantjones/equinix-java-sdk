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
 * Request body for placing a trouble ticket order ({@code POST /v1/orders/troubleticket},
 * {@code TroubleTicketOrderRequest}).
 *
 * <p>{@code ibxLocation} (IBX, cage and cabinet detail), {@code serviceDetails} (incident time,
 * problem code and supporting information) and {@code contacts} (ordering, notification and
 * technical contacts) are required by the API. An optional {@code customerReferenceNumber} and a
 * list of previously uploaded {@code attachments} may also be supplied.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TroubleTicketOrderRequest {

    @JsonProperty("ibxLocation")
    private final IbxLocation ibxLocation;

    @JsonProperty("serviceDetails")
    private final TroubleTicketServiceDetails serviceDetails;

    @JsonProperty("contacts")
    private final List<TroubleTicketContact> contacts;

    @JsonProperty("customerReferenceNumber")
    private final String customerReferenceNumber;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    private TroubleTicketOrderRequest(Builder builder) {
        this.ibxLocation = builder.ibxLocation;
        this.serviceDetails = builder.serviceDetails;
        this.contacts = builder.contacts;
        this.customerReferenceNumber = builder.customerReferenceNumber;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a trouble ticket order request body.
     *
     * @param ibxLocation    the IBX/cage/cabinet location (required)
     * @param serviceDetails the trouble incident details (required)
     * @param contacts       the ordering, notification and technical contacts (required)
     * @return a new builder
     */
    public static Builder builder(IbxLocation ibxLocation, TroubleTicketServiceDetails serviceDetails,
                                  List<TroubleTicketContact> contacts) {
        return new Builder(ibxLocation, serviceDetails, contacts);
    }

    public static class Builder {
        private final IbxLocation ibxLocation;
        private final TroubleTicketServiceDetails serviceDetails;
        private final List<TroubleTicketContact> contacts;
        private String customerReferenceNumber;
        private List<OrderAttachment> attachments;

        private Builder(IbxLocation ibxLocation, TroubleTicketServiceDetails serviceDetails,
                        List<TroubleTicketContact> contacts) {
            this.ibxLocation = ibxLocation;
            this.serviceDetails = serviceDetails;
            this.contacts = contacts;
        }

        public Builder customerReferenceNumber(String customerReferenceNumber) {
            this.customerReferenceNumber = customerReferenceNumber;
            return this;
        }

        public Builder attachments(List<OrderAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public TroubleTicketOrderRequest build() {
            return new TroubleTicketOrderRequest(this);
        }
    }
}
