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

    long asn;

    String label;

    List<MetroConnectivity> metroConnectivity;

    int reachableMetroCount;

    long totalIxCapacityMbps;

    boolean fabricAvailableAnywhere;

    /**
     * Returns connectivity for a specific metro.
     *
     * @param metro the metro to look up
     * @return the metro connectivity entry, or {@code null} if not present
     */
    public MetroConnectivity forMetro(MetroId metro) {
        return metroConnectivity.stream()
                .filter(mc -> java.util.Objects.equals(mc.getMetro(), metro))
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
            sb.append("| ").append(mc.getMetro().code());
            sb.append(" | ").append(mc.isHasIxPeering() ? "Yes" : "No");
            sb.append(" | ").append(mc.isHasIxPeering() ? formatGbps(mc.getIxCapacityMbps()) : "-");
            sb.append(" | ").append(mc.isRouteServerAvailable() ? "Yes" : "No");
            sb.append(" | ").append(mc.isHasFabric() ? "Yes" : "No");
            sb.append(" | ").append(mc.getConnectivityType().getDisplayName());
            sb.append(" |\n");
        }
        return sb.toString();
    }

    /**
     * Formats an Mbps capacity as Gbps without truncating sub-Gbps figures: whole Gbps without a decimal
     * ({@code "100G"}), fractional Gbps with one decimal ({@code "10.5G"}), and sub-Gbps capacity in Mbps
     * ({@code "500M"}) rather than collapsing to a misleading {@code "0G"}.
     *
     * @param mbps the capacity in megabits per second
     * @return a formatted capacity string
     */
    private static String formatGbps(long mbps) {
        if (mbps > 0 && mbps < 1000) {
            return mbps + "M";
        }
        double gbps = mbps / 1000.0;
        return (gbps == Math.rint(gbps))
                ? String.format("%.0fG", gbps)
                : String.format("%.1fG", gbps);
    }

    /**
     * A single metro's connectivity details for this ASN.
     */
    @Value
    @Builder
    public static class MetroConnectivity {
        MetroId metro;
        ConnectivityType connectivityType;
        boolean hasIxPeering;
        boolean hasFabric;
        long ixCapacityMbps;
        boolean routeServerAvailable;
        boolean bfdAvailable;
        List<IxPresenceDetail> ixSessions;
        String fabricServiceProfileUuid;
    }
}
