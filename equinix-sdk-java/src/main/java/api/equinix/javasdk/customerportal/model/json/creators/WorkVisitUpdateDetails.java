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
 * Details for updating a work visit ({@code workvisits_additional_info} in the work-visits v2
 * spec). All fields are optional; supply only those being changed.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitUpdateDetails {

    @JsonProperty("visitStartDateTime")
    private String visitStartDateTime;

    @JsonProperty("visitEndDateTime")
    private String visitEndDateTime;

    @JsonProperty("openCabinet")
    private Boolean openCabinet;

    public WorkVisitUpdateDetails visitStartDateTime(String visitStartDateTime) {
        this.visitStartDateTime = visitStartDateTime;
        return this;
    }

    public WorkVisitUpdateDetails visitEndDateTime(String visitEndDateTime) {
        this.visitEndDateTime = visitEndDateTime;
        return this;
    }

    public WorkVisitUpdateDetails openCabinet(Boolean openCabinet) {
        this.openCabinet = openCabinet;
        return this;
    }
}
