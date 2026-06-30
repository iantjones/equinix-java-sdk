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
 * <p>The snapshot is immutable; call {@link #load(Metros)} again (or {@code Fabric.reloadMetroRegistry()})
 * to pick up metros added since it was built.</p>
 *
 * <pre>{@code
 * MetroRegistry registry = fabric.metroRegistry();
 * registry.get("SV").ifPresent(m -> System.out.println(m.getName() + " " + m.getIbxs()));
 * boolean exists = registry.contains(newMetroCode);   // works even if not in the MetroCode enum
 * }</pre>
 *
 * @author ianjones
 */
public final class MetroRegistry {

    private final Map<String, Metro> byCode;

    private MetroRegistry(Map<String, Metro> byCode) {
        this.byCode = byCode;
    }

    /**
     * Loads the registry by fetching every metro from the Metros API. Metros are keyed by their
     * {@link MetroId} (the exact wire code), so distinct unlisted metros do not collide.
     *
     * @param metros the Metros client to read from (e.g. {@code fabric.metros()})
     * @return a populated, immutable registry
     */
    public static MetroRegistry load(Metros metros) {
        Map<String, Metro> map = new LinkedHashMap<>();
        // loadAll() pages through the full catalogue — list() alone returns only the first page.
        for (Metro metro : metros.list().loadAll()) {
            MetroId id = metro.metroId();
            if (id != null) {
                map.put(id.code(), metro);
            }
        }
        return new MetroRegistry(Collections.unmodifiableMap(map));
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
}
