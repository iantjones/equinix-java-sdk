package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.StreamType;
import api.equinix.javasdk.fabric.model.Stream;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
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
                    .withEnabled(true)
                    .create();

            assertNotNull(stream);
            assertEquals("d4e5f6a7-b8c9-0123-defa-345678901bcd", stream.getUuid());
            assertEquals("Production-Telemetry-Stream", stream.getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/streams"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("TELEMETRY_STREAM")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Production-Telemetry-Stream")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Primary telemetry stream")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("true"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("PUTs the full stream body, seeded from current state with overrides")
        void saveUpdatesStream() {
            stubSingleton(wireMock, "/fabric/v4/streams/.*",
                    "/json/fabric/stream_response.json");
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/streams/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/stream_response.json"))));

            Stream stream = fabric.streams().getByUuid("d4e5f6a7-b8c9-0123-defa-345678901bcd");
            Stream updated = stream.update().withName("Renamed-Stream").withEnabled(false).save();

            assertNotNull(updated);
            // Full-body PUT: unchanged fields preserved from current state, overrides applied.
            wireMock.verify(putRequestedFor(urlPathMatching("/fabric/v4/streams/d4e5f6a7-b8c9-0123-defa-345678901bcd"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Renamed-Stream")))
                    .withRequestBody(matchingJsonPath("$.enabled", equalTo("false")))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("TELEMETRY_STREAM"))));
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
}
