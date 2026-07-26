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

import com.eqixiac.equinix.customerportal.enums.SmartHandsCameraProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed {@code serviceDetails} for a smart hands pictures/document order
 * ({@code picturesDocumentRequest.serviceDetails} in the smart hands v1 spec). Pass an instance to
 * {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>Required: {@code documentOnly} and {@code scopeOfWork}. When {@code documentOnly} is
 * {@code false}, {@code cameraProvidedBy}, {@code specificDateAndTime} and {@code description}
 * become mandatory.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PicturesDocumentDetails {

    @JsonProperty("documentOnly")
    private final Boolean documentOnly;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    @JsonProperty("cameraProvidedBy")
    private SmartHandsCameraProvider cameraProvidedBy;

    @JsonProperty("specificDateAndTime")
    private Boolean specificDateAndTime;

    @JsonProperty("description")
    private String description;

    private PicturesDocumentDetails(Builder builder) {
        this.documentOnly = builder.documentOnly;
        this.scopeOfWork = builder.scopeOfWork;
        this.cameraProvidedBy = builder.cameraProvidedBy;
        this.specificDateAndTime = builder.specificDateAndTime;
        this.description = builder.description;
    }

    /**
     * Returns a new builder for pictures/document service details.
     *
     * @param documentOnly whether only documents are required (required)
     * @param scopeOfWork  the scope of work (required)
     * @return a new builder
     */
    public static Builder builder(Boolean documentOnly, String scopeOfWork) {
        return new Builder(documentOnly, scopeOfWork);
    }

    public static class Builder {
        private final Boolean documentOnly;
        private final String scopeOfWork;
        private SmartHandsCameraProvider cameraProvidedBy;
        private Boolean specificDateAndTime;
        private String description;

        private Builder(Boolean documentOnly, String scopeOfWork) {
            this.documentOnly = documentOnly;
            this.scopeOfWork = scopeOfWork;
        }

        public Builder cameraProvidedBy(SmartHandsCameraProvider cameraProvidedBy) {
            this.cameraProvidedBy = cameraProvidedBy;
            return this;
        }

        public Builder specificDateAndTime(Boolean specificDateAndTime) {
            this.specificDateAndTime = specificDateAndTime;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public PicturesDocumentDetails build() {
            return new PicturesDocumentDetails(this);
        }
    }
}
