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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a Reports v1 report definition ({@code reportRequest}); returned by
 * {@code GET /v1/reportCenter/reports/definitions} and
 * {@code GET /v1/reportCenter/reports/definitions/{reportName}}. Read-only.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportDefinitionJson {

    @JsonProperty("name")
    private String name;

    @JsonProperty("parameters")
    private List<ReportParameterJson> parameters;

    @JsonProperty("scheduleType")
    private String scheduleType;

    @JsonProperty("period")
    private String period;

    @JsonProperty("control")
    private String control;

    @JsonProperty("categories")
    private List<String> categories;
}
