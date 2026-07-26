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
 * Request body for scheduling a work visit
 * ({@code POST /colocations/v2/orders/workVisits}, {@code Workvisit_Create}).
 *
 * <p>{@code details} is required and carries the cages, visit start/end times and visitors; supply
 * the typed {@link WorkVisitDetails} creator. The {@code details} parameter is typed as
 * {@link Object} to accept the typed creator or, as an escape hatch, a free-form
 * {@code Map<String, Object>}. The remaining fields are optional.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitOrderRequest {

    @JsonProperty("details")
    private final Object details;

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

    private WorkVisitOrderRequest(Builder builder) {
        this.details = builder.details;
        this.description = builder.description;
        this.customerReferenceId = builder.customerReferenceId;
        this.purchaseOrder = builder.purchaseOrder;
        this.attachments = builder.attachments;
        this.contacts = builder.contacts;
    }

    /**
     * Returns a new builder for a work visit order request.
     *
     * @param details the work visit details (cages, times, visitors) (required); pass a
     *                {@link WorkVisitDetails} or a free-form {@code Map<String, Object>}
     * @return a new builder
     */
    public static Builder builder(Object details) {
        return new Builder(details);
    }

    public static class Builder {
        private final Object details;
        private String description;
        private String customerReferenceId;
        private OrderPurchaseOrder purchaseOrder;
        private List<OrderAttachment> attachments;
        private List<OrderContact> contacts;

        private Builder(Object details) {
            this.details = details;
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

        public WorkVisitOrderRequest build() {
            return new WorkVisitOrderRequest(this);
        }
    }
}
