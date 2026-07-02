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

import api.equinix.javasdk.customerportal.enums.FileType;
import api.equinix.javasdk.customerportal.model.Report;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * JSON model for a Report Center report.
 *
 * <p>Carries the union of the {@code downloadable-report} (list view, {@code getReports}) and
 * {@code report} (detail view, {@code getReportById}) schemas, so the same class deserializes both
 * operations. Fields absent from a given response remain {@code null}.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportJson implements Report {

    @Getter static TypeReference<List<ReportJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("reportId")
    private String reportId;

    @JsonProperty("scheduledId")
    private String scheduledId;

    @JsonProperty("reportName")
    private String reportName;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileType")
    private FileType fileType;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("createdFor")
    private String createdFor;

    @JsonProperty("requestedDate")
    private String requestedDate;

    @JsonProperty("generatedDate")
    private String generatedDate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("numberOfDownloads")
    private Integer numberOfDownloads;

    // ---- detail view ({@code report}) fields ----

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("location")
    private String location;

    @JsonProperty("numberOfAttempts")
    private Integer numberOfAttempts;

    @JsonProperty("lastAccessedBy")
    private String lastAccessedBy;

    @JsonProperty("lastAccessedDate")
    private String lastAccessedDate;

    @JsonProperty("publisherInfo")
    private Map<String, Object> publisherInfo;

    @JsonProperty("parameters")
    private List<ReportParameterJson> parameters;
}
