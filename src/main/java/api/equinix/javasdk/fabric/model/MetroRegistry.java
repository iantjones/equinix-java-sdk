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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.internetaccess.model.Ibx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An in-memory snapshot of every metro the Metros API returns, keyed by {@link MetroId} so lookups
 * work for metros the {@link MetroCode} enum does not list. This is the authoritative, always-current
 * set of metros (and their IBX data centers) — built by loading {@code fabric.metros().list()} once
 * and caching the result — for code that needs to discover or validate metros rather than hard-code
 * the enum.
 *
 * <p><b>Cross-source enrichment.</b> Fabric's metros carry the metro-level picture (centroid
 * coordinates, connected-metro latency, IBX <em>codes</em>) but no per-IBX detail; that lives in
 * EIA's {@code /internetAccess/v2/ibxs} (per-IBX coordinates, country, metro association). When
 * built with {@code EquinixConfig.enrichMetroRegistry} (or via {@link #load(Metros, Collection)}),
 * the registry merges both so it is the SDK's most complete location directory: {@link #ibx(String)}
 * returns the full EIA {@link Ibx} — ready for {@code design.geo.SpeedOfLightLatency} IBX-to-IBX
 * math. The registry is deliberately cross-domain in that mode (fabric + internetaccess data, one
 * lookup surface).</p>
 *
 * <p>The snapshot is immutable; call {@link #load(Metros)} again (or {@code Fabric.reloadMetroRegistry()})
 * to pick up metros added since it was built.</p>
 *
 * <pre>{@code
 * MetroRegistry registry = fabric.metroRegistry();
 * registry.get("SV").ifPresent(m -> System.out.println(m.getName() + " " + m.getIbxs()));
 * boolean exists = registry.contains(newMetroCode);   // works even if not in the MetroCode enum
 *
 * // With EquinixConfig.enrichMetroRegistry(true):
 * Ibx sv5 = registry.ibx("SV5").orElseThrow();
 * double ms = SpeedOfLightLatency.roundTrip().millisBetween(sv5, registry.ibx("LA4").orElseThrow());
 * }</pre>
 *
 * @author ianjones
 */
public final class MetroRegistry {

    private final Map<String, Metro> byCode;

    private final Map<String, Ibx> ibxByCode;

    private MetroRegistry(Map<String, Metro> byCode, Map<String, Ibx> ibxByCode) {
        this.byCode = byCode;
        this.ibxByCode = ibxByCode;
    }

    /**
     * Loads the registry by fetching every metro from the Metros API. Metros are keyed by their
     * {@link MetroId} (the exact wire code), so distinct unlisted metros do not collide.
     *
     * @param metros the Metros client to read from (e.g. {@code fabric.metros()})
     * @return a populated, immutable registry
     */
    public static MetroRegistry load(Metros metros) {
        return load(metros, Collections.emptyList());
    }

    /**
     * Loads the registry and enriches it with per-IBX detail (typically the EIA ibx catalogue —
     * see the class notes on cross-source enrichment). IBXes are keyed case-insensitively by
     * {@code ibxCode}; duplicates keep the first occurrence.
     *
     * @param metros the Metros client to read from (e.g. {@code fabric.metros()})
     * @param ibxDetails per-IBX records to merge in (empty for an un-enriched registry)
     * @return a populated, immutable registry
     */
    public static MetroRegistry load(Metros metros, Collection<? extends Ibx> ibxDetails) {
        Map<String, Metro> map = new LinkedHashMap<>();
        // loadAll() pages through the full catalogue — list() alone returns only the first page.
        for (Metro metro : metros.list().loadAll()) {
            MetroId id = metro.metroId();
            if (id != null) {
                map.put(id.code(), metro);
            }
        }
        Map<String, Ibx> ibxMap = new LinkedHashMap<>();
        for (Ibx ibx : ibxDetails) {
            if (ibx != null && ibx.getIbxCode() != null) {
                ibxMap.putIfAbsent(ibx.getIbxCode().trim().toUpperCase(), ibx);
            }
        }
        return new MetroRegistry(Collections.unmodifiableMap(map), Collections.unmodifiableMap(ibxMap));
    }

    /**
     * @param metroId the metro to look up
     * @return the metro, or empty if not present (or {@code metroId} is null)
     */
    public Optional<Metro> get(MetroId metroId) {
        return metroId == null ? Optional.empty() : Optional.ofNullable(byCode.get(metroId.code()));
    }

    /**
     * @param code the raw metro code (e.g. {@code "SV"}); case-insensitive
     * @return the metro, or empty if not present (or {@code code} is null/blank)
     */
    public Optional<Metro> get(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return get(MetroId.of(code));
    }

    /**
     * @param code a well-known metro code
     * @return the metro, or empty if not present (or {@code code} is null)
     */
    public Optional<Metro> get(MetroCode code) {
        return code == null ? Optional.empty() : get(MetroId.of(code));
    }

    /**
     * @param code the raw metro code; case-insensitive
     * @return {@code true} if this registry contains a metro with that code
     */
    public boolean contains(String code) {
        return get(code).isPresent();
    }

    /**
     * @param code a well-known metro code
     * @return {@code true} if this registry contains that metro
     */
    public boolean contains(MetroCode code) {
        return get(code).isPresent();
    }

    /**
     * @return all metros in the registry (unmodifiable)
     */
    public Collection<Metro> all() {
        return Collections.unmodifiableCollection(byCode.values());
    }

    /**
     * @return the ids of every metro in the registry (unmodifiable)
     */
    public Set<MetroId> metroIds() {
        Set<MetroId> ids = new java.util.LinkedHashSet<>();
        for (String code : byCode.keySet()) {
            ids.add(MetroId.of(code));
        }
        return Collections.unmodifiableSet(ids);
    }

    /**
     * @param code the raw metro code; case-insensitive
     * @return the IBX data-center codes within the metro, or an empty list if the metro is unknown
     *         or reports none
     */
    public List<String> ibxs(String code) {
        return get(code).map(Metro::getIbxs).orElse(Collections.emptyList());
    }

    /**
     * @return the number of metros in the registry
     */
    public int size() {
        return byCode.size();
    }

    /**
     * Looks up per-IBX detail (coordinates, country, metro association) merged in at load time.
     * Populated only when the registry was built with enrichment — see
     * {@code EquinixConfig.enrichMetroRegistry} and the class notes.
     *
     * @param ibxCode the IBX data-center code (e.g. {@code "SV5"}); case-insensitive
     * @return the IBX record, or empty if unknown or the registry is not enriched
     */
    public Optional<Ibx> ibx(String ibxCode) {
        if (ibxCode == null || ibxCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ibxByCode.get(ibxCode.trim().toUpperCase()));
    }

    /**
     * Returns the enriched per-IBX records associated with a metro (matched on the record's own
     * {@code metroCode}). Empty when the registry is not enriched or the metro has no records —
     * {@link #ibxs(String)} still lists the metro's IBX <em>codes</em> from Fabric either way.
     *
     * @param metroCode the metro code (e.g. {@code "SV"}); case-insensitive
     * @return the IBX records within that metro (unmodifiable)
     */
    public List<Ibx> ibxDetails(String metroCode) {
        if (metroCode == null || metroCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = metroCode.trim().toUpperCase();
        List<Ibx> matches = new ArrayList<>();
        for (Ibx ibx : ibxByCode.values()) {
            if (ibx.getMetroCode() != null && normalized.equals(ibx.getMetroCode().trim().toUpperCase())) {
                matches.add(ibx);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * @return {@code true} if this registry carries merged per-IBX detail (built with
     *         {@code EquinixConfig.enrichMetroRegistry} and the EIA fetch succeeded)
     */
    public boolean isEnriched() {
        return !ibxByCode.isEmpty();
    }
}
