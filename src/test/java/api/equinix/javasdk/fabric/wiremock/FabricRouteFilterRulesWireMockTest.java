package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Route Filter Rules (parent-keyed by routeFilterId).
 */
class FabricRouteFilterRulesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    static final String ROUTE_FILTER_ID = "e5f6a7b8-c9d0-1234-efab-456789012cde";
    static final String RULE_UUID = "b1c2d3e4-f5a6-7890-bcde-f01234567890";

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
        @DisplayName("returns route filter rule for valid UUID")
        void returnsRule() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*",
                    "/json/fabric/route_filter_rule_response.json");

            RouteFilterRule rule = fabric.routeFilterRules().getByUuid(ROUTE_FILTER_ID, RULE_UUID);
            assertNotNull(rule);
            assertEquals(RULE_UUID, rule.getUuid());
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*",
                    "/json/fabric/route_filter_rule_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routeFilters/.*/routeFilterRules/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/route_filter_rule_response.json"))));

            RouteFilterRule rule = fabric.routeFilterRules().getByUuid(ROUTE_FILTER_ID, RULE_UUID);
            RouteFilterRule updated = rule.update(ROUTE_FILTER_ID).name("Renamed-Rule").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/" + RULE_UUID))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Rule\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*",
                    "/json/fabric/route_filter_rule_response.json");

            RouteFilterRule rule = fabric.routeFilterRules().getByUuid(ROUTE_FILTER_ID, RULE_UUID);
            assertThrows(IllegalStateException.class, () -> rule.update(ROUTE_FILTER_ID).save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/.*/routeFilterRules/.*")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.routeFilterRules().getByUuid(ROUTE_FILTER_ID, RULE_UUID));
        }
    }
}
