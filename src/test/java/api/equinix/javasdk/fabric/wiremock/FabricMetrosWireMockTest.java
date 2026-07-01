package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.MetroPresence;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
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
}
