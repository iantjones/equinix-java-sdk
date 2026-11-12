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
                .filter(Double::isFinite) // NaN = unknown (no measurement, no coordinates)
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
                .filter(Double::isFinite) // NaN = unknown (no measurement, no coordinates)
                .average();
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
                String val = Double.isFinite(entry.getLatencyMs())
                        ? String.format("%.1f%s", entry.getLatencyMs(), entry.isEstimated() ? "*" : "")
                        : "?";
                sb.append(" | ").append(String.format("%" + siteColWidths[si] + "s", val));
            }
            sb.append("\n");
        }

        sb.append("\nRound-trip ms. * = estimated from distance (no measured Fabric latency); ? = unknown (no coordinates)");
        return sb.toString();
    }
}
