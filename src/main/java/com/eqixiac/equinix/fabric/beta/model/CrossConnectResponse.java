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
import com.eqixiac.equinix.fabric.beta.enums.BmmrType;
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectState;
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cross Connect Response.
 *
 * <p>Maps spec schema {@code CrossConnectResponse}. Unknown properties are ignored on read.</p>
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
public class CrossConnectResponse {

    /** Cross Connect URI. */
    @JsonProperty("href")
    private String href;

    /** Type of Cross Connect. */
    @JsonProperty("type")
    private CrossConnectType type;

    /** Cross Connect Identifier. */
    @JsonProperty("uuid")
    private String uuid;

    /** A short, descriptive name for this Cross Connect. */
    @JsonProperty("name")
    private String name;

    /** Cross Connect State. */
    @JsonProperty("state")
    private CrossConnectState state;

    /** List of actions that can be performed on the Cross Connect based on its current state. Use these values to determine which action types are valid to submit to the actions endpoint. Read-only (server-populated). */
    @JsonProperty("allowedActions")
    private List<CrossConnectResponseAllowedActions> allowedActions;

    /** Cross Connect BMMR Type. */
    @JsonProperty("bmmrType")
    private BmmrType bmmrType;

    /** Media type of the Cross Connect. */
    @JsonProperty("mediaType")
    private String mediaType;

    /** Media sub-type of the Cross Connect. */
    @JsonProperty("mediaSubType")
    private String mediaSubType;

    /** Circuit identifier of the Cross Connect. */
    @JsonProperty("circuitId")
    private String circuitId;

    /** Serial number of the Cross Connect. Read-only (server-populated). */
    @JsonProperty("serialNumber")
    private String serialNumber;

    /** Cross Connect Redundancy. */
    @JsonProperty("redundancy")
    private CrossConnectRedundancy redundancy;

    /** A-Side connection details. */
    @JsonProperty("aSide")
    private CrossConnectASideResponse aSide;

    /** Z-Side connection details. */
    @JsonProperty("zSide")
    private CrossConnectZSideResponse zSide;

    /** Account the Cross Connect belongs to. */
    @JsonProperty("account")
    private CrossConnectAccount account;

    /** Preferences for notifications on Cross Connect configuration or status changes. */
    @JsonProperty("notifications")
    private List<CrossConnectNotification> notifications;

    /** Cross Connect Order. */
    @JsonProperty("order")
    private OrderResponse order;

    /** Links to the Cross Connect sub-resources - schedules, services, notes and attachments. Read-only (server-populated). */
    @JsonProperty("links")
    private List<CrossConnectLink> links;

    /** Cross Connect Change Log. */
    @JsonProperty("changeLog")
    private CrossConnectChangelog changeLog;
}
