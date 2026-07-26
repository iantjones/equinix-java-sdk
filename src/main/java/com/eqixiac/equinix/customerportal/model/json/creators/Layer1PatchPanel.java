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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Patch panel ({@code patchPanel} in the cross-connects v2 spec): the device with a series of
 * connector ports that identifies the source/destination location to connect. {@code id} is
 * required; {@code portA}/{@code portB} are optional (when omitted the next available ports are
 * used). Use {@code portB} of {@code -1} when no port-B selection is required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1PatchPanel {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("portA")
    private Integer portA;

    @JsonProperty("portB")
    private Integer portB;

    public Layer1PatchPanel(String id) {
        this.id = id;
    }

    /**
     * Sets the desired port numbers for ports A and B.
     *
     * @param portA the port-A number
     * @param portB the port-B number ({@code -1} when no selection is required)
     * @return this patch panel
     */
    public Layer1PatchPanel ports(Integer portA, Integer portB) {
        this.portA = portA;
        this.portB = portB;
        return this;
    }
}
