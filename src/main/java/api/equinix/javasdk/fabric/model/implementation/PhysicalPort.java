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

import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.fabric.enums.PortState;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>PhysicalPort class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class PhysicalPort {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("state")
    private PortState state;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("tether")
    private Tether tether;

    @JsonProperty("demarcationPoint")
    private DemarcationPoint demarcationPoint;

    @JsonProperty("settings")
    private PhysicalPortSettings settings;

    @JsonProperty("operation")
    private PortOperation portOperation;
}
