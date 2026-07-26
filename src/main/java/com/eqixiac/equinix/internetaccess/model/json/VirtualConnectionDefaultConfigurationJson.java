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

package com.eqixiac.equinix.internetaccess.model.json;

import com.eqixiac.equinix.internetaccess.enums.BillingType;
import com.eqixiac.equinix.internetaccess.enums.Redundancy;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.VirtualConnectionDefaultConfiguration;
import com.eqixiac.equinix.internetaccess.model.implementation.VirtualConnectionConnection;
import com.eqixiac.equinix.internetaccess.model.implementation.VirtualConnectionRoutingProtocol;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Read-only JSON model for a {@link VirtualConnectionDefaultConfiguration} returned by the Equinix
 * Internet Access (EIA) v1 lookup
 * {@code GET /internetAccess/v1/virtualConnectionDefaultConfigurations}. Implements
 * {@link VirtualConnectionDefaultConfiguration} directly, so no wrapper is required.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualConnectionDefaultConfigurationJson implements VirtualConnectionDefaultConfiguration {

    @JsonProperty("type")
    private Redundancy type;

    @JsonProperty("useCase")
    private UseCase useCase;

    @JsonProperty("billing")
    private BillingType billing;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("minBandwidthCommit")
    private Integer minBandwidthCommit;

    @JsonProperty("connection")
    private VirtualConnectionConnection connection;

    @JsonProperty("routingProtocol")
    private VirtualConnectionRoutingProtocol routingProtocol;
}
