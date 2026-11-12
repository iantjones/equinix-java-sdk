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
import com.eqixiac.equinix.fabric.beta.enums.OpticalConnectDemarcationPointConnectorType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Specifies the demarcation point: the patch panel and ports at this side.
 *
 * <p>Maps spec schema {@code OpticalConnectDemarcationPoint}. Unknown properties are ignored on read.</p>
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
public class OpticalConnectDemarcationPoint {

    /** Unique identifier of the patch panel. */
    @JsonProperty("patchPanelId")
    private String patchPanelId;

    /** Specify the desired port number for Port A. &lt;br&gt; When ports are not provided, next available ports will be used. */
    @JsonProperty("patchPanelPortA")
    private String patchPanelPortA;

    /** Specify the desired port number for Port B. &lt;br&gt; When ports are not provided, next available ports will be used. &lt;br&gt; Required for Connector type FC, SC and ST only. */
    @JsonProperty("patchPanelPortB")
    private String patchPanelPortB;

    /** Type of Connector */
    @JsonProperty("connectorType")
    private OpticalConnectDemarcationPointConnectorType connectorType;
}
