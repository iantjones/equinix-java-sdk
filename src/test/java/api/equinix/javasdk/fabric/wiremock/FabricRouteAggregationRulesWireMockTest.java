package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.IPAddress;
import api.equinix.javasdk.fabric.enums.RouteAggregationRuleState;
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
    @DisplayName("list()")
    class ListRules {

        static final String LIST_PATH =
                "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules";
        static final String LIST_PATH_PATTERN =
                "/fabric/v4/routeAggregations/[^/]+/routeAggregationRules";

        @Test
        @DisplayName("GETs {parent}/routeAggregationRules and deserializes the rules")
        void listsRules() {
            stubPaginatedGet(wireMock, LIST_PATH_PATTERN,
                    "/json/fabric/paginated_route_aggregation_rules.json");

            PaginatedList<RouteAggregationRule> rules =
                    fabric.routeAggregationRules().list(PARENT);

            assertNotNull(rules);
            assertEquals(2, rules.size());
            assertEquals("c2d3e4f5-a6b7-8901-cdef-012345678901", rules.get(0).getUuid());
            assertEquals("Aggregate-10-0-0-0-8", rules.get(0).getName());

            // Spec state values round-trip, including the NOT_PROVISIONED addition.
            assertEquals(RouteAggregationRuleState.PROVISIONED, rules.get(0).getState());
            assertEquals(RouteAggregationRuleState.NOT_PROVISIONED, rules.get(1).getState());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH)));
        }

        @Test
        @DisplayName("uses the parent id in the request path (no child segment)")
        void listTargetsParentScopedCollection() {
            stubPaginatedGet(wireMock, LIST_PATH_PATTERN,
                    "/json/fabric/paginated_route_aggregation_rules.json");

            fabric.routeAggregationRules().list(PARENT);

            // GET on the collection, not on an individual rule, and not a POST /search.
            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH)));
            wireMock.verify(0, getRequestedFor(urlPathMatching(LIST_PATH + "/[^/]+")));
            wireMock.verify(0, postRequestedFor(urlPathMatching(LIST_PATH_PATTERN + "/search")));
        }
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

        @Test
        @DisplayName("prefix(IPAddress) PATCHes a byte-identical op/path/value body to prefix(String)")
        void typedPrefixMatchesStringPathOnUpdate() {
            stubSingleton(wireMock, RULE_PATH_PATTERN,
                    "/json/fabric/route_aggregation_rule_response.json");
            wireMock.stubFor(patch(urlPathMatching(RULE_PATH_PATTERN))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            RouteAggregationRule rule = fabric.routeAggregationRules().getByUuid(PARENT, RULE);

            // Same update issued twice: once via the String setter, once via the typed
            // IPAddress overload (which formats via IPAddress.toCidr(), preserving the CIDR subnet).
            rule.update(PARENT).prefix("192.168.0.0/16").save();
            rule.update(PARENT).prefix(IPAddress.parse("192.168.0.0/16")).save();

            var patches = wireMock.findAll(patchRequestedFor(urlPathMatching(RULE_PATH)));
            assertEquals(2, patches.size());
            assertEquals(patches.get(0).getBodyAsString(), patches.get(1).getBodyAsString());
            wireMock.verify(2, patchRequestedFor(urlPathMatching(RULE_PATH))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/prefix\",\"value\":\"192.168.0.0/16\"}]")));
        }
    }

    @Nested
    @DisplayName("define().create()")
    class Create {

        @Test
        @DisplayName("POSTs the rule body to /routeAggregationRules and returns the created rule")
        void createsRule() {
            wireMock.stubFor(post(urlPathMatching(
                    "/fabric/v4/routeAggregations/.*/routeAggregationRules"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            RouteAggregationRule created = fabric.routeAggregationRules().define(PARENT)
                    .withName("Aggregate-10-0-0-0-8")
                    .withPrefix("10.0.0.0/8")
                    .withDescription("Aggregate the 10/8 private range")
                    .create();

            assertNotNull(created);
            assertEquals(RULE, created.getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules"))
                    .withRequestBody(equalToJson(
                            "{\"name\":\"Aggregate-10-0-0-0-8\","
                            + "\"prefix\":\"10.0.0.0/8\","
                            + "\"description\":\"Aggregate the 10/8 private range\"}",
                            true, true)));
        }

        @Test
        @DisplayName("withPrefix(IPAddress) POSTs a byte-identical create body to the String setter")
        void typedPrefixMatchesStringPath() {
            wireMock.stubFor(post(urlPathMatching(
                    "/fabric/v4/routeAggregations/.*/routeAggregationRules"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            // Same create issued twice: once via the String setter, once via the typed
            // IPAddress overload (which formats via IPAddress.toCidr(), preserving the CIDR subnet).
            fabric.routeAggregationRules().define(PARENT)
                    .withName("Aggregate-10-0-0-0-8")
                    .withPrefix("10.0.0.0/8")
                    .withDescription("Aggregate the 10/8 private range")
                    .create();
            fabric.routeAggregationRules().define(PARENT)
                    .withName("Aggregate-10-0-0-0-8")
                    .withPrefix(IPAddress.parse("10.0.0.0/8"))
                    .withDescription("Aggregate the 10/8 private range")
                    .create();

            var posts = wireMock.findAll(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules")));
            assertEquals(2, posts.size());
            assertEquals(posts.get(0).getBodyAsString(), posts.get(1).getBodyAsString());
            wireMock.verify(2, postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules"))
                    .withRequestBody(matchingJsonPath("$.prefix", equalTo("10.0.0.0/8"))));
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

    @Nested
    @DisplayName("Wrapper delete(routeAggregationId)")
    class WrapperDelete {

        private static final String URL =
                "/fabric/v4/routeAggregations/" + PARENT + "/routeAggregationRules/" + RULE;

        @Test
        @DisplayName("DELETEs /routeAggregations/{raId}/routeAggregationRules/{uuid} and returns true")
        void deletesRule() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/route_aggregation_rule_response.json"))));

            RouteAggregationRule rule = fabric.routeAggregationRules().getByUuid(PARENT, RULE);
            Boolean deleted = rule.delete(PARENT);

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }
}
