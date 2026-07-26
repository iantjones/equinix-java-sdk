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
 * Patch equipment ({@code patchEquipment} in the cross-connects v2 spec): a chargeable additional
 * service enabling Equinix to install the cross connect from the demarcation panel to your
 * equipment. All four fields ({@code cabinetId}, {@code connectorType}, {@code details},
 * {@code port}) are required by the spec. The cabinet must belong to the cage defined in the
 * A-side details.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Layer1PatchEquipment {

    @JsonProperty("cabinetId")
    private final String cabinetId;

    @JsonProperty("connectorType")
    private final String connectorType;

    @JsonProperty("details")
    private final String details;

    @JsonProperty("port")
    private final Integer port;

    /**
     * Creates patch equipment.
     *
     * @param cabinetId     the device cabinet id (required)
     * @param connectorType the device connector type to facilitate cross connection (required)
     * @param details       additional information to facilitate the connection (required)
     * @param port          the device port number to be connected (required)
     */
    public Layer1PatchEquipment(String cabinetId, String connectorType, String details, Integer port) {
        this.cabinetId = cabinetId;
        this.connectorType = connectorType;
        this.details = details;
        this.port = port;
    }
}
