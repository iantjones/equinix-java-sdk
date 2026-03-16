package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

import java.util.List;
import java.util.Optional;

/**
 * A metro-by-site latency grid. Provides indexed access and formatted
 * output for architecture review documents.
 */
@Value
public class LatencyMatrix {

    List<MetroCode> metros;
    List<String> siteLabels;
    List<List<LatencyEntry>> matrix;

    /**
     * Retrieves the latency entry for a specific metro–site pair.
     */
    public Optional<LatencyEntry> get(MetroCode metro, String siteLabel) {
        int mi = metros.indexOf(metro);
        int si = siteLabels.indexOf(siteLabel);
        if (mi < 0 || si < 0) return Optional.empty();
        return Optional.of(matrix.get(mi).get(si));
    }

    /**
     * Worst-case latency from a metro to any site.
     */
    public double worstCase(MetroCode metro) {
        int mi = metros.indexOf(metro);
        if (mi < 0) return Double.MAX_VALUE;
        return matrix.get(mi).stream()
                .mapToDouble(LatencyEntry::getLatencyMs)
                .max()
                .orElse(Double.MAX_VALUE);
    }

    /**
     * Average latency from a metro to all sites.
     */
    public double average(MetroCode metro) {
        int mi = metros.indexOf(metro);
        if (mi < 0) return Double.MAX_VALUE;
        return matrix.get(mi).stream()
                .mapToDouble(LatencyEntry::getLatencyMs)
                .average()
                .orElse(Double.MAX_VALUE);
    }

    /**
     * Renders the latency matrix as an ASCII table suitable for console output
     * or inclusion in markdown reports.
     */
    public String toTableString() {
        if (metros.isEmpty() || siteLabels.isEmpty()) return "(empty matrix)";

        int metroColWidth = metros.stream()
                .mapToInt(m -> m.name().length())
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
            sb.append(String.format("%-" + metroColWidth + "s", metros.get(mi).name()));
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
