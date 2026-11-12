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
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectNoteResponseSource;
import com.eqixiac.equinix.fabric.beta.enums.CrossConnectNoteResponseType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cross Connect Note.
 *
 * <p>Maps spec schema {@code CrossConnectNoteResponse}. Unknown properties are ignored on read.</p>
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
public class CrossConnectNoteResponse {

    /** Note URI. Read-only (server-populated). */
    @JsonProperty("href")
    private String href;

    /** Note Identifier. Read-only (server-populated). */
    @JsonProperty("uuid")
    private String uuid;

    /** Sub-resource type. Read-only (server-populated). */
    @JsonProperty("type")
    private CrossConnectNoteResponseType type;

    /** Party that added the note. Read-only (server-populated). */
    @JsonProperty("source")
    private CrossConnectNoteResponseSource source;

    /** Note comments. Read-only (server-populated). */
    @JsonProperty("comments")
    private String comments;

    /** The note this note is responding to. Returned when the note was added in response to an Equinix note. Read-only (server-populated). */
    @JsonProperty("parentNote")
    private CrossConnectParentNote parentNote;

    /** Cross Connect Change Log. */
    @JsonProperty("changeLog")
    private CrossConnectChangelog changeLog;
}
