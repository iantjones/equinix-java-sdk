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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>ChangeLog class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@NoArgsConstructor
public class ChangeLog {

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdByFullName")
    private String createdByFullName;

    @JsonProperty("createdByEmail")
    private String createdByEmail;

    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("updatedBy")
    private String updatedBy;

    @JsonProperty("updatedByFullName")
    private String updatedByFullName;

    @JsonProperty("updatedByEmail")
    private String updatedByEmail;

    @JsonProperty("updatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedDateTime;

    @JsonProperty("deletedBy")
    private String deletedBy;

    @JsonProperty("deletedByFullName")
    private String deletedByFullName;

    @JsonProperty("deletedByEmail")
    private String deletedByEmail;

    @JsonProperty("deletedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDateTime;
}
