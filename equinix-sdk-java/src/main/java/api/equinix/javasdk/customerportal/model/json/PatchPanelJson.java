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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.enums.InstallLocation;
import api.equinix.javasdk.customerportal.enums.PatchPanelType;
import api.equinix.javasdk.customerportal.enums.ProvisioningType;
import api.equinix.javasdk.customerportal.model.PatchPanel;
import api.equinix.javasdk.customerportal.model.implementation.UserPortDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a patch panel. Deserializes both the list shape ({@code patch_panel_response},
 * returned by {@code ListPatchPanels}) and the full detail shape ({@code patch_panel_details},
 * returned by {@code GetPatchPanel}); fields absent from either shape are left {@code null}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatchPanelJson implements PatchPanel {

    @JsonProperty("patchPanelId")
    private String patchPanelId;

    @JsonProperty("availablePortCount")
    private Integer availablePortCount;

    @JsonProperty("patchPanelReferenceId")
    private String patchPanelReferenceId;

    @JsonProperty("ifcEnabled")
    private Boolean ifcEnabled;

    @JsonProperty("provisioningType")
    private ProvisioningType provisioningType;

    // ---- patch_panel_details (GetPatchPanel) ----

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cageId")
    private String cageId;

    @JsonProperty("cabinetId")
    private String cabinetId;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("dedicatedMediaType")
    private String dedicatedMediaType;

    @JsonProperty("preWired")
    private Boolean preWired;

    @JsonProperty("type")
    private PatchPanelType type;

    @JsonProperty("rackLocations")
    private String rackLocations;

    @JsonProperty("installLocations")
    private InstallLocation installLocations;

    @JsonProperty("installationRequired")
    private Boolean installationRequired;

    @JsonProperty("circuitAvailable")
    private Boolean circuitAvailable;

    @JsonProperty("availablePorts")
    private List<Integer> availablePorts;

    @JsonProperty("connectionServices")
    private List<ConnectionServiceJson> connectionServices;

    @JsonProperty("usedPortsDetails")
    private List<UserPortDetails> usedPortsDetails;
}
