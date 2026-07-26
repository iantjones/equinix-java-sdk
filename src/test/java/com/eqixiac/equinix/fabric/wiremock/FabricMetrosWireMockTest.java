package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.MetroPresence;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Metros (read-only: list + get-by-code).
 */
class FabricMetrosWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("list()")
    class ListMetros {

        @Test
        @DisplayName("returns a paginated list of metros")
        void returnsMetros() {
            stubPaginatedGet(wireMock, "/fabric/v4/metros", "/json/fabric/paginated_metros_list.json");

            PaginatedList<Metro> metros = fabric.metros().list();

            assertNotNull(metros);
            assertEquals(2, metros.size());
            Metro first = metros.get(0);
            assertEquals(MetroCode.SV, first.getCode());
            assertEquals("Silicon Valley", first.getName());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros")));
        }
    }

    @Nested
    @DisplayName("list(MetroPresence)")
    class ListMetrosByPresence {

        @Test
        @DisplayName("sends the presence query param and returns metros")
        void sendsPresenceQueryParam() {
            stubPaginatedGet(wireMock, "/fabric/v4/metros", "/json/fabric/paginated_metros_list.json");

            PaginatedList<Metro> metros = fabric.metros().list(MetroPresence.MY_PORTS);

            assertNotNull(metros);
            assertEquals(2, metros.size());
            assertEquals(MetroCode.SV, metros.get(0).getCode());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros"))
                    .withQueryParam("presence", equalTo("MY_PORTS")));
        }
    }

    @Nested
    @DisplayName("getByMetroCode()")
    class GetByMetroCode {

        @Test
        @DisplayName("returns a single metro for a metro code")
        void returnsMetro() {
            stubSingleton(wireMock, "/fabric/v4/metros/.*", "/json/fabric/metro_single_response.json");

            Metro metro = fabric.metros().getByMetroCode(MetroCode.SV);

            assertNotNull(metro);
            assertEquals(MetroCode.SV, metro.getCode());
            assertEquals("Silicon Valley", metro.getName());

            // Full spec-fidelity of the Metro schema (fabricv4): every property deserializes.
            assertEquals("US", metro.getCountry());
            assertEquals(60000L, metro.getEquinixAsn());
            assertEquals(10000L, metro.getLocalVCBandwidthMax());
            assertEquals(2, metro.getServices().size());
            assertEquals("ETHERNET_IP_SERVICE", metro.getServices().get(0).getType());
            assertEquals(8, metro.getGeoScopes().size());
            assertTrue(metro.getGeoScopes().contains(com.eqixiac.equinix.fabric.enums.GeoScopeType.CONUS));
            assertEquals(1, metro.getGeoZones().size());
            assertEquals("EU", metro.getGeoZones().get(0).getCode());
            assertEquals("European Union", metro.getGeoZones().get(0).getName());
            assertEquals(10000L, metro.getConnectedMetros().get(0).getRemoteVCBandwidthMax());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros/SV")));
        }
    }

    @Nested
    @DisplayName("getByMetroId(MetroId)")
    class GetByMetroId {

        @Test
        @DisplayName("targets GET /metros/{code} using the MetroId's code")
        void returnsMetroById() {
            stubSingleton(wireMock, "/fabric/v4/metros/.*", "/json/fabric/metro_single_response.json");

            Metro metro = fabric.metros().getByMetroId(MetroId.of("SV"));

            assertNotNull(metro);
            assertEquals(MetroCode.SV, metro.getCode());
            assertEquals(MetroId.of("SV"), metro.metroId());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros/SV")));
        }
    }

    @Nested
    @DisplayName("Forward compatibility (metros not in the MetroCode enum)")
    class ForwardCompat {

        @Test
        @DisplayName("a metro absent from the enum keeps its real code via metroId() while getCode() is UNKNOWN")
        void unlistedMetro_preservesRawCode() {
            stubPaginatedGet(wireMock, "/fabric/v4/metros", "/json/fabric/paginated_metros_forward_compat.json");

            PaginatedList<Metro> metros = fabric.metros().list();
            Metro unlisted = metros.get(1);

            assertEquals(MetroCode.UNKNOWN, unlisted.getCode(), "not in the enum -> UNKNOWN");
            assertEquals("ZZ", unlisted.metroId().code(), "but the real code is preserved");
            assertFalse(unlisted.metroId().isKnown());
            assertEquals(List.of("ZZ1", "ZZ2"), unlisted.getIbxs());
        }

        @Test
        @DisplayName("getByMetroCode(String) targets a metro by raw code")
        void getByRawStringCode() {
            stubSingleton(wireMock, "/fabric/v4/metros/.*", "/json/fabric/metro_single_response.json");

            Metro metro = fabric.metros().getByMetroCode("SV");

            assertEquals(MetroId.of("SV"), metro.metroId());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/metros/SV")));
        }
    }

    @Nested
    @DisplayName("MetroRegistry")
    class Registry {

        @Test
        @DisplayName("loads all metros keyed by MetroId, including ones absent from the enum")
        void loadsAllMetros() throws Exception {
            stubPaginatedGet(wireMock, "/fabric/v4/metros", "/json/fabric/paginated_metros_forward_compat.json");

            try (Fabric fresh = new Fabric(testCredentials())) {
                redirectToWireMock(fresh);
                MetroRegistry registry = fresh.metroRegistry();

                assertEquals(2, registry.size());
                assertTrue(registry.contains("SV"));
                assertTrue(registry.contains("ZZ"));          // unlisted metro is still present
                assertTrue(registry.contains(MetroCode.SV));
                assertEquals("New Metro Not In Enum", registry.get("ZZ").orElseThrow().getName());
                assertEquals(List.of("ZZ1", "ZZ2"), registry.ibxs("zz"));   // case-insensitive
                assertTrue(registry.metroIds().contains(MetroId.of("ZZ")));
            }
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/metros.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.metros().getByMetroCode(MetroCode.SV));
        }
    }

    @Nested
    @DisplayName("MetroWrapper refresh()")
    class WrapperRefresh {

        @Test
        @DisplayName("re-GETs /metros/{code} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            String url = "/fabric/v4/metros/SV";
            wireMock.stubFor(get(urlPathEqualTo(url))
                    .inScenario("metro-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/metro_single_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(url))
                    .inScenario("metro-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/metro_single_response.json")
                            .replace("\"name\": \"Silicon Valley\"", "\"name\": \"Silicon Valley Renamed\""))));

            Metro metro = fabric.metros().getByMetroCode(MetroCode.SV);
            assertEquals("Silicon Valley", metro.getName());

            // The wrapper's own refresh() — distinct from MetroRegistry.refresh(), which reloads
            // the whole registry rather than a single wrapper in place.
            Metro refreshed = metro.refresh();

            assertSame(metro, refreshed, "refresh() returns the same live wrapper");
            assertEquals("Silicon Valley Renamed", metro.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(url)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "code": "SV", "name": "Page-1-Metro" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "code": "DC", "name": "Page-2-Metro" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-GETs /metros with the offset query param advanced to page 2")
        void loadAllFetchesSecondPage() {
            // Page 1: catch-all, registered first (WireMock: the later, more specific stub wins).
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/metros"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/metros"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Metro> metros = fabric.metros().list();
            assertEquals(1, metros.size());
            assertTrue(metros.hasNextPage());

            metros.loadAll();

            assertEquals(2, metros.size());
            assertEquals("Page-1-Metro", metros.get(0).getName());
            assertEquals("Page-2-Metro", metros.get(1).getName());
            assertEquals(MetroCode.DC, metros.get(1).getCode());
            assertFalse(metros.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/metros"))
                    .withQueryParam("offset", equalTo("100")));
        }
    }
}
