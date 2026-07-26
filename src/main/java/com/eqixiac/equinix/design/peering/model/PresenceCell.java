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

package com.eqixiac.equinix.design.peering.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.peering.enums.ConnectivityType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * A single cell in the {@link PresenceMatrix}, representing one ASN's presence
 * at one Equinix metro.
 *
 * <p>Contains the connectivity type (IX peering, Fabric, both, or none), port capacity
 * at the IX, number of IX sessions, and whether the ASN participates in route servers
 * at this metro. This rich cell data enables the matrix to convey far more than a
 * simple boolean presence/absence indicator.</p>
 *
 * @author ianjones
 * @see PresenceMatrix
 */
@Value
@Builder
public class PresenceCell {

    long asn;

    MetroId metro;

    ConnectivityType connectivityType;

    boolean ixPresent;

    boolean facilityPresent;

    boolean fabricAvailable;

    /**
     * The UUID of the Fabric service profile that evidences Fabric availability at this metro,
     * or {@code null} when {@code isFabricAvailable()} is {@code false}. When several matching
     * profiles publish this metro, the first in catalog order is carried.
     */
    String fabricServiceProfileUuid;

    int ixSessionCount;

    long totalIxCapacityMbps;

    boolean routeServerPeer;

    boolean bfdSupported;

    List<IxPresenceDetail> ixSessions;

    /**
     * Returns a compact, fixed-width (4-character) symbol for matrix display. The values are
     * space-padded for column alignment — compare with {@code trim()} or use
     * {@code getConnectivityType()} for logic; do not {@code equals()} against unpadded literals.
     *
     * @return {@code "IX+F"} for both, {@code " IX "} for IX only, {@code "FAB "} for Fabric only,
     *         {@code "FAC "} for facility only, {@code " -- "} for none
     */
    public String symbol() {
        switch (connectivityType) {
            case BOTH: return "IX+F";
            case IX_PEERING: return " IX ";
            case FABRIC_CONNECTION: return "FAB ";
            case FACILITY_ONLY: return "FAC ";
            default: return " -- ";
        }
    }

    /**
     * Returns a detailed, variable-width symbol including capacity (unpadded, unlike
     * {@link #symbol()}). A {@code *} after the capacity marks route-server participation.
     *
     * @return e.g. {@code "IX:100G*+FAB"}, {@code "IX:10G"}, {@code "FAB"}, {@code "FAC"},
     *         or {@code "---"} for none
     */
    public String detailedSymbol() {
        StringBuilder sb = new StringBuilder();
        if (ixPresent) {
            sb.append("IX:").append(compactCapacity(totalIxCapacityMbps));
            if (routeServerPeer) sb.append("*");
        }
        if (fabricAvailable) {
            if (sb.length() > 0) sb.append("+");
            sb.append("FAB");
        }
        if (sb.length() == 0) {
            if (facilityPresent) return "FAC";
            return "---";
        }
        return sb.toString();
    }

    /**
     * Compact capacity label for matrix cells: whole Gbps without a decimal ({@code "100G"}), fractional
     * Gbps with one decimal ({@code "10.5G"}), and sub-Gbps capacity shown in Mbps ({@code "500M"})
     * rather than truncating to a misleading {@code "0G"}.
     *
     * @param mbps the capacity in megabits per second
     * @return a compact capacity string
     */
    private static String compactCapacity(long mbps) {
        if (mbps < 1000) {
            return mbps + "M";
        }
        double gbps = mbps / 1000.0;
        return (gbps == Math.rint(gbps))
                ? String.format("%.0fG", gbps)
                : String.format("%.1fG", gbps);
    }
}
