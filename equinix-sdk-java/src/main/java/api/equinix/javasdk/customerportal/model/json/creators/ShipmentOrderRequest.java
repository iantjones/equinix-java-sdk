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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Request body for scheduling an inbound or outbound shipment
 * ({@code POST /colocations/v2/orders/shipments}, {@code Shipment_Create}).
 *
 * <p>{@code type} ({@code INBOUND}/{@code OUTBOUND}), {@code requestedDateTime}, {@code cageId} and
 * {@code details} are required. {@code details} is one of {@code InboundShipments_create} or
 * {@code OutboundShipments_create}; because the shape varies it is supplied as a free-form map.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentOrderRequest {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("requestedDateTime")
    private final String requestedDateTime;

    @JsonProperty("cageId")
    private final String cageId;

    @JsonProperty("details")
    private final Map<String, Object> details;

    @JsonProperty("accountNumber")
    private final String accountNumber;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("customerReferenceId")
    private final String customerReferenceId;

    @JsonProperty("purchaseOrder")
    private final OrderPurchaseOrder purchaseOrder;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    @JsonProperty("contacts")
    private final List<OrderContact> contacts;

    private ShipmentOrderRequest(Builder builder) {
        this.type = builder.type;
        this.requestedDateTime = builder.requestedDateTime;
        this.cageId = builder.cageId;
        this.details = builder.details;
        this.accountNumber = builder.accountNumber;
        this.description = builder.description;
        this.customerReferenceId = builder.customerReferenceId;
        this.purchaseOrder = builder.purchaseOrder;
        this.attachments = builder.attachments;
        this.contacts = builder.contacts;
    }

    /**
     * Returns a new builder for a shipment order request.
     *
     * @param type              the shipment type ({@code INBOUND}/{@code OUTBOUND}) (required)
     * @param requestedDateTime the requested date/time (required)
     * @param cageId            the cage id (required)
     * @param details           the inbound/outbound shipment details (required)
     * @return a new builder
     */
    public static Builder builder(String type, String requestedDateTime, String cageId, Map<String, Object> details) {
        return new Builder(type, requestedDateTime, cageId, details);
    }

    public static class Builder {
        private final String type;
        private final String requestedDateTime;
        private final String cageId;
        private final Map<String, Object> details;
        private String accountNumber;
        private String description;
        private String customerReferenceId;
        private OrderPurchaseOrder purchaseOrder;
        private List<OrderAttachment> attachments;
        private List<OrderContact> contacts;

        private Builder(String type, String requestedDateTime, String cageId, Map<String, Object> details) {
            this.type = type;
            this.requestedDateTime = requestedDateTime;
            this.cageId = cageId;
            this.details = details;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public Builder purchaseOrder(OrderPurchaseOrder purchaseOrder) {
            this.purchaseOrder = purchaseOrder;
            return this;
        }

        public Builder attachments(List<OrderAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder contacts(List<OrderContact> contacts) {
            this.contacts = contacts;
            return this;
        }

        public ShipmentOrderRequest build() {
            return new ShipmentOrderRequest(this);
        }
    }
}
