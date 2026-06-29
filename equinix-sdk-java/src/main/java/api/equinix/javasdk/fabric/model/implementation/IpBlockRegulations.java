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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Regulatory information required when requesting an Equinix-owned IP block (the addressing plans
 * and questionnaire answers mandated for APAC blocks).
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpBlockRegulations {

    @JsonProperty("addressingPlans")
    private List<IpBlockAddressingPlan> addressingPlans;

    @JsonProperty("questions")
    private IpBlockRegulationQuestions questions;

    public IpBlockRegulations(List<IpBlockAddressingPlan> addressingPlans, IpBlockRegulationQuestions questions) {
        this.addressingPlans = addressingPlans;
        this.questions = questions;
    }
}
