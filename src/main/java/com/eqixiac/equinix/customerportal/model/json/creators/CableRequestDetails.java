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

import com.eqixiac.equinix.customerportal.enums.SmartHandsConnectorType;
import com.eqixiac.equinix.customerportal.enums.SmartHandsMediaType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed {@code serviceDetails} for a smart hands request-cables order
 * ({@code cableRequestRequest.serviceDetails} in the smart hands v1 spec). Pass an instance to
 * {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>Required: {@code quantity} (one of {@code "1"}..{@code "10"} or {@code ">10"}) and
 * {@code scopeOfWork}. When {@code quantity} is {@code "1"}, {@code mediaType},
 * {@code connectorType} and {@code length} become mandatory.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CableRequestDetails {

    @JsonProperty("quantity")
    private final String quantity;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    @JsonProperty("mediaType")
    private SmartHandsMediaType mediaType;

    @JsonProperty("connectorType")
    private SmartHandsConnectorType connectorType;

    @JsonProperty("length")
    private String length;

    private CableRequestDetails(Builder builder) {
        this.quantity = builder.quantity;
        this.scopeOfWork = builder.scopeOfWork;
        this.mediaType = builder.mediaType;
        this.connectorType = builder.connectorType;
        this.length = builder.length;
    }

    /**
     * Returns a new builder for request-cables service details.
     *
     * @param quantity    the number of cables (required; e.g. {@code "1"}..{@code "10"}, {@code ">10"})
     * @param scopeOfWork the scope of work (required)
     * @return a new builder
     */
    public static Builder builder(String quantity, String scopeOfWork) {
        return new Builder(quantity, scopeOfWork);
    }

    public static class Builder {
        private final String quantity;
        private final String scopeOfWork;
        private SmartHandsMediaType mediaType;
        private SmartHandsConnectorType connectorType;
        private String length;

        private Builder(String quantity, String scopeOfWork) {
            this.quantity = quantity;
            this.scopeOfWork = scopeOfWork;
        }

        public Builder mediaType(SmartHandsMediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder connectorType(SmartHandsConnectorType connectorType) {
            this.connectorType = connectorType;
            return this;
        }

        public Builder length(String length) {
            this.length = length;
            return this;
        }

        public CableRequestDetails build() {
            return new CableRequestDetails(this);
        }
    }
}
