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

package api.equinix.javasdk.design.peering.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Categorizes how a network can be reached at an Equinix metro.
 *
 * <p>A target ASN may be reachable through IX peering, private Fabric connections,
 * or both. Understanding the available connectivity types is essential for resiliency
 * analysis — relying solely on one type creates a single point of failure.</p>
 *
 * @author ianjones
 * @see api.equinix.javasdk.design.peering.model.PresenceCell
 * @see api.equinix.javasdk.design.peering.model.UnifiedConnectivityView
 */
@Getter
@AllArgsConstructor
public enum ConnectivityType {

    IX_PEERING("IX Peering", "Reachable via Equinix Internet Exchange peering"),

    FABRIC_CONNECTION("Fabric Connection", "Reachable via Equinix Fabric private connectivity"),

    BOTH("IX + Fabric", "Reachable via both IX peering and Fabric private connectivity"),

    FACILITY_ONLY("Facility Only", "Present in Equinix facility but not at IX or on Fabric"),

    NONE("None", "Not reachable at this metro through Equinix");

    private final String displayName;
    private final String description;

    /**
     * Combines IX peering and Fabric availability into a unified connectivity type.
     *
     * @param hasIxPeering   whether the ASN peers at an Equinix IX in this metro
     * @param hasFabric       whether the ASN is available via Fabric service profile
     * @param hasFacility     whether the ASN is colocated in an Equinix facility
     * @return the combined connectivity type
     */
    public static ConnectivityType resolve(boolean hasIxPeering, boolean hasFabric, boolean hasFacility) {
        if (hasIxPeering && hasFabric) return BOTH;
        if (hasIxPeering) return IX_PEERING;
        if (hasFabric) return FABRIC_CONNECTION;
        if (hasFacility) return FACILITY_ONLY;
        return NONE;
    }
}
