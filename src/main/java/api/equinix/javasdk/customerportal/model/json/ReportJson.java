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
import api.equinix.javasdk.customerportal.model.Report;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class ReportJson implements Serializable, Report {

    @Getter static TypeReference<Page<Report, ReportJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<ReportJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ReportJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("reportName")
    private String reportName;

    @JsonProperty("reportType")
    private String reportType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("generatedDate")
    private String generatedDate;

    @JsonProperty("fileFormat")
    private String fileFormat;
}
