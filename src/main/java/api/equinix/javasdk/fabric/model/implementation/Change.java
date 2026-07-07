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

import api.equinix.javasdk.fabric.enums.ChangeStatus;
import api.equinix.javasdk.fabric.enums.ChangeType;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Change {


    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private ChangeType type;

    @JsonProperty("status")
    private ChangeStatus status;

    /**
     * The change's patch operations. Network changes carry an array of operations;
     * connection changes carry a single operation, which is accepted here as a
     * one-element list.
     */
    @JsonProperty("data")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<ChangeData> data;

    @JsonProperty("information")
    private String information;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("updatedBy")
    private String updatedBy;

    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("updatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedDateTime;
}