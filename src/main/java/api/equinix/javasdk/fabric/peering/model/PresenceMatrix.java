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

package api.equinix.javasdk.fabric.peering.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Builder;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A two-dimensional matrix of ASN presence across Equinix metros.
 *
 * <p>Rows represent target ASNs; columns represent Equinix metros. Each cell is a
 * {@link PresenceCell} containing rich connectivity data (IX peering, Fabric availability,
 * port capacity, route server participation). The matrix supports both ASN-centric
 * views ("where is AWS present?") and metro-centric views ("who is present in DC?").</p>
 *
 * <h3>Output Formats</h3>
 * <ul>
 *   <li>{@link #toTableString()} — ASCII table for console output</li>
 *   <li>{@link #toDetailedTableString()} — ASCII table with capacity annotations</li>
 *   <li>{@link #toMarkdown()} — Markdown table for reports</li>
 * </ul>
 *
 * @author ianjones
 * @see PresenceCell
 * @see NetworkPresence
 */
@Value
@Builder
public class PresenceMatrix {

    /** Ordered list of ASNs (rows). */
    List<Long> asns;

    /** ASN to human-readable label. */
    Map<Long, String> asnLabels;

    /** Ordered list of metros (columns). */
    List<MetroCode> metros;

    /** The matrix data: ASN → Metro → Cell. */
    Map<Long, Map<MetroCode, PresenceCell>> cells;

    /**
     * Gets the cell for a specific ASN at a specific metro.
     *
     * @param asn   the autonomous system number
     * @param metro the Equinix metro code
     * @return the presence cell, or {@code null} if the combination is not in the matrix
     */
    public PresenceCell get(long asn, MetroCode metro) {
        Map<MetroCode, PresenceCell> row = cells.get(asn);
        return row != null ? row.get(metro) : null;
    }

    /**
     * Returns all cells for a given ASN (row view).
     *
     * @param asn the autonomous system number
     * @return map of metro → cell for this ASN, or empty map
     */
    public Map<MetroCode, PresenceCell> forAsn(long asn) {
        return cells.getOrDefault(asn, Collections.emptyMap());
    }

    /**
     * Returns all cells for a given metro (column view).
     *
     * @param metro the Equinix metro code
     * @return map of ASN → cell for this metro
     */
    public Map<Long, PresenceCell> forMetro(MetroCode metro) {
        Map<Long, PresenceCell> column = new LinkedHashMap<>();
        for (Long asn : asns) {
            PresenceCell cell = get(asn, metro);
            if (cell != null) {
                column.put(asn, cell);
            }
        }
        return column;
    }

    /**
     * Returns metros where ALL specified ASNs have IX peering presence.
     *
     * @param targetAsns the ASNs that must all be present
     * @return list of metros where all target ASNs peer at Equinix IXes
     */
    public List<MetroCode> metrosWithAllAsns(Collection<Long> targetAsns) {
        return metros.stream()
                .filter(metro -> targetAsns.stream()
                        .allMatch(asn -> {
                            PresenceCell cell = get(asn, metro);
                            return cell != null && cell.isIxPresent();
                        }))
                .collect(Collectors.toList());
    }

    /**
     * Returns metros where ANY of the specified ASNs have IX peering presence.
     *
     * @param targetAsns the ASNs to check
     * @return list of metros where at least one target ASN peers at an Equinix IX
     */
    public List<MetroCode> metrosWithAnyAsn(Collection<Long> targetAsns) {
        return metros.stream()
                .filter(metro -> targetAsns.stream()
                        .anyMatch(asn -> {
                            PresenceCell cell = get(asn, metro);
                            return cell != null && cell.isIxPresent();
                        }))
                .collect(Collectors.toList());
    }

    /**
     * Counts how many of the target ASNs have IX peering at each metro.
     *
     * @return map of metro → count of ASNs with IX presence
     */
    public Map<MetroCode, Integer> asnCountByMetro() {
        Map<MetroCode, Integer> counts = new LinkedHashMap<>();
        for (MetroCode metro : metros) {
            int count = 0;
            for (Long asn : asns) {
                PresenceCell cell = get(asn, metro);
                if (cell != null && cell.isIxPresent()) count++;
            }
            counts.put(metro, count);
        }
        return counts;
    }

    /**
     * Renders the matrix as a compact ASCII table.
     *
     * @return formatted ASCII table with ASNs as rows and metros as columns
     */
    public String toTableString() {
        return renderTable(false);
    }

    /**
     * Renders the matrix as a detailed ASCII table with capacity annotations.
     *
     * @return formatted ASCII table with capacity and route server indicators
     */
    public String toDetailedTableString() {
        return renderTable(true);
    }

    /**
     * Renders the matrix as a Markdown table.
     *
     * @return Markdown-formatted table
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("| ASN | Network |");
        for (MetroCode m : metros) sb.append(" ").append(m.name()).append(" |");
        sb.append("\n|-----|---------|");
        for (int i = 0; i < metros.size(); i++) sb.append("------|");
        sb.append("\n");

        for (Long asn : asns) {
            String label = asnLabels.getOrDefault(asn, String.valueOf(asn));
            sb.append("| ").append(asn).append(" | ").append(label).append(" |");
            for (MetroCode metro : metros) {
                PresenceCell cell = get(asn, metro);
                String sym = cell != null ? cell.symbol() : " -- ";
                sb.append(" ").append(sym).append(" |");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String renderTable(boolean detailed) {
        int labelWidth = 16;
        int cellWidth = detailed ? 12 : 6;

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.format("%-" + labelWidth + "s", "Network"));
        for (MetroCode m : metros) {
            sb.append(String.format("%" + cellWidth + "s", m.name()));
        }
        sb.append("\n");
        sb.append(repeat("-", labelWidth + metros.size() * cellWidth));
        sb.append("\n");

        // Rows
        for (Long asn : asns) {
            String label = asnLabels.getOrDefault(asn, "AS" + asn);
            if (label.length() > labelWidth - 1) label = label.substring(0, labelWidth - 1);
            sb.append(String.format("%-" + labelWidth + "s", label));

            for (MetroCode metro : metros) {
                PresenceCell cell = get(asn, metro);
                String sym = cell != null
                        ? (detailed ? cell.detailedSymbol() : cell.symbol())
                        : (detailed ? "---" : " -- ");
                sb.append(String.format("%" + cellWidth + "s", sym));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}
