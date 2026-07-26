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
import java.util.Map;

/**
 * Request body for placing a cross-connect deinstallation order
 * ({@code POST /colocations/v2/orders/crossConnects/deinstall}, {@code Layer1_Deinstall_request}).
 *
 * <p>{@code details} (one entry per asset to deinstall) and {@code removalDate} are required. The
 * recommended (typed) path is {@link #builder(List, String)} with a list of
 * {@link Layer1DeinstallDetail}; for forward compatibility a raw
 * {@link #builderRaw(List, String)} escape hatch accepting a list of free-form maps is also
 * provided.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrossConnectDeinstallRequest {

    @JsonProperty("details")
    private final Object details;

    @JsonProperty("removalDate")
    private final String removalDate;

    @JsonProperty("customerReferenceId")
    private final String customerReferenceId;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("purchaseOrder")
    private final OrderPurchaseOrder purchaseOrder;

    @JsonProperty("contacts")
    private final List<OrderContact> contacts;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    private CrossConnectDeinstallRequest(Builder builder) {
        this.details = builder.details;
        this.removalDate = builder.removalDate;
        this.customerReferenceId = builder.customerReferenceId;
        this.description = builder.description;
        this.purchaseOrder = builder.purchaseOrder;
        this.contacts = builder.contacts;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a cross-connect deinstallation request using typed details
     * (recommended).
     *
     * @param details     the per-asset deinstall details (required)
     * @param removalDate the requested removal date (required)
     * @return a new builder
     */
    public static Builder builder(List<Layer1DeinstallDetail> details, String removalDate) {
        return new Builder(details, removalDate);
    }

    /**
     * Returns a new builder for a cross-connect deinstallation request using raw, free-form
     * details. This escape hatch is provided for forward compatibility; prefer
     * {@link #builder(List, String)}.
     *
     * @param rawDetails  the per-asset deinstall details as free-form maps (required)
     * @param removalDate the requested removal date (required)
     * @return a new builder
     */
    public static Builder builderRaw(List<Map<String, Object>> rawDetails, String removalDate) {
        return new Builder((Object) rawDetails, removalDate);
    }

    public static class Builder {
        private final Object details;
        private final String removalDate;
        private String customerReferenceId;
        private String description;
        private OrderPurchaseOrder purchaseOrder;
        private List<OrderContact> contacts;
        private List<OrderAttachment> attachments;

        private Builder(List<Layer1DeinstallDetail> details, String removalDate) {
            this.details = details;
            this.removalDate = removalDate;
        }

        private Builder(Object details, String removalDate) {
            this.details = details;
            this.removalDate = removalDate;
        }

        public Builder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public CrossConnectDeinstallRequest build() {
            return new CrossConnectDeinstallRequest(this);
        }
    }
}
