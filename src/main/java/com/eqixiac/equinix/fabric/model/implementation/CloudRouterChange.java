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

package com.eqixiac.equinix.fabric.model.implementation;
import com.eqixiac.equinix.fabric.enums.ChangeType;

import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.eqixiac.equinix.fabric.enums.ChangeStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Current state of the latest Fabric Cloud Router change (the Fabric v4 {@code CloudRouterChange}
 * schema): change type (e.g. {@code ROUTER_UPDATE}), status and the individual change operations.
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudRouterChange {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ChangeType type;

    @JsonProperty("status")
    private ChangeStatus status;

    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("updatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedDateTime;

    @JsonProperty("information")
    private String information;

    @JsonProperty("data")
    private List<ChangeData> data;
}
