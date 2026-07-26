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

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.eqixiac.equinix.fabric.enums.PortType;
import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.fabric.model.PortStatistic;
import com.eqixiac.equinix.fabric.model.implementation.BandwidthUtilization;
import com.eqixiac.equinix.fabric.model.implementation.PortStat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * <p>Port traffic statistics. {@code GET /fabric/v4/ports/{portId}/stats} returns the spec's
 * {@code Statistics} schema, whose properties ({@code startDateTime}, {@code endDateTime},
 * {@code viewPoint}, {@code bandwidthUtilization}) are top-level on this model.</p>
 *
 * <p>The legacy top-ports listing fields ({@code uuid}, {@code type}, {@code name},
 * {@code bandwidth}, {@code stats}) are retained for backward compatibility with the
 * historical top-utilized-ports response shape.</p>
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortStatisticJson {

    @JsonProperty("startDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime startDateTime;

    @JsonProperty("endDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime endDateTime;

    @JsonProperty("viewPoint")
    private Side viewPoint;

    @JsonProperty("bandwidthUtilization")
    private BandwidthUtilization bandwidthUtilization;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("stats")
    private PortStat stats;
}
