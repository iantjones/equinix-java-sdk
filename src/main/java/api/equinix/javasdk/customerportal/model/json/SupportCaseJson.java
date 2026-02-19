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
import api.equinix.javasdk.customerportal.enums.CasePriority;
import api.equinix.javasdk.customerportal.enums.CaseStatus;
import api.equinix.javasdk.customerportal.model.SupportCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class SupportCaseJson implements Serializable {

    @Getter static TypeReference<Page<SupportCase, SupportCaseJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<SupportCaseJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<SupportCaseJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private CaseStatus status;

    @JsonProperty("priority")
    private CasePriority priority;

    @JsonProperty("caseNumber")
    private String caseNumber;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("updatedDate")
    private String updatedDate;
}
