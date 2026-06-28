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
 * Request body for updating an inbound or outbound shipment order
 * ({@code PATCH /colocations/v2/orders/shipments/{orderId}}, {@code Shipment_Modify_request}).
 * All fields are optional; supply only those being changed.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentUpdateRequest {

    @JsonProperty("contacts")
    private final List<OrderContact> contacts;

    @JsonProperty("requestedDateTime")
    private final String requestedDateTime;

    @JsonProperty("details")
    private final Map<String, Object> details;

    private ShipmentUpdateRequest(Builder builder) {
        this.contacts = builder.contacts;
        this.requestedDateTime = builder.requestedDateTime;
        this.details = builder.details;
    }

    /**
     * Returns a new builder for a shipment update request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<OrderContact> contacts;
        private String requestedDateTime;
        private Map<String, Object> details;

        private Builder() {
        }

        public Builder contacts(List<OrderContact> contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder requestedDateTime(String requestedDateTime) {
            this.requestedDateTime = requestedDateTime;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public ShipmentUpdateRequest build() {
            return new ShipmentUpdateRequest(this);
        }
    }
}
