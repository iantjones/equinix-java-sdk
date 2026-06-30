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
import api.equinix.javasdk.design.peering.client.PeeringDbFacility;
import api.equinix.javasdk.design.peering.client.PeeringDbIx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges PeeringDB Equinix IX and facility IDs to Equinix Fabric {@link MetroId} values entirely
 * from live data — no hardcoded city table.
 *
 * <p>Equinix names every IBX (facility) with its metro as a prefix — {@code LA3}/{@code LA4} are in
 * Los Angeles, {@code SV5} in Silicon Valley, {@code DC11} in Ashburn. So a facility is resolved by
 * reading the IBX code out of its PeeringDB name and looking it up in the live IBX&rarr;metro map
 * supplied at construction (built from {@code fabric.metros().getIbxs()}); an exact IBX hit wins, and
 * the metro prefix is a fallback (accepted only when it names a metro present in the live map, so a
 * brand-new metro resolves while a stray token does not). Each resolved facility also seeds a
 * city&rarr;metro map, through which IXes — which carry only a city — resolve. This means even city
 * aliases (a "San Jose" exchange co-located with the Silicon Valley {@code SV} facilities) map
 * correctly, with no static table and without depending on coordinates.</p>
 *
 * <p>Because the bridge is keyed by {@link MetroId} (not the {@code MetroCode} enum), a metro that is
 * live in Fabric but not yet in the enum still resolves and stays distinct.</p>
 *
 * <p>Call {@link #mapFacilities} <em>before</em> {@link #mapIxes} (the IX bridge depends on the
 * facility pass).</p>
 *
 * @author ianjones
 * @see MetroId
 */
public class EquinixIXMapping {

    /** An IBX-code-shaped token: a 2-3 letter metro prefix followed by 1-3 digits, e.g. {@code LA4}. */
    private static final Pattern IBX_TOKEN = Pattern.compile("[A-Za-z]{2,3}\\d{1,3}");

    private final Map<String, MetroId> ibxToMetro;

    /** Metro code &rarr; metro, derived from the live {@link #ibxToMetro} values; gates the prefix fallback. */
    private final Map<String, MetroId> codeToMetro = new LinkedHashMap<>();

    private final Map<String, MetroId> cityToMetro = new LinkedHashMap<>();
    private final Map<Integer, MetroId> ixIdToMetro = new LinkedHashMap<>();
    private final Map<Integer, MetroId> facIdToMetro = new LinkedHashMap<>();
    private final Map<MetroId, List<Integer>> metroToIxIds = new LinkedHashMap<>();
    private final Map<MetroId, List<Integer>> metroToFacIds = new LinkedHashMap<>();

    /**
     * @param ibxToMetro live IBX-code &rarr; metro map (upper-cased keys), typically built from
     *                  {@code fabric.metros()} and each metro's {@code getIbxs()}. A {@code null} map
     *                  yields an empty bridge.
     */
    public EquinixIXMapping(Map<String, MetroId> ibxToMetro) {
        this.ibxToMetro = ibxToMetro != null ? ibxToMetro : Collections.emptyMap();
        for (MetroId metro : this.ibxToMetro.values()) {
            if (metro != null) {
                codeToMetro.putIfAbsent(metro.code(), metro);
            }
        }
    }

    /**
     * Binds each Equinix facility to its metro by reading the IBX code out of the facility name, and
     * seeds the city&rarr;metro map used by {@link #mapIxes}. Call this before {@link #mapIxes}.
     *
     * @param equinixFacs the Equinix facility map from PeeringDB (fac ID &rarr; facility metadata)
     */
    public void mapFacilities(Map<Integer, PeeringDbFacility> equinixFacs) {
        if (equinixFacs == null) {
            return;
        }
        for (Map.Entry<Integer, PeeringDbFacility> entry : equinixFacs.entrySet()) {
            PeeringDbFacility fac = entry.getValue();
            MetroId metro = resolveFromName(fac.getName());
            if (metro == null) {
                metro = resolveFromName(fac.getAka());
            }
            if (metro == null) {
                continue;
            }
            facIdToMetro.put(entry.getKey(), metro);
            metroToFacIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(entry.getKey());
            if (fac.getCity() != null) {
                cityToMetro.putIfAbsent(normalize(fac.getCity()), metro);
            }
        }
    }

