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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified view of all connectivity options to a target ASN across Equinix metros,
 * combining both IX peering (from PeeringDB) and Fabric private connections
 * (from Equinix Fabric service profiles).
 *
 * <p>This is the "one view to rule them all" — for a given ASN, it shows every
 * way the customer can reach that network through Equinix, at every metro,
 * through both public peering and private connectivity. No other SDK provides
 * this unified perspective.</p>
 *
 * @author ianjones
 * @see PresenceMatrix
 * @see PeeringIntelligenceResult
 */
@Value
@Builder
public class UnifiedConnectivityView {

    /** The target ASN. */
    long asn;

    /** Human-readable label for the target. */
    String label;

    /** Per-metro connectivity entries. */
    List<MetroConnectivity> metroConnectivity;

    /** Total number of metros where this ASN is reachable via any Equinix path. */
    int reachableMetroCount;

    /** Total IX capacity across all metros in Mbps. */
    long totalIxCapacityMbps;

    /** Whether Fabric private connections are available at any metro. */
    boolean fabricAvailableAnywhere;

    /**
     * Returns connectivity for a specific metro.
     *
     * @param metro the metro to look up
     * @return the metro connectivity entry, or {@code null} if not present
     */
    public MetroConnectivity forMetro(MetroCode metro) {
        return metroConnectivity.stream()
                .filter(mc -> mc.getMetro() == metro)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns only metros where IX peering is available.
     *
     * @return filtered metro connectivity list
     */
    public List<MetroConnectivity> ixPeeringMetros() {
        return metroConnectivity.stream()
                .filter(MetroConnectivity::isHasIxPeering)
                .collect(Collectors.toList());
    }

    /**
     * Returns only metros where Fabric is available.
     *
     * @return filtered metro connectivity list
     */
    public List<MetroConnectivity> fabricMetros() {
        return metroConnectivity.stream()
                .filter(MetroConnectivity::isHasFabric)
                .collect(Collectors.toList());
    }

    /**
     * Renders as a Markdown table showing per-metro connectivity.
     *
     * @return Markdown-formatted table
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("### Unified Connectivity: ").append(label).append(" (AS").append(asn).append(")\n\n");
        sb.append("| Metro | IX Peering | IX Capacity | Route Server | Fabric | Combined |\n");
        sb.append("|-------|-----------|-------------|--------------|--------|----------|\n");

        for (MetroConnectivity mc : metroConnectivity) {
            sb.append("| ").append(mc.getMetro().name());
            sb.append(" | ").append(mc.isHasIxPeering() ? "Yes" : "No");
            sb.append(" | ").append(mc.isHasIxPeering() ? mc.getIxCapacityMbps() / 1000 + "G" : "-");
            sb.append(" | ").append(mc.isRouteServerAvailable() ? "Yes" : "No");
            sb.append(" | ").append(mc.isHasFabric() ? "Yes" : "No");
            sb.append(" | ").append(mc.getConnectivityType().getDisplayName());
            sb.append(" |\n");
        }
        return sb.toString();
    }

    /**
     * A single metro's connectivity details for this ASN.
     */
    @Value
    @Builder
    public static class MetroConnectivity {
        MetroCode metro;
        ConnectivityType connectivityType;
        boolean hasIxPeering;
        boolean hasFabric;
        int ixCapacityMbps;
        boolean routeServerAvailable;
        boolean bfdAvailable;
        List<IxPresenceDetail> ixSessions;
        String fabricServiceProfileUuid;
    }
}
