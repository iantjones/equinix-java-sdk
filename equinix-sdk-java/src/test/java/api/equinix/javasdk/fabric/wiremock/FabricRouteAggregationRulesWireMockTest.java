package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Route Aggregation Rules (parent-keyed).
 */
class FabricRouteAggregationRulesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final String PARENT = "b1c2d3e4-f5a6-7890-bcde-f01234567890";
    static final String RULE = "c2d3e4f5-a6b7-8901-cdef-012345678901";
    static final String RULE_PATH =
            "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/" + RULE;
    static final String RULE_PATH_PATTERN =
            "/fabric/v4/routeAggregations/.*/routeAggregationRules/.*";

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
        @DisplayName("returns route aggregation rule for valid UUID")
        void returnsRule() {
            stubSingleton(wireMock, RULE_PATH_PATTERN,
                    "/json/fabric/route_aggregation_rule_response.json");

            RouteAggregationRule rule = fabric.routeAggregationRules().getByUuid(PARENT, RULE);
            assertNotNull(rule);
            assertEquals(RULE, rule.getUuid());
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesName() {
            stubSingleton(wireMock, RULE_PATH_PATTERN,
                    "/json/fabric/route_aggregation_rule_response.json");
            wireMock.stubFor(patch(urlPathMatching(RULE_PATH_PATTERN))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            RouteAggregationRule rule = fabric.routeAggregationRules().getByUuid(PARENT, RULE);
            RouteAggregationRule updated = rule.update(PARENT).name("Renamed-Rule").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching(RULE_PATH))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Rule\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, RULE_PATH_PATTERN,
                    "/json/fabric/route_aggregation_rule_response.json");

            RouteAggregationRule rule = fabric.routeAggregationRules().getByUuid(PARENT, RULE);
            assertThrows(IllegalStateException.class, () -> rule.update(PARENT).save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching(RULE_PATH_PATTERN)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, RULE_PATH_PATTERN,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.routeAggregationRules().getByUuid(PARENT, RULE));
        }
    }
}
