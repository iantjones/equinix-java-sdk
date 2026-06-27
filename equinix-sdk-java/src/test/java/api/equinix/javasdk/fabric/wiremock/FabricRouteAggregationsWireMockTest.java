package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.RouteAggregationType;
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
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("POSTs to /routeAggregations/search and returns a filtered list")
        void searchReturnsResults() {
            // Fabric exposes route aggregations via POST /search, not a GET list.
            stubPaginatedPost(wireMock, "/fabric/v4/routeAggregations/search",
                    "/json/fabric/paginated_route_aggregations.json");

            PaginatedFilteredList<RouteAggregation> aggregations = fabric.routeAggregations().search();

            assertNotNull(aggregations);
            assertEquals(1, aggregations.size());
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", aggregations.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/routeAggregations/search")));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the configured route aggregation and returns the created object")
        void createsRouteAggregation() {
            // POST returns the created object body directly.
            stubCreate(wireMock, "/fabric/v4/routeAggregations",
                    "/json/fabric/route_aggregation_response.json");

            RouteAggregation created = fabric.routeAggregations().define()
                    .ofType(RouteAggregationType.BGP_IPv4_PREFIX_AGGREGATION)
                    .withName("Production-Aggregation")
                    .withDescription("Primary route aggregation")
                    .withProjectId("d7b0a4b8-1c2d-4e5f-a6b7-c8d9e0f12345")
                    .create();

            assertNotNull(created);
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", created.getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/routeAggregations"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("BGP_IPv4_PREFIX_AGGREGATION")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-Aggregation")))
                    .withRequestBody(matchingJsonPath("$.project.projectId",
                            equalTo("d7b0a4b8-1c2d-4e5f-a6b7-c8d9e0f12345"))));
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
