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
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Create Cross Connect request. A single request body supports all Cross Connect connection types - patch panel to patch panel, connection with LOA and connection to a BMMR (by Equinix or by Self). Refer to the request examples for each use case.
 *
 * <p>Maps spec schema {@code CreateCrossConnect}. Unknown properties are ignored on read.</p>
 * <p>Spec-required properties: {@code aSide}, {@code name}, {@code type}, {@code zSide}.</p>
 * <p>Request-capable: every property has a chained setter ({@code setX(...)} returns this), so an
 * instance doubles as its own builder.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CreateCrossConnect {

    /** Type of Cross Connect. Required by the spec. */
    @JsonProperty("type")
    private CrossConnectType type;

    /** A short, descriptive name for this Cross Connect. Required by the spec. */
    @JsonProperty("name")
    private String name;

    /** Required when connecting to a BMMR. &lt;br&gt; Use EQUINIX when BMMR Extension is required and Equinix installs it, or SELF when you install it yourself. */
    @JsonProperty("bmmrType")
    private BmmrType bmmrType;

    /** Circuit identifier of the Cross Connect. */
    @JsonProperty("circuitId")
    private String circuitId;

    /** Provide it to place a diverse Cross Connect against an existing one - set group to the redundancy group id or the serial number of the existing Cross Connect, and priority to SECONDARY. */
    @JsonProperty("redundancy")
    private CrossConnectRedundancy redundancy;

    /** A-Side connection details. Required by the spec. */
    @JsonProperty("aSide")
    private CrossConnectASideRequest aSide;

    /** Z-Side connection details. &lt;br&gt; Provide the DemarcationPoint, the LOA reference or the BMMR location depending on the Cross Connect being requested. Required by the spec. */
    @JsonProperty("zSide")
    private CrossConnectZSideRequest zSide;

    /** Preferred installation schedule. When omitted, the Cross Connect is scheduled with the STANDARD schedule code. */
    @JsonProperty("schedule")
    private ScheduleRequest schedule;

    /** Cross Connect Services. */
    @JsonProperty("services")
    private List<CrossConnectServiceRequest> services;

    /** Preferences for notifications on Cross Connect configuration or status changes. */
    @JsonProperty("notifications")
    private List<CrossConnectNotification> notifications;

    /** Cross Connect Order. */
    @JsonProperty("order")
    private OrderRequest order;

    /** Additional Note. */
    @JsonProperty("notes")
    private List<CrossConnectNoteRequest> notes;

    /** Additional Attachments. */
    @JsonProperty("attachments")
    private List<CrossConnectAttachment> attachments;
}
