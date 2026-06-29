package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import org.junit.jupiter.api.*;

import java.util.List;

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
    @DisplayName("replace()")
    class Replace {

        @Test
        @DisplayName("PUTs the rule body and returns the replaced rule")
        void replacesRule() {
            wireMock.stubFor(put(urlPathMatching(RULE_PATH_PATTERN))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            RouteAggregationRuleCreatorJson body = new RouteAggregationRuleCreatorJson(
                    "Aggregate-10-0-0-0-8", "10.0.0.0/8", "Aggregate the 10/8 private range");

            RouteAggregationRule replaced = fabric.routeAggregationRules().replace(PARENT, RULE, body);

            assertNotNull(replaced);
            assertEquals(RULE, replaced.getUuid());

            wireMock.verify(putRequestedFor(urlPathMatching(RULE_PATH))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Aggregate-10-0-0-0-8")))
                    .withRequestBody(matchingJsonPath("$.prefix", equalTo("10.0.0.0/8"))));
        }
    }

    @Nested
    @DisplayName("createBulk()")
    class CreateBulk {

        @Test
        @DisplayName("POSTs {data:[...]} to /bulk and returns the created rules")
        void createsRulesInBulk() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/routeAggregations/.*/routeAggregationRules/bulk"))
                    .willReturn(okJson(loadFixture("/json/fabric/paginated_route_aggregation_rules.json"))));

            List<RouteAggregationRuleCreatorJson> bodies = List.of(
                    new RouteAggregationRuleCreatorJson("Aggregate-10-0-0-0-8", "10.0.0.0/8",
                            "Aggregate the 10/8 private range"),
                    new RouteAggregationRuleCreatorJson("Aggregate-192-168-0-0-16", "192.168.0.0/16",
                            "Aggregate the 192.168/16 private range"));

            List<RouteAggregationRule> created = fabric.routeAggregationRules().createBulk(PARENT, bodies);

            assertNotNull(created);
            assertEquals(2, created.size());
            assertEquals("c2d3e4f5-a6b7-8901-cdef-012345678901", created.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/bulk"))
                    .withRequestBody(matchingJsonPath("$.data[0].prefix", equalTo("10.0.0.0/8")))
                    .withRequestBody(matchingJsonPath("$.data[1].prefix", equalTo("192.168.0.0/16"))));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("POSTs {filter:{and:[...]}} to /search and deserializes the rules")
        void searchesRules() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/routeAggregations/.*/routeAggregationRules/search"))
                    .willReturn(okJson(loadFixture("/json/fabric/paginated_route_aggregation_rules.json"))));

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/prefix", "10.0.0.0/8");

            PaginatedFilteredList<RouteAggregationRule> rules = fabric.routeAggregationRules().search(PARENT, filter, null);

            assertNotNull(rules);
            assertEquals(2, rules.size());
            assertEquals("c2d3e4f5-a6b7-8901-cdef-012345678901", rules.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/prefix")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("10.0.0.0/8"))));
        }
    }

    @Nested
    @DisplayName("getChanges()")
    class GetChanges {

        @Test
        @DisplayName("GETs {uuid}/changes and returns the list of changes")
        void returnsChanges() {
            stubPaginatedGet(wireMock, "/fabric/v4/routeAggregations/.*/routeAggregationRules/.*/changes",
                    "/json/fabric/route_aggregation_changes_response.json");

            List<Change> changes = fabric.routeAggregationRules().getChanges(PARENT, RULE);

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", changes.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/" + RULE + "/changes")));
        }
    }

    @Nested
    @DisplayName("getChange()")
    class GetChange {

        @Test
        @DisplayName("GETs {uuid}/changes/{changeId} and returns the single change")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/routeAggregations/.*/routeAggregationRules/.*/changes/.*",
                    "/json/fabric/route_aggregation_change_response.json");

            Change change = fabric.routeAggregationRules().getChange(
                    PARENT, RULE, "a9b8c7d6-e5f4-3210-abcd-fedcba987654");

            assertNotNull(change);
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", change.getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/" + RULE
                            + "/changes/a9b8c7d6-e5f4-3210-abcd-fedcba987654")));
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
