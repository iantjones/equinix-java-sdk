package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.enums.NetworkEquinixStatus;
import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkState;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Network;
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
 * WireMock-based API tests for Fabric Networks.
 */
class FabricNetworksWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns network for valid UUID")
        void returnsNetwork() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertNotNull(network);
            assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", network.getUuid());
        }

        @Test
        @DisplayName("deserializes account, operation and links fields")
        void deserializesNewFields() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");

            assertNotNull(network.getAccount());
            assertEquals("Acme Corp", network.getAccount().getAccountName());
            assertEquals(NetworkState.ACTIVE, network.getState());
            assertNotNull(network.getLocation());
            assertEquals(api.equinix.javasdk.core.enums.MetroCode.SV, network.getLocation().getMetroCode());
            assertEquals("Silicon Valley", network.getLocation().getMetroName());
            assertEquals(Region.AMER, network.getLocation().getRegion());
            assertNotNull(network.getOperation());
            assertEquals(NetworkEquinixStatus.PROVISIONED, network.getOperation().getEquinixStatus());
            assertNotNull(network.getLinks());
            assertEquals(1, network.getLinks().size());
            assertEquals("getNetworkConnections", network.getLinks().get(0).getRel());
        }

        @Test
        @DisplayName("reads the spec NetworkState value DELETED (not null/UNKNOWN)")
        void readsDeletedState() {
            wireMock.stubFor(get(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(okJson("{"
                            + "\"uuid\":\"c3d4e5f6-a7b8-9012-cdef-234567890abc\","
                            + "\"name\":\"Decommissioned-Network\","
                            + "\"type\":\"EVPLAN\","
                            + "\"state\":\"DELETED\","
                            + "\"scope\":\"REGIONAL\""
                            + "}")));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertEquals(NetworkState.DELETED, network.getState());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Network not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.networks().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the network body to the collection and returns the created network")
        void createsNetwork() {
            stubCreate(wireMock, "/fabric/v4/networks", "/json/fabric/network_response.json");

            Network network = fabric.networks().define(NetworkType.EVPLAN)
                    .name("Production-EVPLAN-Network")
                    .scope(NetworkScope.REGIONAL)
                    .create();

            assertNotNull(network);
            assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", network.getUuid());
            assertEquals("Production-EVPLAN-Network", network.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/networks"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("EVPLAN")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-EVPLAN-Network")))
                    .withRequestBody(matchingJsonPath("$.scope", equalTo("REGIONAL"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("sends an RFC 6902 JSON Patch with json-patch content-type")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/network_response.json"))));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            Network updated = network.update().name("New-Name").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"New-Name\"}]")));
        }

        @Test
        @DisplayName("accumulates multiple field changes into one patch document")
        void saveMultipleFields() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/network_response.json"))));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            network.update()
                    .name("Renamed")
                    .patch(api.equinix.javasdk.core.http.request.PatchOperation.replace("/scope", "GLOBAL"))
                    .save();

            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/networks/.*"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed\"},"
                                    + "{\"op\":\"replace\",\"path\":\"/scope\",\"value\":\"GLOBAL\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertThrows(IllegalStateException.class, () -> network.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/networks/.*")));
        }

        @Test
        @DisplayName("404 on update throws EquinixNotFoundException")
        void updateNotFound() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*",
                    "/json/fabric/network_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/networks/.*"))
                    .willReturn(aResponse().withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Network not found\"}]")));

            Network network = fabric.networks().getByUuid("c3d4e5f6-a7b8-9012-cdef-234567890abc");
            assertThrows(EquinixNotFoundException.class, () -> network.update().name("x").save());
        }
    }

    @Nested
    @DisplayName("getChanges()")
    class GetChanges {

        @Test
        @DisplayName("GETs {uuid}/changes and returns the list of changes")
        void returnsChanges() {
            stubPaginatedGet(wireMock, "/fabric/v4/networks/.*/changes",
                    "/json/fabric/network_changes_response.json");

            List<Change> changes = fabric.networks().getChanges("c3d4e5f6-a7b8-9012-cdef-234567890abc");

            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals("4b17da68-3d6b-436d-9c8f-2105f3b950d9", changes.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc/changes")));
        }
    }

    @Nested
    @DisplayName("getChange()")
    class GetChange {

        @Test
        @DisplayName("GETs {uuid}/changes/{changeId} and returns the single change")
        void returnsChange() {
            stubSingleton(wireMock, "/fabric/v4/networks/.*/changes/.*",
                    "/json/fabric/network_change_response.json");

            Change change = fabric.networks().getChange(
                    "c3d4e5f6-a7b8-9012-cdef-234567890abc", "4b17da68-3d6b-436d-9c8f-2105f3b950d9");

            assertNotNull(change);
            assertEquals("4b17da68-3d6b-436d-9c8f-2105f3b950d9", change.getUuid());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc/changes/4b17da68-3d6b-436d-9c8f-2105f3b950d9")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/networks/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.networks().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        private static final String SEARCH_URL = "/fabric/v4/networks/search";

        @Test
        @DisplayName("no-arg search POSTs the default body to /networks/search and returns a filtered list")
        void searchNoArg() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_networks.json");

            PaginatedFilteredList<Network> networks = fabric.networks().search();

            assertNotNull(networks);
            assertEquals(2, networks.size());
            assertEquals("c3d4e5f6-a7b8-9012-cdef-234567890abc", networks.get(0).getUuid());
            assertEquals(NetworkState.ACTIVE, networks.get(0).getState());
            assertEquals(NetworkState.INACTIVE, networks.get(1).getState());

            // Default no-arg search sends an (empty) filter, no sort.
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.pagination")));
        }

        @Test
        @DisplayName("search(filter) carries the filter predicate in the POST body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_networks.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/name", "Production-EVPLAN-Network")
                    .equals("/scope", "REGIONAL");

            PaginatedFilteredList<Network> networks = fabric.networks().search(filter);

            assertNotNull(networks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("Production-EVPLAN-Network")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/scope")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].values[0]", equalTo("REGIONAL"))));
        }

        @Test
        @DisplayName("search(sort) carries the sort directive in the POST body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_networks.json");

            SortPropertyList sort = Sort.sort().desc("/changeLog/createdDateTime");

            PaginatedFilteredList<Network> networks = fabric.networks().search(sort);

            assertNotNull(networks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/changeLog/createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) carries both filter and sort in the POST body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, SEARCH_URL, "/json/fabric/paginated_networks.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "EVPLAN");
            SortPropertyList sort = Sort.sort().asc("/name");

            PaginatedFilteredList<Network> networks = fabric.networks().search(filter, sort);

            assertNotNull(networks);
            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_URL))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo("EVPLAN")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/name")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getConnections()")
    class GetConnections {

        @Test
        @DisplayName("GETs {networkId}/connections and returns the paginated connection list")
        void returnsConnections() {
            stubPaginatedGet(wireMock, "/fabric/v4/networks/.*/connections",
                    "/json/fabric/paginated_connections.json");

            PaginatedList<Connection> connections = fabric.networks()
                    .getConnections("c3d4e5f6-a7b8-9012-cdef-234567890abc");

            assertNotNull(connections);
            assertEquals(2, connections.size());
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connections.get(0).getUuid());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/networks/c3d4e5f6-a7b8-9012-cdef-234567890abc/connections")));
        }
    }
}
