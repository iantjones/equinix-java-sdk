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
 *     .includeResiliency(true)   // default off; requires at least one customer metro
 *     .analyze();
 *
 * // Matrix view
 * System.out.println(result.getPresenceMatrix().toTableString());
 *
 * // Resiliency for a specific ASN (getResiliency() is null unless
 * // includeResiliency(true) was set with at least one customer metro)
 * result.getResiliency().failoverPathsForAsn(16509);
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

    PeeringRequest request;

    PresenceMatrix presenceMatrix;

    Map<Long, NetworkPresence> networkPresences;

    Map<MetroId, MetroPresenceReport> metroReports;

    ResiliencyAssessment resiliency;

    Map<Long, UnifiedConnectivityView> unifiedViews;

    List<PeeringOpportunity> peeringOpportunities;

    Instant computedAt;

    long computeTimeMs;

    List<String> dataSources;

    /**
     * Non-fatal data-completeness notes accumulated during the analysis: a data source that could not
     * be loaded, records that could not be resolved and were excluded, or an optional phase that was
     * skipped after a recoverable failure. Empty (or {@code null}) when the analysis ran cleanly. These
     * exist so an incomplete result is never silently presented as complete — always surface them rather
     * than reading the figures as authoritative when this list is non-empty.
     */
    List<String> warnings;

    /**
     * Returns the data-completeness warnings, never {@code null}.
     *
     * @return the warnings list, or an empty list when the analysis ran cleanly
     */
    public List<String> warnings() {
        return warnings != null ? warnings : java.util.Collections.emptyList();
    }

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
    public MetroPresenceReport metroReport(MetroId metro) {
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

        if (!warnings().isEmpty()) {
            sb.append("Data-completeness warnings: ").append(warnings().size())
                    .append(" (see toMarkdown() or warnings())\n");
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

        // Data-completeness warnings — surfaced up front so an incomplete result is never read as
        // complete. Empty when the analysis ran cleanly.
        if (!warnings().isEmpty()) {
            sb.append("> ⚠ **Data completeness:** this analysis is partial — read the figures below with "
                    + "these caveats in mind:\n");
            for (String warning : warnings()) {
                sb.append("> - ").append(warning).append("\n");
            }
            sb.append("\n");
        }

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
            sb.append("- **Total IX Capacity:** ").append(formatGbps(np.getTotalIxCapacityMbps())).append("\n");
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
                sb.append(" | ").append(po.getMetro().code());
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

    /**
     * Formats an Mbps capacity as a human-readable Gbps string without truncating sub-Gbps figures:
     * whole Gbps render without a decimal ({@code "100 Gbps"}), fractional Gbps keep one decimal
     * ({@code "0.5 Gbps"}), and sub-Gbps capacity is shown in Mbps ({@code "500 Mbps"}) rather than
     * collapsing to a misleading {@code "0 Gbps"}.
     *
     * @param mbps the capacity in megabits per second
     * @return a formatted capacity string
     */
    static String formatGbps(long mbps) {
        if (mbps > 0 && mbps < 1000) {
            return mbps + " Mbps";
        }
        double gbps = mbps / 1000.0;
        return (gbps == Math.rint(gbps))
                ? String.format("%.0f Gbps", gbps)
                : String.format("%.1f Gbps", gbps);
    }
}
