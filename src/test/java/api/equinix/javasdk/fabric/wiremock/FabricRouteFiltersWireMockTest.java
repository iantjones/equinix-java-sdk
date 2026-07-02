package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.RouteFilterAction;
import api.equinix.javasdk.fabric.enums.RouteFilterType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.RouteFilter;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.Sort;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.util.List;

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

            // The response-example wire name "changeLog" still maps.
            assertNotNull(filter.getChangeLog());
            assertEquals("user1234", filter.getChangeLog().getCreatedBy());
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
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("search() POSTs to /routeFilters/search and returns a filtered list")
        void searchReturnsResults() {
            // Fabric exposes route filters via POST /search, not a GET list.
            stubPaginatedPost(wireMock, "/fabric/v4/routeFilters/search",
                    "/json/fabric/paginated_route_filters.json");

            PaginatedFilteredList<RouteFilter> filters = fabric.routeFilters().search();

            assertNotNull(filters);
            assertEquals(2, filters.size());
            assertEquals("e5f6a7b8-c9d0-1234-efab-456789012cde", filters.get(0).getUuid());

            // The RouteFiltersData schema names the property "changelog" (lowercase);
            // the @JsonAlias must map it onto changeLog.
            assertNotNull(filters.get(0).getChangeLog());
            assertEquals("user1234", filters.get(0).getChangeLog().getCreatedBy());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routeFilters/search")));
        }

        @Test
        @DisplayName("search(filter) POSTs the AND/equals filter in the body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, "/fabric/v4/routeFilters/search",
                    "/json/fabric/paginated_route_filters.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "BGP_IPv4_PREFIX_FILTER");

            PaginatedFilteredList<RouteFilter> results = fabric.routeFilters().search(filter);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routeFilters/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("BGP_IPv4_PREFIX_FILTER"))));
        }

        @Test
        @DisplayName("search(sort) POSTs the sort array in the body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/routeFilters/search",
                    "/json/fabric/paginated_route_filters.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<RouteFilter> results = fabric.routeFilters().search(sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routeFilters/search"))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) POSTs both filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/routeFilters/search",
                    "/json/fabric/paginated_route_filters.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/state", "PROVISIONED");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<RouteFilter> results = fabric.routeFilters().search(filter, sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/routeFilters/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/state")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("PROVISIONED")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the configured route filter and returns the created object")
        void createsRouteFilter() {
            // POST returns the created object body directly.
            stubCreate(wireMock, "/fabric/v4/routeFilters",
                    "/json/fabric/route_filter_response.json");

            RouteFilter created = fabric.routeFilters().define()
                    .ofType(RouteFilterType.BGP_IPv4_PREFIX_FILTER)
                    .name("Production-IPv4-Prefix-Filter")
                    .description("IPv4 prefix filter for production BGP peering sessions")
                    .notMatchedRuleAction(RouteFilterAction.DENY)
                    .create();

            assertNotNull(created);
            assertEquals("e5f6a7b8-c9d0-1234-efab-456789012cde", created.getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/fabric/v4/routeFilters"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("BGP_IPv4_PREFIX_FILTER")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-IPv4-Prefix-Filter")))
                    .withRequestBody(matchingJsonPath("$.notMatchedRuleAction", equalTo("DENY"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PATCHes an op/path/value array as application/json")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*",
                    "/json/fabric/route_filter_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/routeFilters/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/route_filter_response.json"))));

            RouteFilter filter = fabric.routeFilters().getByUuid("e5f6a7b8-c9d0-1234-efab-456789012cde");
            RouteFilter updated = filter.update().name("Renamed-Filter").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/routeFilters/e5f6a7b8-c9d0-1234-efab-456789012cde"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-Filter\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*",
                    "/json/fabric/route_filter_response.json");

            RouteFilter filter = fabric.routeFilters().getByUuid("e5f6a7b8-c9d0-1234-efab-456789012cde");
            assertThrows(IllegalStateException.class, () -> filter.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/routeFilters/.*")));
        }
    }

    @Nested
    @DisplayName("getChanges()")
    class GetChanges {

        @Test
        @DisplayName("GETs {uuid}/changes and returns the list of changes")
        void returnsChanges() {
            stubPaginatedGet(wireMock, "/fabric/v4/routeFilters/.*/changes",
                    "/json/fabric/route_filter_changes_response.json");

            List<Change> changes = fabric.routeFilters().getChanges("e5f6a7b8-c9d0-1234-efab-456789012cde");

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", changes.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/e5f6a7b8-c9d0-1234-efab-456789012cde/changes")));
        }
    }

    @Nested
    @DisplayName("getChange()")
    class GetChange {

        @Test
        @DisplayName("GETs {uuid}/changes/{changeId} and returns the single change")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/routeFilters/.*/changes/.*",
                    "/json/fabric/route_filter_change_response.json");

            Change change = fabric.routeFilters().getChange(
                    "e5f6a7b8-c9d0-1234-efab-456789012cde", "a9b8c7d6-e5f4-3210-abcd-fedcba987654");

            assertNotNull(change);
            assertEquals("a9b8c7d6-e5f4-3210-abcd-fedcba987654", change.getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/e5f6a7b8-c9d0-1234-efab-456789012cde/changes/a9b8c7d6-e5f4-3210-abcd-fedcba987654")));
        }
    }

    @Nested
    @DisplayName("getConnections()")
    class GetConnections {

        @Test
        @DisplayName("GETs {uuid}/connections and returns the attached connections")
        void returnsConnections() {
            stubPaginatedGet(wireMock, "/fabric/v4/routeFilters/.*/connections",
                    "/json/fabric/paginated_connections.json");

            List<Connection> connections = fabric.routeFilters().getConnections("e5f6a7b8-c9d0-1234-efab-456789012cde");

            assertNotNull(connections);
            assertEquals(2, connections.size());
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connections.get(0).getUuid());
            assertEquals("Connection-One", connections.get(0).getName());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/routeFilters/e5f6a7b8-c9d0-1234-efab-456789012cde/connections")));
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
