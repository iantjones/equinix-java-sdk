package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Route Aggregations.
 */
class FabricRouteAggregationsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns route aggregation for valid UUID")
        void returnsRouteAggregation() {
            stubSingleton(wireMock, "/fabric/v4/routeAggregations/.*",
                    "/json/fabric/route_aggregation_response.json");

            RouteAggregation ra = fabric.routeAggregations().getByUuid("b1c2d3e4-f5a6-7890-bcde-f01234567890");
            assertNotNull(ra);
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", ra.getUuid());
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/routeAggregations/.*",
                    "/json/fabric/route_aggregation_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routeAggregations/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_response.json"))));

            RouteAggregation ra = fabric.routeAggregations().getByUuid("b1c2d3e4-f5a6-7890-bcde-f01234567890");
            RouteAggregation updated = ra.update().name("Renamed-Aggregation").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/routeAggregations/b1c2d3e4-f5a6-7890-bcde-f01234567890"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Aggregation\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/routeAggregations/.*",
                    "/json/fabric/route_aggregation_response.json");

            RouteAggregation ra = fabric.routeAggregations().getByUuid("b1c2d3e4-f5a6-7890-bcde-f01234567890");
            assertThrows(IllegalStateException.class, () -> ra.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/routeAggregations/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/routeAggregations/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.routeAggregations().getByUuid("test-uuid"));
        }
    }
}
