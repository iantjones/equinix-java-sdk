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
 * Request body for creating a trouble ticket / support case. The ticket {@code code} (identifying
 * the support category) and a {@code description} of the issue are required; all other fields are
 * optional. {@code attachments} reference previously uploaded files by id; the {@code details} and
 * {@code contacts} blocks, whose shape varies by ticket code, are modelled as free-form maps.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportCaseCreateRequest {

    @JsonProperty("code")
    private final String code;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("primaryId")
    private final String primaryId;

    @JsonProperty("secondaryId")
    private final String secondaryId;

    @JsonProperty("occurredDateTime")
    private final String occurredDateTime;

    @JsonProperty("customerReferenceId")
    private final String customerReferenceId;

    @JsonProperty("details")
    private final Map<String, Object> details;

    @JsonProperty("contacts")
    private final List<Map<String, Object>> contacts;

    @JsonProperty("attachments")
    private final List<SupportCaseAttachment> attachments;

    private SupportCaseCreateRequest(Builder builder) {
        this.code = builder.code;
        this.description = builder.description;
        this.primaryId = builder.primaryId;
        this.secondaryId = builder.secondaryId;
        this.occurredDateTime = builder.occurredDateTime;
        this.customerReferenceId = builder.customerReferenceId;
        this.details = builder.details;
        this.contacts = builder.contacts;
        this.attachments = builder.attachments;
    }

    /**
     * Returns a new builder for a support case create request body.
     *
     * @param code        the ticket code identifying the support category (required)
     * @param description the description of the trouble or issue (required)
     * @return a new builder
     */
    public static Builder builder(String code, String description) {
        return new Builder(code, description);
    }

    public static class Builder {
        private final String code;
        private final String description;
        private String primaryId;
        private String secondaryId;
        private String occurredDateTime;
        private String customerReferenceId;
        private Map<String, Object> details;
        private List<Map<String, Object>> contacts;
        private List<SupportCaseAttachment> attachments;

        private Builder(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public Builder primaryId(String primaryId) {
            this.primaryId = primaryId;
            return this;
        }

        public Builder secondaryId(String secondaryId) {
            this.secondaryId = secondaryId;
            return this;
        }

        public Builder occurredDateTime(String occurredDateTime) {
            this.occurredDateTime = occurredDateTime;
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

        public Builder contacts(List<Map<String, Object>> contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder attachments(List<SupportCaseAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public SupportCaseCreateRequest build() {
            return new SupportCaseCreateRequest(this);
        }
    }
}
