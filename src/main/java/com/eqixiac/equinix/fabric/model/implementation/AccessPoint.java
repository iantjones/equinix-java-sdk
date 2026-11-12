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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.fabric.enums.AccessPointRole;
import com.eqixiac.equinix.fabric.enums.AccessPointType;
import com.eqixiac.equinix.fabric.enums.PeeringType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessPoint {

    @JsonProperty("type")
    private AccessPointType type;

    @Getter(AccessLevel.NONE)
    @JsonProperty("interface")
    private Interface apInterface;

    //TODO This is a very messy/dirty solution - needs thought and to be re-worked.
    public SimpleAccessPoint.SideWrapper simpleClone(Side accessPointSide) {
        return SimpleAccessPoint.simpleClone(this, accessPointSide);
    }

    public Interface getInterface() {
        return apInterface;
    }

    @JsonProperty("account")
    private Account account;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("port")
    private PortSummary port;

    @JsonProperty("profile")
    private Profile profile;

    @JsonProperty("linkProtocol")
    private LinkProtocol linkProtocol;

    @JsonProperty("virtualDevice")
    private VirtualDeviceSummary virtualDevice;

    @JsonProperty("sellerRegion")
    private String sellerRegion;

    @JsonProperty("authenticationKey")
    private String authenticationKey;

    @JsonProperty("providerConnectionId")
    private String providerConnectionId;

    @JsonProperty("peeringType")
    private PeeringType peeringType;

    @JsonProperty("router")
    private CloudRouterSummary router;

    @JsonProperty("network")
    private NetworkSummary network;

    @JsonProperty("environment")
    private ProviderEnvironment environment;

    @JsonProperty("activationKey")
    private String activationKey;

    @JsonProperty("virtualNetwork")
    private VirtualNetwork virtualNetwork;

    @JsonProperty("interconnection")
    private MetalInterconnection interconnection;

    @JsonProperty("role")
    private AccessPointRole role;

    /** {@code providerIdentifier}: the provider's identifier for this access point (spec 4.34.0). */
    @JsonProperty("providerIdentifier")
    private String providerIdentifier;

    /** {@code productOffering} (z-side only, spec 4.34.0). */
    @JsonProperty("productOffering")
    private ProductOffering productOffering;

    /** {@code bandwidthCommit} in Mbps (0-100000, spec 4.34.0). */
    @JsonProperty("bandwidthCommit")
    private Integer bandwidthCommit;

    /** {@code address} (spec 4.34.0). */
    @JsonProperty("address")
    private AccessPointAddress address;

    /** {@code interconnect} of an {@code IC} access point (live shape, 2026-11-11). */
    @JsonProperty("interconnect")
    private InterconnectRef interconnect;
}
