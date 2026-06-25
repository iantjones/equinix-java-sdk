package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.RouteFilter;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Route Filters.
 */
class FabricRouteFiltersWireMockTest extends WireMockTestBase {

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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns route filter for valid UUID")
        void returnsRouteFilter() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*",
                    "/json/fabric/route_filter_response.json");

            RouteFilter filter = fabric.routeFilters().getByUuid("e5f6a7b8-c9d0-1234-efab-456789012cde");
            assertNotNull(filter);
            assertEquals("e5f6a7b8-c9d0-1234-efab-456789012cde", filter.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/routeFilters/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Route filter not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.routeFilters().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/routeFilters/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.routeFilters().getByUuid("test-uuid"));
        }
    }
}
