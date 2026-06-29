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

/**
 * Trouble-related information for a trouble ticket order ({@code serviceDetails}).
 *
 * <p>{@code incidentDateTime} (cannot be in the future) and {@code problemCode} are required. The
 * {@code problemCode} is one of the codes returned by the trouble ticket types reference endpoint
 * (e.g. {@code net01}, {@code pwr01}, {@code ms13}). {@code serviceName} is mandatory for managed
 * services trouble tickets.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TroubleTicketServiceDetails {

    @JsonProperty("incidentDateTime")
    private final String incidentDateTime;

    @JsonProperty("problemCode")
    private final String problemCode;

    @JsonProperty("serviceName")
    private final String serviceName;

    @JsonProperty("callFromCage")
    private final Boolean callFromCage;

    @JsonProperty("needSupportFromASubmarineCableStationEngineer")
    private final Boolean needSupportFromASubmarineCableStationEngineer;

    @JsonProperty("additionalDetails")
    private final String additionalDetails;

    private TroubleTicketServiceDetails(Builder builder) {
        this.incidentDateTime = builder.incidentDateTime;
        this.problemCode = builder.problemCode;
        this.serviceName = builder.serviceName;
        this.callFromCage = builder.callFromCage;
        this.needSupportFromASubmarineCableStationEngineer = builder.needSupportFromASubmarineCableStationEngineer;
        this.additionalDetails = builder.additionalDetails;
    }

    /**
     * Returns a new builder for trouble ticket service details.
     *
     * @param incidentDateTime the date/time the issue occurred (cannot be in the future) (required)
     * @param problemCode      the problem code matching the trouble description (required)
     * @return a new builder
     */
    public static Builder builder(String incidentDateTime, String problemCode) {
        return new Builder(incidentDateTime, problemCode);
    }

    public static class Builder {
        private final String incidentDateTime;
        private final String problemCode;
        private String serviceName;
        private Boolean callFromCage;
        private Boolean needSupportFromASubmarineCableStationEngineer;
        private String additionalDetails;

        private Builder(String incidentDateTime, String problemCode) {
            this.incidentDateTime = incidentDateTime;
            this.problemCode = problemCode;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder callFromCage(Boolean callFromCage) {
            this.callFromCage = callFromCage;
            return this;
        }

        public Builder needSupportFromASubmarineCableStationEngineer(Boolean needSupport) {
            this.needSupportFromASubmarineCableStationEngineer = needSupport;
            return this;
        }

        public Builder additionalDetails(String additionalDetails) {
            this.additionalDetails = additionalDetails;
            return this;
        }

        public TroubleTicketServiceDetails build() {
            return new TroubleTicketServiceDetails(this);
        }
    }
}
