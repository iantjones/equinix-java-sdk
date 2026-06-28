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
 * A cage scheduled for a work visit ({@code Cages_details} in the work-visits v2 spec). {@code id}
 * is required; {@code accountNumber} and {@code cabinetId} are optional (provide a cabinet id to
 * limit access to a specific cabinet).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitCage {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("cabinetId")
    private String cabinetId;

    public WorkVisitCage(String id) {
        this.id = id;
    }

    public WorkVisitCage accountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public WorkVisitCage cabinetId(String cabinetId) {
        this.cabinetId = cabinetId;
        return this;
    }
}
