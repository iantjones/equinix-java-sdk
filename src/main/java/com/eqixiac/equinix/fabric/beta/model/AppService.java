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
import com.eqixiac.equinix.fabric.beta.enums.AppServiceState;
import com.eqixiac.equinix.fabric.beta.enums.AppServiceType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * App Service object
 *
 * <p>Maps spec schema {@code AppService}. Unknown properties are ignored on read.</p>
 * <p>Spec-required properties: {@code name}, {@code type}.</p>
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
public class AppService {

    /** Resource URI Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** App Service Type Required by the spec. */
    @JsonProperty("type")
    private AppServiceType type;

    /** Equinix-assigned access point identifier */
    @JsonProperty("uuid")
    private String uuid;

    /** Customer-provided App Service name Required by the spec. */
    @JsonProperty("name")
    private String name;

    /** Customer-provided App Service description */
    @JsonProperty("description")
    private String description;

    /** App Service lifecycle state */
    @JsonProperty("state")
    private AppServiceState state;

    /** Accessible endpoint through this service */
    @JsonProperty("endpoint")
    private String endpoint;

    /** List of source domains from where traffic is allowed */
    @JsonProperty("sourceDomains")
    private List<String> sourceDomains;

    /** The {@code account} property. */
    @JsonProperty("account")
    private SimplifiedAccount account;

    /** The {@code project} property. */
    @JsonProperty("project")
    private Project project;

    /** Change log */
    @JsonProperty("changeLog")
    private Changelog changeLog;

    /** Current state of latest AppService change */
    @JsonProperty("change")
    private AppServiceChange change;
}
