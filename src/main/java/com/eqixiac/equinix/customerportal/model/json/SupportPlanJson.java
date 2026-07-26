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

package com.eqixiac.equinix.customerportal.model.json;

import com.eqixiac.equinix.customerportal.enums.PlanFrequency;
import com.eqixiac.equinix.customerportal.enums.SupportPlanStatus;
import com.eqixiac.equinix.customerportal.model.SupportPlan;
import com.eqixiac.equinix.customerportal.model.implementation.SupportPlanAssignment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a Smart Hands support plan ({@code supports} schema, Support Plans v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportPlanJson implements SupportPlan {

    @JsonProperty("id")
    private String id;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("planName")
    private String planName;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("ibxSpecific")
    private Boolean ibxSpecific;

    @JsonProperty("planFrequency")
    private PlanFrequency planFrequency;

    @JsonProperty("purchasedMinutes")
    private Integer purchasedMinutes;

    @JsonProperty("assignedMinutes")
    private Integer assignedMinutes;

    @JsonProperty("consumedMinutes")
    private Integer consumedMinutes;

    @JsonProperty("remainingMinutes")
    private Integer remainingMinutes;

    @JsonProperty("previousConsumedMinutes")
    private Integer previousConsumedMinutes;

    @JsonProperty("currentConsumedMinutes")
    private Integer currentConsumedMinutes;

    @JsonProperty("prepaidConsumedMinutes")
    private Integer prepaidConsumedMinutes;

    @JsonProperty("transitionMinutes")
    private Integer transitionMinutes;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("status")
    private SupportPlanStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("assignment")
    private SupportPlanAssignment assignment;
}