    /**
     * Binds each Equinix IX to a metro via the facility-derived city&rarr;metro map (IXes carry only a
     * city, not an IBX code). Call {@link #mapFacilities} first.
     *
     * @param equinixIxes the Equinix IX map from PeeringDB (IX ID &rarr; IX metadata)
     */
    public void mapIxes(Map<Integer, PeeringDbIx> equinixIxes) {
        if (equinixIxes == null) {
            return;
        }
        for (Map.Entry<Integer, PeeringDbIx> entry : equinixIxes.entrySet()) {
            MetroId metro = cityToMetro.get(normalize(entry.getValue().getCity()));
            if (metro != null) {
                ixIdToMetro.put(entry.getKey(), metro);
                metroToIxIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
    }

    /**
     * Reads an Equinix IBX code out of a name (e.g. {@code "Equinix LA4 - Los Angeles"}) and resolves
     * it to a metro: an exact match against the live IBX&rarr;metro map wins; otherwise the IBX's
     * metro-code prefix is tried, accepted only when that prefix names a metro present in the live map.
     *
     * @param name a PeeringDB facility name (or {@code aka})
     * @return the resolved metro, or {@code null} if the name carries no recognizable IBX code
     */
    public MetroId resolveFromName(String name) {
        if (name == null) {
            return null;
        }
        // First pass: an exact IBX-code hit is authoritative.
        Matcher matcher = IBX_TOKEN.matcher(name);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group().toUpperCase(Locale.ROOT);
            MetroId exact = ibxToMetro.get(token);
            if (exact != null) {
                return exact;
            }
            tokens.add(token);
        }
        // Second pass: fall back to the IBX's metro-code prefix (e.g. LA4 -> LA), but only if that
        // prefix is a metro we actually saw live — keeps new metros working without false positives.
        for (String token : tokens) {
            String prefix = token.replaceAll("\\d.*$", "");
            MetroId byPrefix = codeToMetro.get(prefix);
            if (byPrefix != null) {
                return byPrefix;
            }
        }
        return null;
    }

    /**
     * @param ixId the PeeringDB IX ID
     * @return the mapped {@link MetroId}, or {@code null} if unmapped
     */
    public MetroId metroForIx(int ixId) {
        return ixIdToMetro.get(ixId);
    }

    /**
     * @param facId the PeeringDB facility ID
     * @return the mapped {@link MetroId}, or {@code null} if unmapped
     */
    public MetroId metroForFacility(int facId) {
        return facIdToMetro.get(facId);
    }

    /**
     * @param metro the Equinix metro
     * @return the PeeringDB IX IDs in that metro, or an empty list
     */
    public List<Integer> ixIdsForMetro(MetroId metro) {
        return metroToIxIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * @param metro the Equinix metro
     * @return the PeeringDB facility IDs in that metro, or an empty list
     */
    public List<Integer> facIdsForMetro(MetroId metro) {
        return metroToFacIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * @return the metros with at least one mapped IX (unmodifiable)
     */
    public Set<MetroId> metrosWithIx() {
        return Collections.unmodifiableSet(metroToIxIds.keySet());
    }

    /**
     * @return the metros with at least one mapped facility (unmodifiable)
     */
    public Set<MetroId> metrosWithFacilities() {
        return Collections.unmodifiableSet(metroToFacIds.keySet());
    }

    private static String normalize(String city) {
        return city == null ? "" : city.toLowerCase(Locale.ROOT).trim();
    }
}
