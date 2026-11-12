package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.CloudEvent;
import com.eqixiac.equinix.fabric.model.implementation.filter.CloudEventQuery;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.Sort;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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

    private static final String PORT_SUBJECT = "/fabric/v4/ports/8ac8ecfe-c7cb-4def-9f77-5ce93c19d83d";

    @Nested
    @DisplayName("search(CloudEventQuery)")
    class Search {

        @Test
        @DisplayName("forAsset sends the subject IN expression the service requires")
        void searchForAsset() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            PaginatedFilteredList<CloudEvent> results = fabric.cloudEvents().search(
                    CloudEventQuery.forAsset("connections", "095be615-a8ad-4c33-8e9c-c7612fbf6c9f"));

            assertEquals(2, results.size());
            assertEquals("557400f8-d360-11e9-bb65-2a2ae2dbcce4", results.get(0).getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/subject")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("IN")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]",
                            equalTo("/fabric/v4/connections/095be615-a8ad-4c33-8e9c-c7612fbf6c9f"))));
        }

        @Test
        @DisplayName("forProject with types and a time bound matches the spec example's property set")
        void searchForProject() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");

            fabric.cloudEvents().search(CloudEventQuery.forProject("dadd3ab6-c0af-430c-8216-43d44f08c1c5")
                    .types("equinix.fabric.port.status.*")
                    .after(LocalDateTime.of(2018, 4, 5, 17, 31)), Sort.sort().desc("/time"));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/equinixproject")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("=")))
                    .withRequestBody(matchingJsonPath("$.filter.and[1].property", equalTo("/type")))
                    .withRequestBody(matchingJsonPath("$.filter.and[2].property", equalTo("/time")))
                    .withRequestBody(matchingJsonPath("$.filter.and[2].operator", equalTo(">")))
                    .withRequestBody(matchingJsonPath("$.filter.and[2].values[0]", equalTo("2018-04-05T17:31:00Z")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("/time"))));
        }

        @Test
        @DisplayName("a hand-built filter without subject/project/organization is rejected before any request")
        void rejectsUnscopedFilter() {
            FilterPropertyList filter = Filter.filter().and().equals("/type", "equinix.fabric.connection.updated");

            assertThrows(IllegalArgumentException.class, () -> fabric.cloudEvents().search(filter, null));
            assertThrows(IllegalArgumentException.class, () -> fabric.cloudEvents().search((FilterPropertyList) null, null));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/cloudevents/search")));
        }

        @Test
        @DisplayName("a hand-built filter with /equinixorganization is accepted")
        void acceptsOrganizationFilter() {
            stubPaginatedPost(wireMock, "/fabric/v4/cloudevents/search",
                    "/json/fabric/cloud_events_search_response.json");
            FilterPropertyList filter = Filter.filter().and().equals("/equinixorganization", "526698000597331");

            assertEquals(2, fabric.cloudEvents().search(filter, null).size());
        }
    }

    @Nested
    @DisplayName("searchLiveEvents()")
    class LiveEvents {

        @Test
        @DisplayName("POSTs /liveEvents/search with the subject IN filter and reads the spec example")
        void searchLiveEvents() {
            stubPaginatedPost(wireMock, "/fabric/v4/liveEvents/search",
                    "/json/fabric/live_events_search_response.json");

            PaginatedFilteredList<CloudEvent> events = fabric.cloudEvents().searchLiveEvents(PORT_SUBJECT);

            assertEquals(1, events.size());
            assertEquals("6c573287-9914-4202-8b6d-2d15e7da04b8", events.get(0).getUuid());
            assertEquals("equinix.fabric.port.status.down", events.get(0).getType());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/liveEvents/search"))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/subject")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("IN")))
                    .withRequestBody(matchingJsonPath("$.filter.and[0].values[0]", equalTo(PORT_SUBJECT))));
        }

        @Test
        @DisplayName("no subject or a wildcard subject is rejected (live: empty filter 500, wildcard 400)")
        void rejectsInvalidSubjects() {
            assertThrows(IllegalArgumentException.class, () -> fabric.cloudEvents().searchLiveEvents());
            assertThrows(IllegalArgumentException.class, () -> fabric.cloudEvents().searchLiveEvents(PORT_SUBJECT + "*"));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/liveEvents/search")));
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
            stubErrorInline(wireMock, "/fabric/v4/cloudevents/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.cloudEvents().search(CloudEventQuery.forAsset("connections", "test-uuid")));
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

            PaginatedFilteredList<CloudEvent> events = fabric.cloudEvents().search(CloudEventQuery.forProject("p-1"));
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
