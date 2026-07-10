package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.model.CloudEvent;
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
 * WireMock-based API tests for Fabric Cloud Events.
 */
class FabricCloudEventsWireMockTest extends WireMockTestBase {

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
    @DisplayName("getByAssetId()")
    class GetByAssetId {

        @Test
        @DisplayName("GETs {asset}/{assetId}/cloudevents and returns the list of cloud events")
        void returnsCloudEvents() {
            stubPaginatedGet(wireMock, "/fabric/v4/connections/.*/cloudevents",
                    "/json/fabric/cloud_events_by_asset_response.json");

            List<CloudEvent> events = fabric.cloudEvents().getByAssetId(
                    "connections", "095be615-a8ad-4c33-8e9c-c7612fbf6c9f");

            assertNotNull(events);
            assertEquals(2, events.size());
            assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", events.get(0).getUuid());
            assertEquals("equinix.fabric.connection.updated", events.get(0).getType());

            wireMock.verify(getRequestedFor(urlPathMatching(
                    "/fabric/v4/connections/095be615-a8ad-4c33-8e9c-c7612fbf6c9f/cloudevents")));
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("search() POSTs an empty filter/sort to /cloudevents/search")
        void searchNoArgs() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            PaginatedFilteredList<CloudEvent> results = fabric.cloudEvents().search();

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", results.get(0).getUuid());
            assertEquals("equinix.fabric.connection.updated", results.get(0).getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search")));
        }

        @Test
        @DisplayName("search(filter) POSTs the AND/equals filter in the body")
        void searchWithFilter() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "equinix.fabric.connection.updated");

            PaginatedFilteredList<CloudEvent> results = fabric.cloudEvents().search(filter);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]",
                            equalTo("equinix.fabric.connection.updated"))));
        }

        @Test
        @DisplayName("search(sort) POSTs the sort array in the body")
        void searchWithSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            SortPropertyList sort = Sort.sort().desc("/time");

            PaginatedFilteredList<CloudEvent> results = fabric.cloudEvents().search(sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/time")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC"))));
        }

        @Test
        @DisplayName("search(filter, sort) POSTs both filter and sort in the body")
        void searchWithFilterAndSort() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            FilterPropertyList filter = Filter.filter().and()
                    .equals("/type", "equinix.fabric.connection.created");
            SortPropertyList sort = Sort.sort().asc("/time");

            PaginatedFilteredList<CloudEvent> results = fabric.cloudEvents().search(filter, sort);

            assertNotNull(results);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]",
                            equalTo("equinix.fabric.connection.created")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/time")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("ASC"))));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /cloudevents/{uuid} and returns the single cloud event")
        void returnsCloudEvent() {
            stubSingleton(wireMock, "/fabric/v4/cloudevents/557400f8-d360-11e9-bb65-2a2ae2dbcce4",
                    "/json/fabric/cloud_event_response.json");

            CloudEvent event = fabric.cloudEvents().getByUuid("557400f8-d360-11e9-bb65-2a2ae2dbcce4");

            assertNotNull(event);
            assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", event.getUuid());
            assertEquals("equinix.fabric.connection.updated", event.getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/cloudevents/557400f8-d360-11e9-bb65-2a2ae2dbcce4")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/connections/.*/cloudevents",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.cloudEvents().getByAssetId("connections", "test-uuid"));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        // CloudEvents identify themselves by CNCF "id" on the wire (mapped onto getUuid()).
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "id": "PAGE1_EVENT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "id": "PAGE2_EVENT" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-POSTs the search with the body's pagination offset advanced to page 2")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            PaginatedFilteredList<CloudEvent> events = fabric.cloudEvents().search();
            assertEquals(1, events.size());
            assertTrue(events.hasNextPage());

            events.loadAll();

            assertEquals(2, events.size());
            assertEquals("PAGE1_EVENT", events.get(0).getUuid());
            assertEquals("PAGE2_EVENT", events.get(1).getUuid());
            assertFalse(events.hasNextPage());

            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("100"))));
        }
    }
}
