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
 * Request body for creating a trouble ticket
 * ({@code POST /v2/tickets}, {@code Tickets_Create}).
 *
 * <p>{@code code}, {@code description}, {@code occurredDateTime} and {@code primaryId} are
 * required. {@code details} is category-specific (one of many {@code anyOf} schemas), so it is
 * supplied as a free-form map.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TroubleTicketCreateRequest {

    @JsonProperty("code")
    private final String code;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("occurredDateTime")
    private final String occurredDateTime;

    @JsonProperty("primaryId")
    private final String primaryId;

    @JsonProperty("secondaryId")
    private final String secondaryId;

    @JsonProperty("customerReferenceId")
    private final String customerReferenceId;

    @JsonProperty("details")
    private final Map<String, Object> details;

    @JsonProperty("contacts")
    private final List<OrderContact> contacts;

    @JsonProperty("attachments")
    private final List<OrderAttachment> attachments;

    private TroubleTicketCreateRequest(Builder builder) {
        this.code = builder.code;
        this.description = builder.description;
        this.occurredDateTime = builder.occurredDateTime;
        this.primaryId = builder.primaryId;
        this.secondaryId = builder.secondaryId;
        this.customerReferenceId = builder.customerReferenceId;
        this.details = builder.details;
        this.contacts = builder.contacts;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a trouble ticket create request.
     *
     * @param code             the trouble ticket code (required)
     * @param description      the issue description (required)
     * @param occurredDateTime when the issue occurred (required)
     * @param primaryId        the primary identifier (e.g. asset/cage id) (required)
     * @return a new builder
     */
    public static Builder builder(String code, String description, String occurredDateTime, String primaryId) {
        return new Builder(code, description, occurredDateTime, primaryId);
    }

    public static class Builder {
        private final String code;
        private final String description;
        private final String occurredDateTime;
        private final String primaryId;
        private String secondaryId;
        private String customerReferenceId;
        private Map<String, Object> details;
        private List<OrderContact> contacts;
        private List<OrderAttachment> attachments;

        private Builder(String code, String description, String occurredDateTime, String primaryId) {
            this.code = code;
            this.description = description;
            this.occurredDateTime = occurredDateTime;
            this.primaryId = primaryId;
        }

        public Builder secondaryId(String secondaryId) {
            this.secondaryId = secondaryId;
            return this;
        }

        public Builder customerReferenceId(String customerReferenceId) {
            this.customerReferenceId = customerReferenceId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
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

        public TroubleTicketCreateRequest build() {
            return new TroubleTicketCreateRequest(this);
        }
    }
}
