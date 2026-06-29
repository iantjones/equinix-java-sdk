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

package api.equinix.javasdk.internetaccess.model.json;

import api.equinix.javasdk.internetaccess.enums.PatchPanelType;
import api.equinix.javasdk.internetaccess.model.PatchPanel;
import api.equinix.javasdk.internetaccess.model.implementation.CabinetRef;
import api.equinix.javasdk.internetaccess.model.implementation.CageRef;
import api.equinix.javasdk.internetaccess.model.implementation.PatchPanelOwnedPort;
import api.equinix.javasdk.internetaccess.model.implementation.SecureCageAccount;
import api.equinix.javasdk.internetaccess.model.implementation.SpaceLocation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Read-only JSON model for a {@link PatchPanel} returned by the Equinix Internet Access (EIA) v1
 * product-availability lookup {@code GET /internetAccess/v1/patchPanels}. Implements
 * {@link PatchPanel} directly, so no wrapper is required.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatchPanelJson implements PatchPanel {

    @JsonProperty("number")
    private String number;

    @JsonProperty("customerRefNumber")
    private String customerRefNumber;

    @JsonProperty("type")
    private PatchPanelType type;

    @JsonProperty("prewired")
    private Boolean prewired;

    @JsonProperty("availablePortsCount")
    private Integer availablePortsCount;

    @JsonProperty("availablePorts")
    private List<Integer> availablePorts;

    @JsonProperty("ownedPortsCount")
    private Integer ownedPortsCount;

    @JsonProperty("ownedPorts")
    private List<PatchPanelOwnedPort> ownedPorts;

    @JsonProperty("mediaTypes")
    private List<String> mediaTypes;

    @JsonProperty("dedicatedMediaType")
    private String dedicatedMediaType;

    @JsonProperty("cage")
    private CageRef cage;

    @JsonProperty("cabinet")
    private CabinetRef cabinet;

    @JsonProperty("location")
    private SpaceLocation location;

    @JsonProperty("account")
    private SecureCageAccount account;
}
