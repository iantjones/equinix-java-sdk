/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.fabric.enums.EnvironmentActionState;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionType;
import com.eqixiac.equinix.fabric.model.EnvironmentActionResponse;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

/**
 * Wire model of the Fabric v4 {@code EnvironmentActionResponse} schema. Read-only: the JSON model
 * implements the public interface directly.
 *
 * <p><b>Beta</b>: the schema carries the catalog's Beta marker (catalog fetched 2026-09-21).</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnvironmentActionResponseJson implements EnvironmentActionResponse {

    @Getter static TypeReference<EnvironmentActionResponseJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private EnvironmentActionType type;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("state")
    private EnvironmentActionState state;

    @JsonProperty("keyDetails")
    private ActivationKeyDetails keyDetails;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
