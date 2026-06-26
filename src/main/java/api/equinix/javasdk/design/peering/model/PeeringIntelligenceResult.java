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
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Top-level result of a peering intelligence analysis.
 *
 * <p>Contains the complete analysis output: the presence matrix, per-ASN network presence
 * data, per-metro reports, resiliency assessment (if requested), unified connectivity
 * views, and mutual peering opportunities. Provides multiple output formats for
 * integration with dashboards, reports, and downstream tools.</p>
 *
 * <h3>Quick Access</h3>
 * <pre>{@code
 * PeeringIntelligenceResult result = fabric.peeringIntelligence()
 *     .addAsn(16509, "AWS")
 *     .addAsn(8075, "Microsoft")
 *     .customerMetros(MetroCode.DC, MetroCode.DA)
 *     .analyze();
 *
 * // Matrix view
 * System.out.println(result.presenceMatrix().toTableString());
 *
 * // Resiliency for a specific ASN
 * result.resiliency().failoverPathsForAsn(16509);
 *
 * // Unified connectivity view
 * result.unifiedView(16509).toMarkdown();
 *
 * // Full report
 * System.out.println(result.toMarkdown());
 * }</pre>
 *
 * @author ianjones
 * @see PresenceMatrix
 * @see ResiliencyAssessment
 * @see UnifiedConnectivityView
 */
@Value
@Builder
public class PeeringIntelligenceResult {

    /** The original analysis request. */
    PeeringRequest request;

    /** The ASN × Metro presence matrix. */
    PresenceMatrix presenceMatrix;

    /** Per-ASN network presence data (ASN → presence). */
    Map<Long, NetworkPresence> networkPresences;

    /** Per-metro presence reports. */
    Map<MetroCode, MetroPresenceReport> metroReports;

    /** Resiliency assessment (null if not requested). */
    ResiliencyAssessment resiliency;

    /** Unified connectivity views per ASN (null if Fabric integration not requested). */
    Map<Long, UnifiedConnectivityView> unifiedViews;

    /** Mutual peering opportunities (empty if customer ASN not provided). */
    List<PeeringOpportunity> peeringOpportunities;

    /** Timestamp when this analysis was computed. */
    Instant computedAt;

    /** Time taken to compute the analysis in milliseconds. */
    long computeTimeMs;

    /** Data sources used (e.g., "PeeringDB", "Equinix Fabric API"). */
    List<String> dataSources;

    /**
     * Returns the unified connectivity view for a specific ASN.
     *
     * @param asn the target ASN
     * @return the unified view, or {@code null} if not available
     */
    public UnifiedConnectivityView unifiedView(long asn) {
        return unifiedViews != null ? unifiedViews.get(asn) : null;
    }

    /**
     * Returns the network presence data for a specific ASN.
     *
     * @param asn the target ASN
     * @return the network presence, or {@code null} if not analyzed
     */
    public NetworkPresence networkPresence(long asn) {
        return networkPresences != null ? networkPresences.get(asn) : null;
    }

    /**
     * Returns the metro presence report for a specific metro.
     *
     * @param metro the metro code
     * @return the metro report, or {@code null} if not in scope
     */
    public MetroPresenceReport metroReport(MetroCode metro) {
        return metroReports != null ? metroReports.get(metro) : null;
    }

