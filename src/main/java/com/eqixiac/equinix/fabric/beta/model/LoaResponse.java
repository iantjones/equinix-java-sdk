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
import com.eqixiac.equinix.fabric.beta.enums.LoaProductType;
import com.eqixiac.equinix.fabric.beta.enums.LoaState;
import com.eqixiac.equinix.fabric.beta.enums.LoaType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Loa Response
 *
 * <p>Maps spec schema {@code LoaResponse}. Unknown properties are ignored on read.</p>
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
public class LoaResponse {

    /** URI of this LOA resource. */
    @JsonProperty("href")
    private String href;

    /** Unique identifier of this LOA. */
    @JsonProperty("uuid")
    private String uuid;

    /** Type of Letter of Authorization */
    @JsonProperty("type")
    private LoaType type;

    /** A short, descriptive name for this LOA */
    @JsonProperty("name")
    private String name;

    /** Additional context about this LOA */
    @JsonProperty("description")
    private String description;

    /** Product type associated with this LOA. &lt;br&gt; XC - Cross Connect &lt;br&gt; XF_PORT - Fabric Port &lt;br&gt; VD - Network Edge */
    @JsonProperty("authorizedProductType")
    private LoaProductType authorizedProductType;

    /** Current lifecycle state of the LOA. */
    @JsonProperty("state")
    private LoaState state;

    /** The {@code operation} property. */
    @JsonProperty("operation")
    private LoaResponseOperation operation;

    /** Requestor */
    @JsonProperty("requestor")
    private LoaRequestor requestor;

    /** Issuer */
    @JsonProperty("issuer")
    private LoaIssuer issuer;

    /** Specifies the demarcation point. &lt;br&gt; For CAGE_LOA - cageUniqueSpaceId is required. &lt;br&gt; For PATCH_PANEL_LOA - patchPanelId is required. &lt;br&gt; For PATCH_PANEL_PORT_LOA - patchPanelId, patchPanelPortA, patchPanelPortB and connectorType is required. */
    @JsonProperty("demarcationPoint")
    private LoaDemarcationPoint demarcationPoint;

    /** Eligible location where LOA can be issued. */
    @JsonProperty("location")
    private LoaLocation location;

    /** Date and time when this LOA expires.&lt;br&gt; Default to 3 months from the creation date */
    @JsonProperty("expirationDateTime")
    private String expirationDateTime;

    /** Change log */
    @JsonProperty("changeLog")
    private LoaChangelog changeLog;
}
