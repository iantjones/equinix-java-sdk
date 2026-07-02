package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.RouteFilterAction;
import api.equinix.javasdk.fabric.enums.RouteFilterRuleState;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleCreatorJson;
import org.junit.jupiter.api.*;

import java.util.List;

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
    @DisplayName("list()")
    class ListRules {

        @Test
        @DisplayName("GETs /routeFilters/{id}/routeFilterRules and deserializes the rules")
        void listsRules() {
            stubPaginatedGet(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules",
                    "/json/fabric/paginated_route_filter_rules.json");

            PaginatedList<RouteFilterRule> rules = fabric.routeFilterRules().list(ROUTE_FILTER_ID);

            assertNotNull(rules);
            assertEquals(2, rules.size());
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", rules.get(0).getUuid());
            assertEquals("Allow-10-0-0-0-8", rules.get(0).getName());

            // Spec state values round-trip, including the NOT_PROVISIONED addition.
            assertEquals(RouteFilterRuleState.PROVISIONED, rules.get(0).getState());
            assertEquals(RouteFilterRuleState.NOT_PROVISIONED, rules.get(1).getState());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules")));
        }
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

            // The RouteFilterRulesData schema names the property "changelog" (lowercase);
            // the @JsonAlias must map it onto changeLog.
            assertNotNull(rule.getChangeLog());
            assertEquals("user1234", rule.getChangeLog().getCreatedBy());
        }
    }

    @Nested
    @DisplayName("define().create()")
    class Create {

        @Test
        @DisplayName("POSTs the rule body to /routeFilterRules and returns the created rule")
        void createsRule() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/routeFilters/.*/routeFilterRules"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/fabric/route_filter_rule_response.json"))));

            RouteFilterRule created = fabric.routeFilterRules().define(ROUTE_FILTER_ID)
                    .prefix("10.0.0.0/8")
                    .name("Allow-10-0-0-0-8")
                    .description("Permit the 10/8 private range")
                    .action(RouteFilterAction.PERMIT)
                    .prefixMatch("exact")
                    .create();

            assertNotNull(created);
            assertEquals(RULE_UUID, created.getUuid());
            assertEquals("Allow-10-0-0-0-8", created.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules"))
                    .withRequestBody(matchingJsonPath("$.prefix", equalTo("10.0.0.0/8")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Allow-10-0-0-0-8")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Permit the 10/8 private range")))
                    .withRequestBody(matchingJsonPath("$.action", equalTo("PERMIT")))
                    .withRequestBody(matchingJsonPath("$.prefixMatch", equalTo("exact"))));
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
    @DisplayName("replace()")
    class Replace {

        @Test
        @DisplayName("PUTs the rule body and returns the replaced rule")
        void replacesRule() {
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/routeFilters/.*/routeFilterRules/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/route_filter_rule_response.json"))));

            RouteFilterRuleCreatorJson body = new RouteFilterRuleCreatorJson(
                    "10.0.0.0/8", "Allow-10-0-0-0-8", "Permit the 10/8 private range",
                    RouteFilterAction.PERMIT, "exact");

            RouteFilterRule replaced = fabric.routeFilterRules().replace(ROUTE_FILTER_ID, RULE_UUID, body);

            assertNotNull(replaced);
            assertEquals(RULE_UUID, replaced.getUuid());

            wireMock.verify(putRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/" + RULE_UUID))
                    .withRequestBody(matchingJsonPath("$.prefix", equalTo("10.0.0.0/8")))
                    .withRequestBody(matchingJsonPath("$.action", equalTo("PERMIT")))
                    .withRequestBody(matchingJsonPath("$.prefixMatch", equalTo("exact"))));
        }
    }

    @Nested
    @DisplayName("createBulk()")
    class CreateBulk {

        @Test
        @DisplayName("POSTs {data:[...]} to /bulk and returns the created rules")
        void createsRulesInBulk() {
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/routeFilters/.*/routeFilterRules/bulk"))
                    .willReturn(okJson(loadFixture("/json/fabric/paginated_route_filter_rules.json"))));

            List<RouteFilterRuleCreatorJson> bodies = List.of(
                    new RouteFilterRuleCreatorJson("10.0.0.0/8", "Allow-10-0-0-0-8",
                            "Permit the 10/8 private range", RouteFilterAction.PERMIT, "exact"),
                    new RouteFilterRuleCreatorJson("192.168.0.0/16", "Allow-192-168-0-0-16",
                            "Permit the 192.168/16 private range", RouteFilterAction.PERMIT, "orlonger"));

            List<RouteFilterRule> created = fabric.routeFilterRules().createBulk(ROUTE_FILTER_ID, bodies);

            assertNotNull(created);
            assertEquals(2, created.size());
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", created.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/bulk"))
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
            wireMock.stubFor(post(urlPathMatching("/fabric/v4/routeFilters/.*/routeFilterRules/search"))
                    .willReturn(okJson(loadFixture("/json/fabric/paginated_route_filter_rules.json"))));

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/prefix", "10.0.0.0/8");

            PaginatedFilteredList<RouteFilterRule> rules = fabric.routeFilterRules().search(ROUTE_FILTER_ID, filter, null);

            assertNotNull(rules);
            assertEquals(2, rules.size());
            assertEquals("b1c2d3e4-f5a6-7890-bcde-f01234567890", rules.get(0).getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/search"))
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
            stubPaginatedGet(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*/changes",
                    "/json/fabric/route_filter_changes_response.json");

            List<Change> changes = fabric.routeFilterRules().getChanges(ROUTE_FILTER_ID, RULE_UUID);

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", changes.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/" + RULE_UUID + "/changes")));
        }
    }

    @Nested
    @DisplayName("getChange()")
    class GetChange {

        @Test
        @DisplayName("GETs {uuid}/changes/{changeId} and returns the single change")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*/routeFilterRules/.*/changes/.*",
                    "/json/fabric/route_filter_change_response.json");

            Change change = fabric.routeFilterRules().getChange(
                    ROUTE_FILTER_ID, RULE_UUID, "a9b8c7d6-e5f4-3210-abcd-fedcba987654");

            assertNotNull(change);
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", change.getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/" + ROUTE_FILTER_ID + "/routeFilterRules/" + RULE_UUID
                            + "/changes/a9b8c7d6-e5f4-3210-abcd-fedcba987654")));
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
