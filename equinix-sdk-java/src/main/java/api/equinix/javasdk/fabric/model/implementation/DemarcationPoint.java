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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DemarcationPoint {

    @JsonProperty("cabinetUniqueSpaceId")
    private String cabinetUniqueSpaceId;

    @JsonProperty("cageUniqueSpaceId")
    private String cageUniqueSpaceId;

    @JsonProperty("connectorType")
    private String connectorType;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("portReservationId")
    private String portReservationId;

    @JsonProperty("portGroup")
    private String portGroup;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("patchPanelName")
    private String patchPanelName;

    @JsonProperty("patchPanelPortA")
    private String patchPanelPortA;

    @JsonProperty("patchPanelPortB")
    private String patchPanelPortB;

    @JsonProperty("patchPanel")
    private String patchPanel;
}
