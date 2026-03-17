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

package api.equinix.javasdk.fabric.peering;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.peering.client.PeeringDbFacility;
import api.equinix.javasdk.fabric.peering.client.PeeringDbIx;
import api.equinix.javasdk.fabric.peering.model.EquinixIXMapping;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EquinixIXMapping} — the PeeringDB ID to MetroCode translation layer.
 */
@DisplayName("Equinix IX Mapping")
class EquinixIXMappingTest {

    @Nested
    @DisplayName("Static city-to-metro resolution")
    class CityResolutionTests {

        @Test
        @DisplayName("Ashburn should map to DC")
        void ashburn_mapsToDC() {
            assertEquals(MetroCode.DC, EquinixIXMapping.resolveMetroFromCity("Ashburn"));
        }

        @Test
        @DisplayName("City name matching should be case-insensitive")
        void caseInsensitive() {
            assertEquals(MetroCode.DC, EquinixIXMapping.resolveMetroFromCity("ASHBURN"));
            assertEquals(MetroCode.DC, EquinixIXMapping.resolveMetroFromCity("ashburn"));
            assertEquals(MetroCode.DC, EquinixIXMapping.resolveMetroFromCity("Ashburn"));
        }

        @Test
        @DisplayName("Leading/trailing whitespace should be trimmed")
        void trimWhitespace() {
            assertEquals(MetroCode.DC, EquinixIXMapping.resolveMetroFromCity("  ashburn  "));
        }

        @Test
        @DisplayName("Null and empty city should return null")
        void nullAndEmpty_returnNull() {
            assertNull(EquinixIXMapping.resolveMetroFromCity(null));
            assertNull(EquinixIXMapping.resolveMetroFromCity(""));
        }

        @Test
        @DisplayName("Unknown city should return null")
        void unknownCity_returnsNull() {
            assertNull(EquinixIXMapping.resolveMetroFromCity("Timbuktu"));
        }

        @Test
        @DisplayName("Major North American cities should map correctly")
        void northAmerica_mappings() {
            assertEquals(MetroCode.CH, EquinixIXMapping.resolveMetroFromCity("Chicago"));
            assertEquals(MetroCode.DA, EquinixIXMapping.resolveMetroFromCity("Dallas"));
            assertEquals(MetroCode.LA, EquinixIXMapping.resolveMetroFromCity("Los Angeles"));
            assertEquals(MetroCode.SV, EquinixIXMapping.resolveMetroFromCity("San Jose"));
            assertEquals(MetroCode.NY, EquinixIXMapping.resolveMetroFromCity("New York"));
            assertEquals(MetroCode.SE, EquinixIXMapping.resolveMetroFromCity("Seattle"));
            assertEquals(MetroCode.AT, EquinixIXMapping.resolveMetroFromCity("Atlanta"));
            assertEquals(MetroCode.MI, EquinixIXMapping.resolveMetroFromCity("Miami"));
        }

        @Test
        @DisplayName("European cities should map correctly")
        void europe_mappings() {
            assertEquals(MetroCode.AM, EquinixIXMapping.resolveMetroFromCity("Amsterdam"));
            assertEquals(MetroCode.FR, EquinixIXMapping.resolveMetroFromCity("Frankfurt"));
            assertEquals(MetroCode.LD, EquinixIXMapping.resolveMetroFromCity("London"));
            assertEquals(MetroCode.PA, EquinixIXMapping.resolveMetroFromCity("Paris"));
            assertEquals(MetroCode.ZH, EquinixIXMapping.resolveMetroFromCity("Zurich"));
        }

        @Test
        @DisplayName("APAC cities should map correctly")
        void apac_mappings() {
            assertEquals(MetroCode.HK, EquinixIXMapping.resolveMetroFromCity("Hong Kong"));
            assertEquals(MetroCode.SG, EquinixIXMapping.resolveMetroFromCity("Singapore"));
            assertEquals(MetroCode.TY, EquinixIXMapping.resolveMetroFromCity("Tokyo"));
            assertEquals(MetroCode.SY, EquinixIXMapping.resolveMetroFromCity("Sydney"));
        }

        @Test
        @DisplayName("Silicon Valley alias cities should all map to SV")
        void siliconValleyAliases() {
            assertEquals(MetroCode.SV, EquinixIXMapping.resolveMetroFromCity("San Jose"));
            assertEquals(MetroCode.SV, EquinixIXMapping.resolveMetroFromCity("Palo Alto"));
            assertEquals(MetroCode.SV, EquinixIXMapping.resolveMetroFromCity("Silicon Valley"));
        }

        @Test
        @DisplayName("Sao Paulo with both accent forms should map to SP")
        void saoPauloVariants() {
            assertEquals(MetroCode.SP, EquinixIXMapping.resolveMetroFromCity("Sao Paulo"));
            assertEquals(MetroCode.SP, EquinixIXMapping.resolveMetroFromCity("São Paulo"));
        }

