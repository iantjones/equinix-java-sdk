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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Asset associated with ASN
 *
 * <p>Maps spec schema {@code AsnAsset}. Unknown properties are ignored on read.</p>
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
public class AsnAsset {

    /** The {@code uuid} property. */
    @JsonProperty("uuid")
    private String uuid;

    /** Associated Internet Access Service reference */
    @JsonProperty("service")
    private AsnAssetService service;

    /** The {@code announcingMetro} property. */
    @JsonProperty("announcingMetro")
    private String announcingMetro;

    /** The {@code ipBlocks} property. */
    @JsonProperty("ipBlocks")
    private List<AsnAssetIpBlockRef> ipBlocks;

    /** Change log */
    @JsonProperty("changeLog")
    private Changelog changeLog;

    /** Asset state. Returned by the live service (observed 2026-11-12 on POST /fabric/v4/asns/search) but not declared in the spec. */
    @JsonProperty("state")
    private String state;
}
