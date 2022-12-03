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

import api.equinix.javasdk.core.enums.ConnectionStatus;
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>Operation class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortOperation {

    @JsonProperty("operationalStatus")
    private OperationalStatus operationalStatus;

    @JsonProperty("opStatusChangedAt")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime opStatusChangedAt;

    @JsonProperty("connectionCount")
    private Integer connectionCount;

    @JsonProperty("evplVCCount")
    private Integer evplVCCount;

    @JsonProperty("fgVCCount")
    private Integer fgVCCount;

    @JsonProperty("accessVCCount")
    private Integer accessVCCount;
}