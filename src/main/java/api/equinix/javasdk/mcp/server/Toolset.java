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

package api.equinix.javasdk.mcp.server;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The named tool groups the embedded MCP server can expose, selectable via
 * {@code EquinixMcpServer.builder().toolsets(...)}, the {@code EQUINIX_MCP_TOOLSETS}
 * environment variable, or the {@code --toolsets} launch argument (comma-separated ids,
 * e.g. {@code design,ibx}). When nothing is selected every <em>read-only</em> toolset is
 * served ({@link #defaults()}); the {@link #MUTATE mutate} toolset — the Safe Mutation
 * Broker — is never on by default and must be named explicitly (e.g.
 * {@code --toolsets design,mutate}).
 */
public enum Toolset {

    /** The design engines: optimizer, wizard, latency, TCO, savings, peering, Terraform export. */
    DESIGN("design"),

    /** Fabric-backed cross-domain intelligence (currently served by the design engines). */
    FABRIC("fabric"),

    /** Customer Portal reach: open tickets, billing summary. */
    PORTAL("portal"),

    /** Network Edge reach: virtual device inventory. */
    NE("ne"),

    /** IBX SmartView reach: environmental readings, power events. */
    IBX("ibx"),

    /**
     * The Safe Mutation Broker: dry-run-first two-phase creates
     * ({@code fabric_propose_change} / {@code fabric_confirm_change}). <strong>Off by
     * default</strong> — excluded from {@link #defaults()}, so it is served only when the
     * operator names it explicitly in the toolset selection.
     */
    MUTATE("mutate");

    private final String id;

    Toolset(String id) {
        this.id = id;
    }

    /**
     * @return the stable lower-case id used in {@code EQUINIX_MCP_TOOLSETS} / {@code --toolsets}
     */
    public String id() {
        return id;
    }

    /**
     * The default selection: every toolset <em>except</em> {@link #MUTATE}. The Safe Mutation
     * Broker is strictly opt-in — a default launch serves only read-only tools.
     *
     * @return all read-only toolsets
     */
    public static Set<Toolset> defaults() {
        return EnumSet.complementOf(EnumSet.of(MUTATE));
    }

    /**
     * Parses a single toolset id (case-insensitive, surrounding whitespace ignored).
     *
     * @param value the id, e.g. {@code "design"}
     * @return the matching toolset
     * @throws IllegalArgumentException if the id is unknown
     */
    public static Toolset fromId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.id.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown toolset '" + value + "'. Valid toolsets: design, fabric, portal, ne, "
                                + "ibx, mutate."));
    }

    /**
     * Parses a comma-separated toolset list ({@code "design,ibx"}). Blank input selects the
     * {@link #defaults() defaults} — every toolset except {@code mutate}, which must always
     * be named explicitly.
     *
     * @param csv the comma-separated ids, possibly {@code null} or blank
     * @return the selected toolsets (never empty)
     * @throws IllegalArgumentException if any id is unknown
     */
    public static Set<Toolset> parse(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return defaults();
        }
        Set<Toolset> selected = EnumSet.noneOf(Toolset.class);
        for (String part : csv.split(",")) {
            if (!part.trim().isEmpty()) {
                selected.add(fromId(part));
            }
        }
        return selected.isEmpty() ? defaults() : selected;
    }
}