    /**
     * Returns a plain-text summary of the analysis.
     *
     * @return concise summary string
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Peering Intelligence Analysis\n");
        sb.append("=============================\n");
        sb.append("ASNs analyzed: ").append(request.getTargetAsns().size()).append("\n");
        sb.append("Metros with IX presence: ").append(presenceMatrix.getMetros().size()).append("\n");

        if (resiliency != null) {
            sb.append("Resiliency score: ").append(String.format("%.0f%%", resiliency.getOverallScore() * 100)).append("\n");
            sb.append("Correlated failures: ").append(resiliency.getCorrelatedFailures().size()).append("\n");
        }

        if (peeringOpportunities != null && !peeringOpportunities.isEmpty()) {
            sb.append("Peering opportunities: ").append(peeringOpportunities.size()).append("\n");
        }

        sb.append("Computed in ").append(computeTimeMs).append("ms\n");
        return sb.toString();
    }

    /**
     * Renders the complete analysis as a Markdown report.
     *
     * @return full Markdown report with matrix, resiliency, and recommendations
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Peering Intelligence Report\n\n");
        sb.append("**Analyzed:** ").append(request.getTargetAsns().size()).append(" ASNs across ");
        sb.append(presenceMatrix.getMetros().size()).append(" Equinix metros\n");
        sb.append("**Computed:** ").append(computedAt).append(" (").append(computeTimeMs).append("ms)\n");
        sb.append("**Sources:** ").append(String.join(", ", dataSources)).append("\n\n");

        // Presence Matrix
        sb.append("## Presence Matrix\n\n");
        sb.append(presenceMatrix.toMarkdown()).append("\n");

        // Legend
        sb.append("**Legend:** IX = IX Peering, FAB = Fabric Connection, ");
        sb.append("IX+F = Both IX and Fabric, FAC = Facility Only, -- = Not Present\n\n");

        // Network summaries
        sb.append("## Network Profiles\n\n");
        for (Map.Entry<Long, NetworkPresence> entry : networkPresences.entrySet()) {
            NetworkPresence np = entry.getValue();
            sb.append("### ").append(np.getLabel()).append(" (AS").append(np.getAsn()).append(")\n");
            sb.append("- **Type:** ").append(np.getNetworkType().getDisplayName()).append("\n");
            sb.append("- **Peering Policy:** ").append(np.getPeeringPolicy().getDisplayName()).append("\n");
            sb.append("- **IX Metros:** ").append(np.ixMetroCount()).append("\n");
            sb.append("- **Total IX Capacity:** ").append(np.getTotalIxCapacityMbps() / 1000).append(" Gbps\n");
            sb.append("- **Route Server:** ").append(np.isRouteServerParticipant() ? "Yes" : "No").append("\n");
            sb.append("- **IPv6:** ").append(np.isIpv6Capable() ? "Yes" : "No").append("\n\n");
        }

        // Resiliency
        if (resiliency != null) {
            sb.append("## Resiliency Assessment\n\n");
            sb.append("**Overall Score:** ").append(String.format("%.0f%%", resiliency.getOverallScore() * 100));
            sb.append(" (").append(resiliency.getOverallRating()).append(")\n\n");

            if (!resiliency.getCorrelatedFailures().isEmpty()) {
                sb.append("### Correlated Failures\n\n");
                for (CorrelatedFailure cf : resiliency.getCorrelatedFailures()) {
                    sb.append("- **").append(cf.getSeverity()).append(":** ").append(cf.getFailureDomain());
                    sb.append(" — ").append(cf.getRecommendation()).append("\n");
                }
                sb.append("\n");
            }

            if (!resiliency.getFindings().isEmpty()) {
                sb.append("### Findings\n\n");
                for (String finding : resiliency.getFindings()) {
                    sb.append("- ").append(finding).append("\n");
                }
                sb.append("\n");
            }
        }

        // Peering Opportunities
        if (peeringOpportunities != null && !peeringOpportunities.isEmpty()) {
            sb.append("## Peering Opportunities\n\n");
            sb.append("| Target | Metro | IX | Policy | Complexity | Feasibility |\n");
            sb.append("|--------|-------|----|---------|-----------|-----------|\n");
            for (PeeringOpportunity po : peeringOpportunities) {
                sb.append("| ").append(po.getTargetLabel());
                sb.append(" | ").append(po.getMetro().name());
                sb.append(" | ").append(po.getIxName());
                sb.append(" | ").append(po.getTargetPolicy().getDisplayName());
                sb.append(" | ").append(po.getComplexity());
                sb.append(" | ").append(String.format("%.0f%%", po.getFeasibility() * 100));
                sb.append(" |\n");
            }
            sb.append("\n");
        }

        // Unified Views
        if (unifiedViews != null && !unifiedViews.isEmpty()) {
            sb.append("## Unified Connectivity Views\n\n");
            for (UnifiedConnectivityView view : unifiedViews.values()) {
                sb.append(view.toMarkdown()).append("\n");
            }
        }

        return sb.toString();
    }
}
