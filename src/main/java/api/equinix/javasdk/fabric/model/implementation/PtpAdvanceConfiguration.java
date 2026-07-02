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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PTP advanced configuration for a Precision Time service (the Fabric v4
 * {@code ptpAdvanceConfiguration} schema). {@code timeScale} is {@code ARB} or {@code PTP};
 * {@code transportMode} is {@code MULTICAST}, {@code UNICAST} or {@code HYBRID}.
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PtpAdvanceConfiguration {

    @JsonProperty("timeScale")
    private String timeScale;

    @JsonProperty("domain")
    private Integer domain;

    @JsonProperty("priority1")
    private Integer priority1;

    @JsonProperty("priority2")
    private Integer priority2;

    @JsonProperty("logAnnounceInterval")
    private Integer logAnnounceInterval;

    @JsonProperty("logSyncInterval")
    private Integer logSyncInterval;

    @JsonProperty("logDelayReqInterval")
    private Integer logDelayReqInterval;

    @JsonProperty("transportMode")
    private String transportMode;

    @JsonProperty("grantTime")
    private Integer grantTime;
}
