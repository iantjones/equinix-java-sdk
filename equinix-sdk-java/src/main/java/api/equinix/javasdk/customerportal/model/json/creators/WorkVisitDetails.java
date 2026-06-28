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
 * Details for scheduling a work visit ({@code details} of {@code Workvisit_Create} in the
 * work-visits v2 spec, the {@code allOf} of cages plus {@code workvisit_additional_details}).
 * {@code cages}, {@code visitStartDateTime}, {@code visitEndDateTime} and {@code visitors} are
 * required; {@code openCabinet} is optional (when {@code true}, Equinix staff open the secure
 * cabinet during the visit, which may incur an additional charge).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitDetails {

    @JsonProperty("cages")
    private final List<WorkVisitCage> cages;

    @JsonProperty("visitStartDateTime")
    private final String visitStartDateTime;

    @JsonProperty("visitEndDateTime")
    private final String visitEndDateTime;

    @JsonProperty("visitors")
    private final List<WorkVisitVisitor> visitors;

    @JsonProperty("openCabinet")
    private Boolean openCabinet;

    private WorkVisitDetails(Builder builder) {
        this.cages = builder.cages;
        this.visitStartDateTime = builder.visitStartDateTime;
        this.visitEndDateTime = builder.visitEndDateTime;
        this.visitors = builder.visitors;
        this.openCabinet = builder.openCabinet;
    }

    /**
     * Returns a new builder for work visit details.
     *
     * @param cages              the cages scheduled for the visit (required)
     * @param visitStartDateTime the requested start date/time, ISO 8601 UTC (required)
     * @param visitEndDateTime   the requested end date/time, ISO 8601 UTC (required)
     * @param visitors           the visitors (required)
     * @return a new builder
     */
    public static Builder builder(List<WorkVisitCage> cages, String visitStartDateTime, String visitEndDateTime,
                                  List<WorkVisitVisitor> visitors) {
        return new Builder(cages, visitStartDateTime, visitEndDateTime, visitors);
    }

    public static class Builder {
        private final List<WorkVisitCage> cages;
        private final String visitStartDateTime;
        private final String visitEndDateTime;
        private final List<WorkVisitVisitor> visitors;
        private Boolean openCabinet;

        private Builder(List<WorkVisitCage> cages, String visitStartDateTime, String visitEndDateTime,
                        List<WorkVisitVisitor> visitors) {
            this.cages = cages;
            this.visitStartDateTime = visitStartDateTime;
            this.visitEndDateTime = visitEndDateTime;
            this.visitors = visitors;
        }

        public Builder openCabinet(Boolean openCabinet) {
            this.openCabinet = openCabinet;
            return this;
        }

        public WorkVisitDetails build() {
            return new WorkVisitDetails(this);
        }
    }
}
