package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A metro-by-site latency grid. Provides indexed access and formatted
 * output for architecture review documents.
 *
 * <p>A matrix built for a request with no user sites has a row per metro but no columns:
 * there is nothing to measure latency <em>to</em>. The {@link OptionalDouble}-returning
 * accessors ({@code worstCaseMs}, {@code averageMs}) report that as an absent value rather
 * than as a number, so callers cannot mistake "not measurable" for a measurement.</p>
 */
@Value
public class LatencyMatrix {

    List<MetroId> metros;
    List<String> siteLabels;
    List<List<LatencyEntry>> matrix;

    /**
     * Retrieves the latency entry for a specific metro-site pair.
     */
    public Optional<LatencyEntry> get(MetroId metro, String siteLabel) {
        int mi = metros.indexOf(metro);
        int si = siteLabels.indexOf(siteLabel);
        if (mi < 0 || si < 0) return Optional.empty();
        return Optional.of(matrix.get(mi).get(si));
    }

    /**
     * Returns {@code true} when this matrix has at least one site column, i.e. when its
     * latency figures are measurable at all. When this is {@code false}, {@code worstCaseMs}
     * and {@code averageMs} are empty for every metro and {@link #toTableString()} renders the
     * empty placeholder.
     */
    public boolean hasSites() {
        return !siteLabels.isEmpty();
    }

    /**
     * Worst-case latency in milliseconds from a metro to any site.
     *
     * @param metro the metro to measure from
     * @return the highest latency to any site, or {@link OptionalDouble#empty()} when the
     *         metro is not in this matrix or the request defined no sites
     */
    public OptionalDouble worstCaseMs(MetroId metro) {
        int mi = metros.indexOf(metro);
        if (mi < 0) return OptionalDouble.empty();
        return matrix.get(mi).stream()
                .mapToDouble(LatencyEntry::getLatencyMs)
                .max();
    }

    /**
     * Average latency in milliseconds from a metro to all sites.
     *
     * @param metro the metro to measure from
     * @return the mean latency across all sites, or {@link OptionalDouble#empty()} when the
     *         metro is not in this matrix or the request defined no sites
     */
    public OptionalDouble averageMs(MetroId metro) {
        int mi = metros.indexOf(metro);
        if (mi < 0) return OptionalDouble.empty();
        return matrix.get(mi).stream()
                .mapToDouble(LatencyEntry::getLatencyMs)
                .average();
    }

    // ──────────────────────────────────────────────────────────────────────────────────────
    //  Legacy sentinel accessors — NOT public API.
    //
    //  These are retained solely so MetroOptimizerLatencySentinelTest can pin the sentinel
    //  semantics it was written to outlaw; nothing in src/main calls them. They cannot be made
    //  package-private because that suite lives one package up, in
    //  com.eqixiac.equinix.design.optimizer. Treat them as removed: do not call them from new
    //  code, and delete them together with the assertions in that suite.
    // ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * Worst-case latency from a metro to any site, using {@link Double#MAX_VALUE} as an
     * out-of-band "no data" sentinel.
     *
     * @param metro the metro to measure from
     * @return the highest latency to any site, or {@code Double.MAX_VALUE} when the metro is
     *         absent or the request defined no sites
     * @deprecated <strong>Internal; scheduled for deletion, not part of the supported API.</strong>
     *         The {@code Double.MAX_VALUE} sentinel reads as a real measurement and has been
     *         formatted into user-facing text as such. Use {@link #worstCaseMs(MetroId)}, which
     *         makes the absent case explicit.
     */
    @Deprecated
    public double worstCase(MetroId metro) {
        return worstCaseMs(metro).orElse(Double.MAX_VALUE);
    }

    /**
     * Average latency from a metro to all sites, using {@link Double#MAX_VALUE} as an
     * out-of-band "no data" sentinel.
     *
     * @param metro the metro to measure from
     * @return the mean latency across all sites, or {@code Double.MAX_VALUE} when the metro
     *         is absent or the request defined no sites
     * @deprecated <strong>Internal; scheduled for deletion, not part of the supported API.</strong>
     *         The {@code Double.MAX_VALUE} sentinel reads as a real measurement. Use
     *         {@link #averageMs(MetroId)}, which makes the absent case explicit.
     */
    @Deprecated
    public double average(MetroId metro) {
        return averageMs(metro).orElse(Double.MAX_VALUE);
    }

    /**
     * Renders the latency matrix as an ASCII table suitable for console output
     * or inclusion in markdown reports.
     */
    public String toTableString() {
        if (metros.isEmpty() || !hasSites()) return "(empty matrix)";

        int metroColWidth = metros.stream()
                .mapToInt(m -> m.code().length())
                .max().orElse(5);
        metroColWidth = Math.max(metroColWidth, 5);

        int[] siteColWidths = new int[siteLabels.size()];
        for (int i = 0; i < siteLabels.size(); i++) {
            siteColWidths[i] = Math.max(siteLabels.get(i).length(), 8);
        }

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.format("%-" + metroColWidth + "s", "Metro"));
        for (int i = 0; i < siteLabels.size(); i++) {
            sb.append(" | ").append(String.format("%" + siteColWidths[i] + "s", siteLabels.get(i)));
        }
        sb.append("\n");

        // Separator
        sb.append("-".repeat(metroColWidth));
        for (int siteColWidth : siteColWidths) {
            sb.append("-+-").append("-".repeat(siteColWidth));
        }
        sb.append("\n");

        // Rows
        for (int mi = 0; mi < metros.size(); mi++) {
            sb.append(String.format("%-" + metroColWidth + "s", metros.get(mi).code()));
            for (int si = 0; si < siteLabels.size(); si++) {
                LatencyEntry entry = matrix.get(mi).get(si);
                String val = String.format("%.1f%s", entry.getLatencyMs(),
                        entry.isEstimated() ? "*" : "");
                sb.append(" | ").append(String.format("%" + siteColWidths[si] + "s", val));
            }
            sb.append("\n");
        }

        sb.append("\n* = estimated (no direct latency data available)");
        return sb.toString();
    }
}
