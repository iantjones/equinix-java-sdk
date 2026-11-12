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
import com.eqixiac.equinix.fabric.beta.enums.AppDomainState;
import com.eqixiac.equinix.fabric.beta.enums.AppDomainType;
import com.eqixiac.equinix.fabric.beta.enums.AppLinkAttachState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * App Domain object associated with App Link
 *
 * <p>Maps spec schema {@code AppLinkAttachedAppDomain}. Unknown properties are ignored on read.</p>
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
public class AppLinkAttachedAppDomain {

    /** Resource URI Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** App Domain Type Required by the spec. */
    @JsonProperty("type")
    private AppDomainType type;

    /** Equinix-assigned access point identifier Required by the spec. */
    @JsonProperty("uuid")
    private String uuid;

    /** Customer-provided App Domain name */
    @JsonProperty("name")
    private String name;

    /** Customer-provided App Domain description */
    @JsonProperty("description")
    private String description;

    /** App Domain lifecycle state */
    @JsonProperty("state")
    private AppDomainState state;

    /** The {@code project} property. */
    @JsonProperty("project")
    private Project project;

    /** App Link attachment lifecycle state */
    @JsonProperty("attachmentStatus")
    private AppLinkAttachState attachmentStatus;

    /** Change log. Present in the spec examples but not declared in the schema. */
    @JsonProperty("changeLog")
    private Changelog changeLog;
}
