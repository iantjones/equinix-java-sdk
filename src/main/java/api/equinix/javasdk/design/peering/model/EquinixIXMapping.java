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
import api.equinix.javasdk.design.peering.client.PeeringDbFacility;
import api.equinix.javasdk.design.peering.client.PeeringDbIx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bridges PeeringDB Equinix IX and facility IDs to Equinix Fabric {@link MetroCode} values entirely
 * from live data — no hardcoded city table.
 *
 * <p>PeeringDB identifies internet exchanges and facilities by numeric IDs; Fabric uses metro codes.
 * The bridge is derived at runtime:</p>
 * <ul>
 *   <li>Each PeeringDB <strong>facility</strong> carries a latitude/longitude, so it is bound to the
 *       <em>nearest live Fabric metro</em> (within {@value #MAX_BIND_KM} km) using the metro
 *       coordinates supplied at construction. Facilities also seed a city&rarr;metro map.</li>
 *   <li>Each PeeringDB <strong>IX</strong> carries only a city (no coordinates), so it resolves
 *       through the facility-derived city&rarr;metro map — which means even city aliases (e.g. a
 *       "San Jose" exchange co-located with a Silicon Valley facility) map correctly, without any
 *       static lookup table.</li>
 * </ul>
 *
 * <p>Construct with the live metro coordinates (from {@code fabric.metros()}), then call
 * {@link #mapFacilities} <em>before</em> {@link #mapIxes} (the IX bridge depends on the facility
 * pass).</p>
 *
 * @author ianjones
 * @see MetroCode
 */
public class EquinixIXMapping {

    /** Maximum distance from a facility to a metro for the two to be considered co-located. */
    static final double MAX_BIND_KM = 150.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final Map<MetroCode, double[]> metroCoordinates;

    private final Map<String, MetroCode> cityToMetro = new LinkedHashMap<>();
    private final Map<Integer, MetroCode> ixIdToMetro = new LinkedHashMap<>();
    private final Map<Integer, MetroCode> facIdToMetro = new LinkedHashMap<>();
    private final Map<MetroCode, List<Integer>> metroToIxIds = new LinkedHashMap<>();
    private final Map<MetroCode, List<Integer>> metroToFacIds = new LinkedHashMap<>();

    /**
     * @param metroCoordinates live Fabric metro coordinates ({@code MetroCode -> [latitude, longitude]}),
     *                         typically built from {@code fabric.metros()}; only well-known metros need
     *                         appear. A {@code null} map yields an empty bridge.
     */
    public EquinixIXMapping(Map<MetroCode, double[]> metroCoordinates) {
        this.metroCoordinates = metroCoordinates != null ? metroCoordinates : Collections.emptyMap();
    }

    /**
     * Binds each Equinix facility to the nearest live metro by coordinates, and seeds the
     * city&rarr;metro map used by {@link #mapIxes}. Call this before {@link #mapIxes}.
     *
     * @param equinixFacs the Equinix facility map from PeeringDB (fac ID &rarr; facility metadata)
     */
    public void mapFacilities(Map<Integer, PeeringDbFacility> equinixFacs) {
        if (equinixFacs == null) {
            return;
        }
        // Pass 1: facilities with coordinates -> nearest metro; these seed the city bridge.
        for (Map.Entry<Integer, PeeringDbFacility> entry : equinixFacs.entrySet()) {
            PeeringDbFacility fac = entry.getValue();
            if (fac.getLatitude() == null || fac.getLongitude() == null) {
                continue;
            }
            MetroCode metro = nearestMetro(fac.getLatitude(), fac.getLongitude());
            if (metro == null) {
                continue;
            }
            bindFacility(entry.getKey(), metro);
            if (fac.getCity() != null) {
                cityToMetro.putIfAbsent(normalize(fac.getCity()), metro);
            }
        }
        // Pass 2: facilities lacking coordinates fall back to the city bridge from pass 1.
        for (Map.Entry<Integer, PeeringDbFacility> entry : equinixFacs.entrySet()) {
            if (facIdToMetro.containsKey(entry.getKey())) {
                continue;
            }
            MetroCode metro = cityToMetro.get(normalize(entry.getValue().getCity()));
            if (metro != null) {
                bindFacility(entry.getKey(), metro);
            }
        }
    }

    /**
     * Binds each Equinix IX to a metro via the facility-derived city&rarr;metro map (IXes have no
     * coordinates of their own). Call {@link #mapFacilities} first.
     *
     * @param equinixIxes the Equinix IX map from PeeringDB (IX ID &rarr; IX metadata)
     */
    public void mapIxes(Map<Integer, PeeringDbIx> equinixIxes) {
        if (equinixIxes == null) {
            return;
        }
        for (Map.Entry<Integer, PeeringDbIx> entry : equinixIxes.entrySet()) {
            MetroCode metro = cityToMetro.get(normalize(entry.getValue().getCity()));
            if (metro != null) {
                ixIdToMetro.put(entry.getKey(), metro);
                metroToIxIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
    }

    private void bindFacility(int facId, MetroCode metro) {
        facIdToMetro.put(facId, metro);
        metroToFacIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(facId);
    }

    /**
     * @return the nearest metro to the given coordinates, or {@code null} if none is within
     *         {@value #MAX_BIND_KM} km (or no metro coordinates are known)
     */
    public MetroCode nearestMetro(double latitude, double longitude) {
        MetroCode best = null;
        double bestKm = Double.MAX_VALUE;
        for (Map.Entry<MetroCode, double[]> entry : metroCoordinates.entrySet()) {
            double[] coords = entry.getValue();
            if (coords == null || coords.length < 2) {
                continue;
            }
            double km = haversineKm(latitude, longitude, coords[0], coords[1]);
            if (km < bestKm) {
                bestKm = km;
                best = entry.getKey();
            }
        }
        return bestKm <= MAX_BIND_KM ? best : null;
    }

    /**
     * @param ixId the PeeringDB IX ID
     * @return the mapped {@link MetroCode}, or {@code null} if unmapped
     */
    public MetroCode metroForIx(int ixId) {
        return ixIdToMetro.get(ixId);
    }

    /**
     * @param facId the PeeringDB facility ID
     * @return the mapped {@link MetroCode}, or {@code null} if unmapped
     */
    public MetroCode metroForFacility(int facId) {
        return facIdToMetro.get(facId);
    }

    /**
     * @param metro the Equinix metro code
     * @return the PeeringDB IX IDs in that metro, or an empty list
     */
    public List<Integer> ixIdsForMetro(MetroCode metro) {
        return metroToIxIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * @param metro the Equinix metro code
     * @return the PeeringDB facility IDs in that metro, or an empty list
     */
    public List<Integer> facIdsForMetro(MetroCode metro) {
        return metroToFacIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * @return the metros with at least one mapped IX (unmodifiable)
     */
    public Set<MetroCode> metrosWithIx() {
        return Collections.unmodifiableSet(metroToIxIds.keySet());
    }

    /**
     * @return the metros with at least one mapped facility (unmodifiable)
     */
    public Set<MetroCode> metrosWithFacilities() {
        return Collections.unmodifiableSet(metroToFacIds.keySet());
    }

    private static String normalize(String city) {
        return city == null ? "" : city.toLowerCase(Locale.ROOT).trim();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
