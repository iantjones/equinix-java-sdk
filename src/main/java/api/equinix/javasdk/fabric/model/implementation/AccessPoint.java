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

import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.PeeringType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
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
    private BasicPort port;

    @JsonProperty("profile")
    private Profile profile;

    @JsonProperty("linkProtocol")
    private LinkProtocol linkProtocol;

    @JsonProperty("virtualDevice")
    private BasicVirtualDevice virtualDevice;

    @JsonProperty("sellerRegion")
    private String sellerRegion;

    @JsonProperty("authenticationKey")
    private String authenticationKey;

    @JsonProperty("providerConnectionId")
    private String providerConnectionId;

    @JsonProperty("peeringType")
    private PeeringType peeringType;
}
