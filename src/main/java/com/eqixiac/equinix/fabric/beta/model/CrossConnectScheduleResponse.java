/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.model;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectScheduleResponseType;
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectScheduleState;
import com.eqixiac.equinix.fabric.beta.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Schedule of a Cross Connect.
 *
 * <p>Maps spec schema {@code CrossConnectScheduleResponse}. Unknown properties are ignored on read.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CrossConnectScheduleResponse {

    /** Schedule URI. Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** Schedule Identifier. Read-only (server-populated). */
    @JsonProperty("uuid")
    private String uuid;

    /** Sub-resource type. &lt;br&gt; XC_INSTALL_SCHEDULE - Installation schedule. &lt;br&gt; XC_DEINSTALL_SCHEDULE - Deinstallation schedule. Read-only (server-populated). */
    @JsonProperty("type")
    private CrossConnectScheduleResponseType type;

    /** Read-only (server-populated). */
    @JsonProperty("code")
    private ScheduleType code;

    /** Read-only (server-populated). */
    @JsonProperty("state")
    private CrossConnectScheduleState state;

    /** Indicates whether the Cross Connect is scheduled as STANDARD when no EXPEDITE scheduling is available. Returned only for EXPEDITE schedule. Read-only (server-populated). */
    @JsonProperty("autoConvertToStandard")
    private Boolean autoConvertToStandard;

    /** Carrier Circuit delivery date is the date when the Carrier commits to enable the circuit. Your order will be placed on hold, pending the date you enter. Equinix will wait for the Carrier to deliver the service and then will complete the installation. Read-only (server-populated). */
    @JsonProperty("readinessDateTime")
    private String readinessDateTime;

    /** Requested Installation Date and Time. Read-only (server-populated). */
    @JsonProperty("requestedDateTime")
    private String requestedDateTime;

    /** Date and Time proposed by Equinix when the EXPEDITE schedule is not available. Read-only (server-populated). */
    @JsonProperty("equinixProposedDateTime")
    private String equinixProposedDateTime;

    /** Installation Date and Time. Read-only (server-populated). */
    @JsonProperty("installationDateTime")
    private String installationDateTime;

    /** Deinstallation Date and Time. Returned for a removal schedule. Read-only (server-populated). */
    @JsonProperty("deinstallationDateTime")
    private String deinstallationDateTime;

    /** Cross Connect Change Log. */
    @JsonProperty("changeLog")
    private CrossConnectChangelog changeLog;
}
