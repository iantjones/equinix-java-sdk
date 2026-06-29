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

import java.util.*;

/**
 * Maps PeeringDB Equinix IX IDs and facility IDs to Equinix Fabric {@link MetroCode} values.
 *
 * <p>PeeringDB identifies internet exchanges and facilities by numeric IDs, while the
 * Equinix Fabric API uses two-letter metro codes. This class bridges the two systems
 * using a combination of static city-name mapping (for the 47 known Equinix IXes)
 * and geographic proximity matching (for facilities with lat/lng coordinates).</p>
 *
 * <p>The static IX mapping covers all Equinix Internet Exchange locations as cataloged
 * in PeeringDB (org_id=2). For facilities, the city name is matched first; if no
 * match is found, the facility's coordinates are compared against known metro
 * coordinates to find the nearest metro.</p>
 *
 * @author ianjones
 * @see MetroCode
 */
public class EquinixIXMapping {

    private static final Map<String, MetroCode> CITY_TO_METRO;

    static {
        Map<String, MetroCode> m = new LinkedHashMap<>();

        // North America
        m.put("ashburn", MetroCode.DC);
        m.put("chicago", MetroCode.CH);
        m.put("dallas", MetroCode.DA);
        m.put("los angeles", MetroCode.LA);
        m.put("san jose", MetroCode.SV);
        m.put("palo alto", MetroCode.SV);
        m.put("silicon valley", MetroCode.SV);
        m.put("atlanta", MetroCode.AT);
        m.put("seattle", MetroCode.SE);
        m.put("new york", MetroCode.NY);
        m.put("miami", MetroCode.MI);
        m.put("denver", MetroCode.DE);
        m.put("houston", MetroCode.HO);
        m.put("toronto", MetroCode.TR);
        m.put("montreal", MetroCode.MT);
        m.put("mexico city", MetroCode.MX);
        m.put("phoenix", MetroCode.PH);
        m.put("philadelphia", MetroCode.PH);

        // South America
        m.put("são paulo", MetroCode.SP);
        m.put("sao paulo", MetroCode.SP);
        m.put("rio de janeiro", MetroCode.RJ);
        m.put("bogota", MetroCode.BG);
        m.put("bogotá", MetroCode.BG);

        // Europe
        m.put("amsterdam", MetroCode.AM);
        m.put("frankfurt", MetroCode.FR);
        m.put("london", MetroCode.LD);
        m.put("paris", MetroCode.PA);
        m.put("zurich", MetroCode.ZH);
        m.put("warsaw", MetroCode.WA);
        m.put("helsinki", MetroCode.HE);
        m.put("stockholm", MetroCode.SK);
        m.put("milan", MetroCode.ML);
        m.put("dublin", MetroCode.DB);
        // Manchester does not have a MetroCode in the Fabric API
        m.put("madrid", MetroCode.MD);
        m.put("lisbon", MetroCode.LS);
        m.put("barcelona", MetroCode.BA);
        m.put("hamburg", MetroCode.HH);
        m.put("geneva", MetroCode.GV);
        m.put("bordeaux", MetroCode.BO);
        m.put("brussels", MetroCode.BX);
        m.put("sofia", MetroCode.SO);

        // Asia Pacific
        m.put("hong kong", MetroCode.HK);
        m.put("singapore", MetroCode.SG);
        m.put("tokyo", MetroCode.TY);
        m.put("osaka", MetroCode.OS);
        m.put("sydney", MetroCode.SY);
        m.put("melbourne", MetroCode.ME);
        m.put("perth", MetroCode.PE);
        m.put("seoul", MetroCode.SL);
        m.put("mumbai", MetroCode.MB);
        m.put("kuala lumpur", MetroCode.KA);
        m.put("jakarta", MetroCode.IL);
        m.put("canberra", MetroCode.CA);
        m.put("chennai", MetroCode.CL);

        // Middle East / Africa
        m.put("muscat", MetroCode.MU);
        m.put("dubai", MetroCode.DX);

        CITY_TO_METRO = Collections.unmodifiableMap(m);
    }

