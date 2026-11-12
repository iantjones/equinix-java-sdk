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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Change log
 *
 * <p>Maps spec schema {@code Changelog}. Unknown properties are ignored on read.</p>
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
public class Changelog {

    /** Created by User Key */
    @JsonProperty("createdBy")
    private String createdBy;

    /** Created by User Full Name */
    @JsonProperty("createdByFullName")
    private String createdByFullName;

    /** Created by User Email Address */
    @JsonProperty("createdByEmail")
    private String createdByEmail;

    /** Created by Date and Time */
    @JsonProperty("createdDateTime")
    private String createdDateTime;

    /** Updated by User Key */
    @JsonProperty("updatedBy")
    private String updatedBy;

    /** Updated by User Full Name */
    @JsonProperty("updatedByFullName")
    private String updatedByFullName;

    /** Updated by User Email Address */
    @JsonProperty("updatedByEmail")
    private String updatedByEmail;

    /** Updated by Date and Time */
    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    /** Deleted by User Key */
    @JsonProperty("deletedBy")
    private String deletedBy;

    /** Deleted by User Full Name */
    @JsonProperty("deletedByFullName")
    private String deletedByFullName;

    /** Deleted by User Email Address */
    @JsonProperty("deletedByEmail")
    private String deletedByEmail;

    /** Deleted by Date and Time */
    @JsonProperty("deletedDateTime")
    private String deletedDateTime;
}
