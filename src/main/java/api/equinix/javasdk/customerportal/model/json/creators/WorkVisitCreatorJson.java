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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class WorkVisitCreatorJson {

    @JsonProperty("ibxCode") private String ibxCode;
    @JsonProperty("accountNumber") private String accountNumber;
    @JsonProperty("description") private String description;
    @JsonProperty("scheduleStartDate") private String scheduleStartDate;
    @JsonProperty("scheduleEndDate") private String scheduleEndDate;
    @JsonProperty("visitorCompany") private String visitorCompany;
    @JsonProperty("visitorName") private String visitorName;
    @JsonProperty("visitorEmail") private String visitorEmail;
    @JsonProperty("visitorPhone") private String visitorPhone;

    public WorkVisitCreatorJson(WorkVisitOperator.WorkVisitBuilder builder) {
        this.ibxCode = builder.getIbxCode();
        this.accountNumber = builder.getAccountNumber();
        this.description = builder.getDescription();
        this.scheduleStartDate = builder.getScheduleStartDate();
        this.scheduleEndDate = builder.getScheduleEndDate();
        this.visitorCompany = builder.getVisitorCompany();
        this.visitorName = builder.getVisitorName();
        this.visitorEmail = builder.getVisitorEmail();
        this.visitorPhone = builder.getVisitorPhone();
    }
}
