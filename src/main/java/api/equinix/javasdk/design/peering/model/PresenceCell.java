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

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
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

    int ixSessionCount;

    int totalIxCapacityMbps;

    boolean routeServerPeer;

    boolean bfdSupported;

    List<IxPresenceDetail> ixSessions;

    /**
     * Returns a compact symbol for matrix display.
     *
     * @return "IX+F" for both, "IX" for IX only, "FAB" for Fabric only, "FAC" for facility only, "---" for none
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
     * Returns a detailed symbol including capacity.
     *
     * @return e.g., "IX:100G+F" or "IX:10G" or "FAB" or "---"
     */
    public String detailedSymbol() {
        StringBuilder sb = new StringBuilder();
        if (ixPresent) {
            sb.append("IX:");
            if (totalIxCapacityMbps >= 100000) sb.append(totalIxCapacityMbps / 1000).append("G");
            else if (totalIxCapacityMbps >= 1000) sb.append(totalIxCapacityMbps / 1000).append("G");
            else sb.append(totalIxCapacityMbps).append("M");
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
}
