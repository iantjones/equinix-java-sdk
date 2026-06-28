package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Precision Time services.
 */
class FabricPrecisionTimesWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns time service for valid UUID (correct endpoint after rename)")
        void returnsTimeService() {
            stubSingleton(wireMock, "/fabric/v4/timeServices/.*",
                    "/json/fabric/precision_time_response.json");

            PrecisionTime ts = fabric.precisionTimes().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            assertNotNull(ts);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", ts.getUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/timeServices/f6a7b8c9-d0e1-2345-fabc-567890123def")));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        @Test
        @DisplayName("sends an RFC 6902 JSON Patch with json-patch content-type")
        void savePatchesName() {
            stubSingleton(wireMock, "/fabric/v4/timeServices/.*",
                    "/json/fabric/precision_time_response.json");
            wireMock.stubFor(patch(urlPathMatching("/fabric/v4/timeServices/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/precision_time_response.json"))));

            PrecisionTime ts = fabric.precisionTimes().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            PrecisionTime updated = ts.update().name("Renamed-NTP").save();

            assertNotNull(updated);
            wireMock.verify(patchRequestedFor(urlPathMatching("/fabric/v4/timeServices/f6a7b8c9-d0e1-2345-fabc-567890123def"))
                    .withHeader("Content-Type", containing("application/json-patch+json"))
                    .withRequestBody(equalToJson(
                            "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Renamed-NTP\"}]")));
        }

        @Test
        @DisplayName("save() with no changes throws and makes no request")
        void emptyUpdateThrows() {
            stubSingleton(wireMock, "/fabric/v4/timeServices/.*",
                    "/json/fabric/precision_time_response.json");

            PrecisionTime ts = fabric.precisionTimes().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            assertThrows(IllegalStateException.class, () -> ts.update().save());
            wireMock.verify(0, patchRequestedFor(urlPathMatching("/fabric/v4/timeServices/.*")));
        }
    }

    @Nested
    @DisplayName("fulfill()")
    class Fulfill {

        @Test
        @DisplayName("PUTs the connections body to {uuid} and returns the updated service")
        void fulfillsTimeService() {
            wireMock.stubFor(put(urlPathMatching("/fabric/v4/timeServices/.*"))
                    .willReturn(okJson(loadFixture("/json/fabric/precision_time_response.json"))));

            PrecisionTime ts = fabric.precisionTimes().fulfill(
                    "f6a7b8c9-d0e1-2345-fabc-567890123def",
                    List.of("095be615-a8ad-4c33-8e9c-c7612fbf6c9f"));

            assertNotNull(ts);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", ts.getUuid());

            wireMock.verify(putRequestedFor(urlPathEqualTo("/fabric/v4/timeServices/f6a7b8c9-d0e1-2345-fabc-567890123def"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.connections[0].uuid",
                            equalTo("095be615-a8ad-4c33-8e9c-c7612fbf6c9f"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/timeServices/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.precisionTimes().getByUuid("test-uuid"));
        }
    }
}