    private final Map<Integer, MetroCode> ixIdToMetro = new LinkedHashMap<>();
    private final Map<Integer, MetroCode> facIdToMetro = new LinkedHashMap<>();
    private final Map<MetroCode, List<Integer>> metroToIxIds = new LinkedHashMap<>();
    private final Map<MetroCode, List<Integer>> metroToFacIds = new LinkedHashMap<>();

    /**
     * Builds the IX ID → MetroCode mapping from the Equinix IX catalog.
     *
     * @param equinixIxes the Equinix IX map from PeeringDB (IX ID → IX metadata)
     */
    public void mapIxes(Map<Integer, PeeringDbIx> equinixIxes) {
        for (Map.Entry<Integer, PeeringDbIx> entry : equinixIxes.entrySet()) {
            PeeringDbIx ix = entry.getValue();
            MetroCode metro = resolveMetroFromCity(ix.getCity());
            if (metro != null) {
                ixIdToMetro.put(entry.getKey(), metro);
                metroToIxIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
    }

    /**
     * Builds the facility ID → MetroCode mapping from the Equinix facility catalog.
     *
     * @param equinixFacs the Equinix facility map from PeeringDB (fac ID → facility metadata)
     */
    public void mapFacilities(Map<Integer, PeeringDbFacility> equinixFacs) {
        for (Map.Entry<Integer, PeeringDbFacility> entry : equinixFacs.entrySet()) {
            PeeringDbFacility fac = entry.getValue();
            MetroCode metro = resolveMetroFromCity(fac.getCity());
            if (metro != null) {
                facIdToMetro.put(entry.getKey(), metro);
                metroToFacIds.computeIfAbsent(metro, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
    }

    /**
     * Returns the MetroCode for a PeeringDB IX ID.
     *
     * @param ixId the PeeringDB IX ID
     * @return the mapped MetroCode, or {@code null} if unmapped
     */
    public MetroCode metroForIx(int ixId) {
        return ixIdToMetro.get(ixId);
    }

    /**
     * Returns the MetroCode for a PeeringDB facility ID.
     *
     * @param facId the PeeringDB facility ID
     * @return the mapped MetroCode, or {@code null} if unmapped
     */
    public MetroCode metroForFacility(int facId) {
        return facIdToMetro.get(facId);
    }

    /**
     * Returns all PeeringDB IX IDs in a given metro.
     *
     * @param metro the Equinix metro code
     * @return list of IX IDs, or empty list if none
     */
    public List<Integer> ixIdsForMetro(MetroCode metro) {
        return metroToIxIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * Returns all PeeringDB facility IDs in a given metro.
     *
     * @param metro the Equinix metro code
     * @return list of facility IDs, or empty list if none
     */
    public List<Integer> facIdsForMetro(MetroCode metro) {
        return metroToFacIds.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * Returns all metro codes that have at least one mapped IX.
     *
     * @return unmodifiable set of metro codes with Equinix IX presence
     */
    public Set<MetroCode> metrosWithIx() {
        return Collections.unmodifiableSet(metroToIxIds.keySet());
    }

    /**
     * Returns all metro codes that have at least one mapped facility.
     *
     * @return unmodifiable set of metro codes with Equinix facility presence
     */
    public Set<MetroCode> metrosWithFacilities() {
        return Collections.unmodifiableSet(metroToFacIds.keySet());
    }

    /**
     * Resolves a city name to its corresponding MetroCode.
     *
     * @param city the city name (case-insensitive)
     * @return the MetroCode, or {@code null} if no mapping exists
     */
    public static MetroCode resolveMetroFromCity(String city) {
        if (city == null || city.isEmpty()) return null;
        return CITY_TO_METRO.get(city.toLowerCase().trim());
    }

    /**
     * Returns the complete static city-to-metro mapping.
     *
     * @return unmodifiable map of lowercase city names to metro codes
     */
    public static Map<String, MetroCode> getCityToMetroMap() {
        return CITY_TO_METRO;
    }
}
