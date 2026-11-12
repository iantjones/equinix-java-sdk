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
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectScheduleAvailabilityType;
import com.eqixiac.equinix.fabric.beta.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cross Connect schedule window offered at an IBX.
 *
 * <p>Maps spec schema {@code CrossConnectScheduleAvailability}. Unknown properties are ignored on read.</p>
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
public class CrossConnectScheduleAvailability {

    /** Schedule URI. Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** Sub-resource type. &lt;br&gt; XC_INSTALL_SCHEDULE - Installation schedule windows. Read-only (server-populated). */
    @JsonProperty("type")
    private CrossConnectScheduleAvailabilityType type;

    /** Read-only (server-populated). */
    @JsonProperty("code")
    private ScheduleType code;

    /** A-Side of the schedule window. Read-only (server-populated). */
    @JsonProperty("aSide")
    private CrossConnectScheduleAvailabilityASide aSide;

    /** Z-Side of the schedule window. Read-only (server-populated). */
    @JsonProperty("zSide")
    private CrossConnectScheduleAvailabilityZSide zSide;

    /** Start of the schedule window. Read-only (server-populated). */
    @JsonProperty("startDateTime")
    private String startDateTime;

    /** End of the schedule window. Read-only (server-populated). */
    @JsonProperty("endDateTime")
    private String endDateTime;

    /** Dates within the schedule window when the IBX is unavailable, such as the holidays observed at the IBX. Read-only (server-populated). */
    @JsonProperty("unavailableDates")
    private List<String> unavailableDates;
}
