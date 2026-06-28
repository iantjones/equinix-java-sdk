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
 * Request body for placing a cross-connect order
 * ({@code POST /colocations/v2/orders/crossConnects}, {@code Layer1_Create}).
 *
 * <p>{@code details} is required and carries one entry per cross-connect, each with an
 * {@code aSide}/{@code zSide}. The recommended (typed) path is
 * {@link #builder(List)} with a list of {@link Layer1Detail}; for forward compatibility a raw
 * {@link #builderRaw(List)} escape hatch accepting a list of free-form maps is also provided.</p>
 *
 * <p>{@code customerReferenceId}, {@code description}, {@code expediteDateTime},
 * {@code purchaseOrder}, {@code contacts} and {@code attachments} are optional.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrossConnectOrderRequest {

    @JsonProperty("details")
    private final Object details;

    @JsonProperty("customerReferenceId")
    private final String customerReferenceId;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("expediteDateTime")
    private final String expediteDateTime;

    @JsonProperty("purchaseOrder")
    private final OrderPurchaseOrder purchaseOrder;

    @JsonProperty("contacts")
    private final List<OrderContact> contacts;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    private CrossConnectOrderRequest(Builder builder) {
        this.details = builder.details;
        this.customerReferenceId = builder.customerReferenceId;
        this.description = builder.description;
        this.expediteDateTime = builder.expediteDateTime;
        this.purchaseOrder = builder.purchaseOrder;
        this.contacts = builder.contacts;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a cross-connect order request using typed details (recommended).
     *
     * @param details the per-cross-connect details (required)
     * @return a new builder
     */
    public static Builder builder(List<Layer1Detail> details) {
        return new Builder(details);
    }

    /**
     * Returns a new builder for a cross-connect order request using raw, free-form details. This
     * escape hatch is provided for forward compatibility; prefer {@link #builder(List)}.
     *
     * @param rawDetails the per-cross-connect details as free-form maps (required)
     * @return a new builder
     */
    public static Builder builderRaw(List<Map<String, Object>> rawDetails) {
        return new Builder((Object) rawDetails);
    }

    public static class Builder {
        private final Object details;
        private String customerReferenceId;
        private String description;
        private String expediteDateTime;
        private OrderPurchaseOrder purchaseOrder;
        private List<OrderContact> contacts;
        private List<OrderAttachment> attachments;

        private Builder(List<Layer1Detail> details) {
            this.details = details;
        }

        private Builder(Object details) {
            this.details = details;
        }

        public Builder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder expediteDateTime(String expediteDateTime) {
            this.expediteDateTime = expediteDateTime;
            return this;
        }

        public Builder purchaseOrder(OrderPurchaseOrder purchaseOrder) {
            this.purchaseOrder = purchaseOrder;
            return this;
        }

        public Builder contacts(List<OrderContact> contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder attachments(List<OrderAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public CrossConnectOrderRequest build() {
            return new CrossConnectOrderRequest(this);
        }
    }
}
