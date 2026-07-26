package com.eqixiac.equinix.fabric.wiremock;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.fabric.enums.StreamType;
import com.eqixiac.equinix.fabric.model.Stream;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Streams.
 */
class FabricStreamsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns stream for valid UUID")
        void returnsStream() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*",
                    "/json/fabric/stream_response.json");

            Stream stream = fabric.streams().getByUuid("d4e5f6a7-b8c9-0123-defa-345678901bcd");
            assertNotNull(stream);
            assertEquals("d4e5f6a7-b8c9-0123-defa-345678901bcd", stream.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Stream not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.streams().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListStreams {

        @Test
        @DisplayName("GETs the streams collection and returns the page")
        void returnsStreams() {
            stubPaginatedGet(wireMock, "/fabric/v4/streams", "/json/fabric/paginated_streams.json");

            var streams = fabric.streams().list();
            assertEquals(1, streams.size());
            assertEquals("d4e5f6a7-b8c9-0123-defa-345678901bcd", streams.get(0).getUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/streams")));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs the stream body to the collection and returns the created stream")
        void createsStream() {
            stubCreate(wireMock, "/fabric/v4/streams", "/json/fabric/stream_response.json");

            Stream stream = fabric.streams().define()
                    .withType(StreamType.TELEMETRY_STREAM)
                    .withName("Production-Telemetry-Stream")
                    .withDescription("Primary telemetry stream")
                    .create();

            assertNotNull(stream);
            assertEquals("d4e5f6a7-b8c9-0123-defa-345678901bcd", stream.getUuid());
            assertEquals("Production-Telemetry-Stream", stream.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("TELEMETRY_STREAM")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-Telemetry-Stream")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Primary telemetry stream")))
                    .withRequestBody(notMatching(".*\"enabled\".*")));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PUTs the StreamPutRequest body (name/description only), seeded from current state with overrides")
        void saveUpdatesStream() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*",
                    "/json/fabric/stream_response.json");
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/streams/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json"))));

            Stream stream = fabric.streams().getByUuid("d4e5f6a7-b8c9-0123-defa-345678901bcd");
            Stream updated = stream.update().withName("Renamed-Stream").save();

            assertNotNull(updated);
            // Spec StreamPutRequest carries only name/description; type/project are not sent.
            wireMock.verify(putRequestedFor(urlPathMatching("/fabric/v4/streams/d4e5f6a7-b8c9-0123-defa-345678901bcd"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Renamed-Stream")))
                    .withRequestBody(notMatching(".*\"type\".*")));
        }

        @Test
        @DisplayName("create() on an update-seeded builder is rejected")
        void createOnUpdateBuilderThrows() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*",
                    "/json/fabric/stream_response.json");

            Stream stream = fabric.streams().getByUuid("d4e5f6a7-b8c9-0123-defa-345678901bcd");
            assertThrows(IllegalStateException.class, () -> stream.update().withName("x").create());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/streams/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.streams().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("Wrapper refresh()")
    class WrapperRefresh {

        private static final String STREAM_ID = "d4e5f6a7-b8c9-0123-defa-345678901bcd";
        private static final String URL = "/fabric/v4/streams/" + STREAM_ID;

        @Test
        @DisplayName("re-GETs /streams/{uuid} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("stream-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json")))
                    .willSetStateTo("renamed"));
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .inScenario("stream-refresh")
                    .whenScenarioStateIs("renamed")
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json")
                            .replace("Production-Telemetry-Stream", "Renamed-Telemetry-Stream"))));

            Stream stream = fabric.streams().getByUuid(STREAM_ID);
            assertEquals("Production-Telemetry-Stream", stream.getName());

            stream.refresh();

            assertEquals("Renamed-Telemetry-Stream", stream.getName(),
                    "refresh() must swap the wrapper's backing state in place");
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Wrapper delete()")
    class WrapperDelete {

        private static final String STREAM_ID = "d4e5f6a7-b8c9-0123-defa-345678901bcd";
        private static final String URL = "/fabric/v4/streams/" + STREAM_ID;

        @Test
        @DisplayName("DELETEs /streams/{uuid} and returns true")
        void deletesStream() {
            wireMock.stubFor(get(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json"))));
            // deleteOne() reads the deleted resource from the response body, so the stub returns one.
            wireMock.stubFor(delete(urlPathEqualTo(URL))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json"))));

            Stream stream = fabric.streams().getByUuid(STREAM_ID);
            Boolean deleted = stream.delete();

            assertEquals(Boolean.TRUE, deleted);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo(URL)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE1_STREAM" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "PAGE2_STREAM" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() re-GETs /streams with the offset query param advanced to page 2")
        void loadAllFetchesSecondPage() {
            // Page 1: catch-all, registered first (WireMock: the later, more specific stub wins).
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/streams"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/streams"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            var streams = fabric.streams().list();
            assertEquals(1, streams.size());
            assertTrue(streams.hasNextPage());

            streams.loadAll();

            assertEquals(2, streams.size());
            assertEquals("PAGE1_STREAM", streams.get(0).getUuid());
            assertEquals("PAGE2_STREAM", streams.get(1).getUuid());
            assertFalse(streams.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/fabric/v4/streams"))
                    .withQueryParam("offset", equalTo("100")));
        }
    }
}
