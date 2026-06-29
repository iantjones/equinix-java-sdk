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

/**
 * Request body for updating a work visit order
 * ({@code PATCH /colocations/v2/orders/workVisits/{orderId}}, {@code Modify_request}). All fields
 * are optional; {@code details} carries {@code visitStartDateTime}, {@code visitEndDateTime} and
 * {@code openCabinet} — supply the typed {@link WorkVisitUpdateDetails} creator or, as an escape
 * hatch, a free-form {@code Map<String, Object>}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitUpdateRequest {

    @JsonProperty("contacts")
    private final List<ContactUpdate> contacts;

    @JsonProperty("details")
    private final Object details;

    private WorkVisitUpdateRequest(Builder builder) {
        this.contacts = builder.contacts;
        this.details = builder.details;
    }

    /**
     * Returns a new builder for a work visit update request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ContactUpdate> contacts;
        private Object details;

        private Builder() {
        }

        public Builder contacts(List<ContactUpdate> contacts) {
            this.contacts = contacts;
            return this;
        }

        /**
         * Sets the work visit update details. Pass a {@link WorkVisitUpdateDetails} or a free-form
         * {@code Map<String, Object>}.
         *
         * @param details the work visit update details
         * @return this builder
         */
        public Builder details(Object details) {
            this.details = details;
            return this;
        }

        public WorkVisitUpdateRequest build() {
            return new WorkVisitUpdateRequest(this);
        }
    }
}