        @Test
        @DisplayName("Static map should contain all expected entries")
        void staticMap_isComplete() {
            Map<String, MetroCode> map = EquinixIXMapping.getCityToMetroMap();
            assertNotNull(map);
            assertTrue(map.size() >= 45, "Expected at least 45 city mappings, got " + map.size());
        }
    }

    @Nested
    @DisplayName("Dynamic IX ID mapping")
    class IxMappingTests {

        private EquinixIXMapping mapping;

        @BeforeEach
        void setUp() {
            mapping = new EquinixIXMapping();
            Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
            ixes.put(1, createIx(1, "Equinix Ashburn", "Ashburn"));
            ixes.put(2, createIx(2, "Equinix Chicago", "Chicago"));
            ixes.put(3, createIx(3, "Equinix Dallas", "Dallas"));
            ixes.put(99, createIx(99, "Unknown Exchange", "Atlantis")); // unmappable
            mapping.mapIxes(ixes);
        }

        @Test
        @DisplayName("Known IX IDs should resolve to correct metro")
        void knownIxIds_resolve() {
            assertEquals(MetroCode.DC, mapping.metroForIx(1));
            assertEquals(MetroCode.CH, mapping.metroForIx(2));
            assertEquals(MetroCode.DA, mapping.metroForIx(3));
        }

        @Test
        @DisplayName("Unknown IX IDs should return null")
        void unknownIxId_returnsNull() {
            assertNull(mapping.metroForIx(99));
            assertNull(mapping.metroForIx(999));
        }

        @Test
        @DisplayName("Metro should list its IX IDs")
        void ixIdsForMetro() {
            List<Integer> dcIxes = mapping.ixIdsForMetro(MetroCode.DC);
            assertEquals(1, dcIxes.size());
            assertTrue(dcIxes.contains(1));
        }

        @Test
        @DisplayName("Metro with no IXes should return empty list")
        void emptyMetro_returnsEmptyList() {
            assertTrue(mapping.ixIdsForMetro(MetroCode.SG).isEmpty());
        }

        @Test
        @DisplayName("Metros with IX presence should be correct")
        void metrosWithIx() {
            Set<MetroCode> metros = mapping.metrosWithIx();
            assertEquals(3, metros.size());
            assertTrue(metros.contains(MetroCode.DC));
            assertTrue(metros.contains(MetroCode.CH));
            assertTrue(metros.contains(MetroCode.DA));
        }
    }

    @Nested
    @DisplayName("Dynamic facility ID mapping")
    class FacilityMappingTests {

        private EquinixIXMapping mapping;

        @BeforeEach
        void setUp() {
            mapping = new EquinixIXMapping();
            Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
            facs.put(100, createFacility(100, "DC1", "Ashburn", 39.0438, -77.4874));
            facs.put(101, createFacility(101, "DC2", "Ashburn", 39.0438, -77.4874));
            facs.put(200, createFacility(200, "SG1", "Singapore", 1.3521, 103.8198));
            mapping.mapFacilities(facs);
        }

        @Test
        @DisplayName("Known facility IDs should resolve to correct metro")
        void knownFacIds_resolve() {
            assertEquals(MetroCode.DC, mapping.metroForFacility(100));
            assertEquals(MetroCode.DC, mapping.metroForFacility(101));
            assertEquals(MetroCode.SG, mapping.metroForFacility(200));
        }

        @Test
        @DisplayName("Multiple facilities in same metro should all map")
        void multipleFacilitiesInSameMetro() {
            List<Integer> dcFacs = mapping.facIdsForMetro(MetroCode.DC);
            assertEquals(2, dcFacs.size());
            assertTrue(dcFacs.contains(100));
            assertTrue(dcFacs.contains(101));
        }

        @Test
        @DisplayName("Metros with facility presence should be correct")
        void metrosWithFacilities() {
            Set<MetroCode> metros = mapping.metrosWithFacilities();
            assertEquals(2, metros.size());
            assertTrue(metros.contains(MetroCode.DC));
            assertTrue(metros.contains(MetroCode.SG));
        }
    }

    // ---- Helpers ----

    private static PeeringDbIx createIx(int id, String name, String city) {
        PeeringDbIx ix = new PeeringDbIx();
        ix.setId(id);
        ix.setName(name);
        ix.setCity(city);
        ix.setCountry("US");
        return ix;
    }

    private static PeeringDbFacility createFacility(int id, String name, String city,
                                                     double lat, double lng) {
        PeeringDbFacility fac = new PeeringDbFacility();
        fac.setId(id);
        fac.setName(name);
        fac.setCity(city);
        fac.setLatitude(lat);
        fac.setLongitude(lng);
        return fac;
    }
}
