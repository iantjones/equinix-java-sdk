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

import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.DedicatedPortDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.implementation.DedicatedPortConnection;
import api.equinix.javasdk.internetaccess.model.implementation.DedicatedPortRoutingProtocol;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only JSON model for a {@link DedicatedPortDefaultConfiguration} returned by the Equinix
 * Internet Access (EIA) v1 lookup {@code GET /internetAccess/v1/dedicatedPortDefaultConfigurations}.
 * Implements {@link DedicatedPortDefaultConfiguration} directly, so no wrapper is required.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DedicatedPortDefaultConfigurationJson implements DedicatedPortDefaultConfiguration {

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
    private DedicatedPortConnection connection;

    @JsonProperty("routingProtocol")
    private DedicatedPortRoutingProtocol routingProtocol;
}
