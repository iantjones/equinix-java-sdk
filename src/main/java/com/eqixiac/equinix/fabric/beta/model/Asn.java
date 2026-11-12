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
import com.eqixiac.equinix.fabric.beta.enums.AsnRange;
import com.eqixiac.equinix.fabric.beta.enums.AsnRirRegion;
import com.eqixiac.equinix.fabric.beta.enums.AsnState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ASN Record
 *
 * <p>Maps spec schema {@code Asn}. Unknown properties are ignored on read.</p>
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
public class Asn {

    /** The {@code href} property. */
    @JsonProperty("href")
    private String href;

    /** Unique identifier for the ASN record */
    @JsonProperty("uuid")
    private String uuid;

    /** ASN lifecycle state */
    @JsonProperty("state")
    private AsnState state;

    /** The {@code project} property. */
    @JsonProperty("project")
    private Project project;

    /** Autonomous System Number */
    @JsonProperty("number")
    private Long number;

    /** Regional Internet Registry region */
    @JsonProperty("rirRegion")
    private AsnRirRegion rirRegion;

    /** The {@code location} property. */
    @JsonProperty("location")
    private SimplifiedLocation location;

    /** ASN range selection */
    @JsonProperty("range")
    private AsnRange range;

    /** Ownership challenge details for public ASN verification */
    @JsonProperty("ownershipChallengeDetails")
    private AsnOwnershipChallengeDetails ownershipChallengeDetails;

    /** Change reference for ASN */
    @JsonProperty("change")
    private AsnChange change;

    /** The {@code assets} property. */
    @JsonProperty("assets")
    private List<AsnAsset> assets;

    /** List of errors */
    @JsonProperty("errors")
    private List<ApiError> errors;

    /** Change log */
    @JsonProperty("changeLog")
    private Changelog changeLog;

    /** ASN type, PUBLIC_ASN or PRIVATE_ASN. Present in the spec examples but not declared in the schema. */
    @JsonProperty("type")
    private String type;
}
