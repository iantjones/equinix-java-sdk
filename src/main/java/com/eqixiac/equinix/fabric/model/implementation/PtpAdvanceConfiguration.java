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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.PtpTimeScale;
import com.eqixiac.equinix.fabric.enums.PtpTransportMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PTP advanced configuration for a Precision Time service (the Fabric v4
 * {@code ptpAdvanceConfiguration} schema). {@code timeScale} is {@code ARB} or {@code PTP};
 * {@code transportMode} is {@code MULTICAST}, {@code UNICAST} or {@code HYBRID}.
 *
 * <p>Prefer {@code builder()} over the positional constructor — seven of the nine parameters
 * are {@code Integer}s, so builder construction is self-documenting and transposition-proof.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PtpAdvanceConfiguration {

    @JsonProperty("timeScale")
    private PtpTimeScale timeScale;

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
    private PtpTransportMode transportMode;

    @JsonProperty("grantTime")
    private Integer grantTime;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (seven same-typed {@code Integer} parameters)
     * rather than by field declaration order.
     *
     * @param timeScale           the PTP time scale ({@code ARB} or {@code PTP})
     * @param domain              the PTP domain
     * @param priority1           the PTP priority1 value
     * @param priority2           the PTP priority2 value
     * @param logAnnounceInterval the log announce interval
     * @param logSyncInterval     the log sync interval
     * @param logDelayReqInterval the log delay-request interval
     * @param transportMode       the transport mode ({@code MULTICAST}, {@code UNICAST} or {@code HYBRID})
     * @param grantTime           the unicast grant time
     */
    @Builder
    public PtpAdvanceConfiguration(PtpTimeScale timeScale, Integer domain, Integer priority1,
                                   Integer priority2, Integer logAnnounceInterval, Integer logSyncInterval,
                                   Integer logDelayReqInterval, PtpTransportMode transportMode,
                                   Integer grantTime) {
        this.timeScale = timeScale;
        this.domain = domain;
        this.priority1 = priority1;
        this.priority2 = priority2;
        this.logAnnounceInterval = logAnnounceInterval;
        this.logSyncInterval = logSyncInterval;
        this.logDelayReqInterval = logDelayReqInterval;
        this.transportMode = transportMode;
        this.grantTime = grantTime;
    }
}
