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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.customerportal.enums.WorkVisitStatus;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class WorkVisitJson implements Serializable {

    @Getter static TypeReference<Page<WorkVisit, WorkVisitJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<WorkVisitJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<WorkVisitJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("visitId")
    private String visitId;

    @JsonProperty("status")
    private WorkVisitStatus status;

    @JsonProperty("ibxCode")
    private String ibxCode;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("description")
    private String description;

    @JsonProperty("scheduleStartDate")
    private String scheduleStartDate;

    @JsonProperty("scheduleEndDate")
    private String scheduleEndDate;

    @JsonProperty("visitorCompany")
    private String visitorCompany;

    @JsonProperty("visitorName")
    private String visitorName;

    @JsonProperty("visitorEmail")
    private String visitorEmail;

    @JsonProperty("visitorPhone")
    private String visitorPhone;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
}
