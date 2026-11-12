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
import com.eqixiac.equinix.fabric.beta.enums.TagResponseState;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Equinix Fabric Tag Response Object
 *
 * <p>Maps spec schema {@code TagResponse}. Unknown properties are ignored on read.</p>
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
public class TagResponse {

    /** The {@code href} property. */
    @JsonProperty("href")
    private String href;

    /** The {@code uuid} property. */
    @JsonProperty("uuid")
    private String uuid;

    /** The {@code type} property. */
    @JsonProperty("type")
    private String type;

    /** The {@code name} property. */
    @JsonProperty("name")
    private String name;

    /** The {@code displayName} property. */
    @JsonProperty("displayName")
    private String displayName;

    /** Status of the Tag request. Can be APPROVED, PENDING_APPROVAL, REJECT */
    @JsonProperty("state")
    private TagResponseState state;

    /** The {@code notifications} property. */
    @JsonProperty("notifications")
    private List<SimplifiedNotification> notifications;

    /** Change log */
    @JsonProperty("changeLog")
    private Changelog changeLog;

    /** The {@code weight} property. */
    @JsonProperty("weight")
    private Integer weight;
}
