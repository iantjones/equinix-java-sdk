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

import api.equinix.javasdk.internetaccess.model.Cabinet;
import api.equinix.javasdk.internetaccess.model.implementation.CageRef;
import api.equinix.javasdk.internetaccess.model.implementation.IbxLocation;
import api.equinix.javasdk.internetaccess.model.implementation.SecureCageAccount;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Read-only JSON model for a {@link Cabinet} returned by the Equinix Internet Access (EIA) v1
 * product-availability lookup {@code GET /internetAccess/v1/cabinets}. Implements {@link Cabinet}
 * directly, so no wrapper is required.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CabinetJson implements Cabinet {

    @JsonProperty("spaceId")
    private String spaceId;

    @JsonProperty("number")
    private String number;

    @JsonProperty("patchPanelsCount")
    private Integer patchPanelsCount;

    @JsonProperty("cage")
    private CageRef cage;

    @JsonProperty("location")
    private IbxLocation location;

    @JsonProperty("account")
    private SecureCageAccount account;
}
