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
import com.eqixiac.equinix.fabric.beta.enums.AppLinkState;
import com.eqixiac.equinix.fabric.beta.enums.AppLinkType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * App Link object
 *
 * <p>Maps spec schema {@code AppServiceAttachedAppLink}. Unknown properties are ignored on read.</p>
 * <p>Spec-required properties: {@code type}, {@code uuid}.</p>
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
public class AppServiceAttachedAppLink {

    /** Resource URI Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** App Link Type Required by the spec. */
    @JsonProperty("type")
    private AppLinkType type;

    /** Equinix-assigned access point identifier Required by the spec. */
    @JsonProperty("uuid")
    private String uuid;

    /** Customer-provided App Link name */
    @JsonProperty("name")
    private String name;

    /** Customer-provided App Link description */
    @JsonProperty("description")
    private String description;

    /** App Link lifecycle state */
    @JsonProperty("state")
    private AppLinkState state;

    /** App Link Cloud Router */
    @JsonProperty("router")
    private AppLinkCloudRouter router;

    /** Customer-provided IP subnet for AppLink setup (/29 or larger subnet) */
    @JsonProperty("ipv4Subnet")
    private String ipv4Subnet;

    /** App Link IP address */
    @JsonProperty("ipv4Address")
    private String ipv4Address;

    /** App Link aggregated data transfer capacity in Mbps */
    @JsonProperty("bandwidth")
    private Integer bandwidth;

    /** The {@code project} property. */
    @JsonProperty("project")
    private Project project;
}
