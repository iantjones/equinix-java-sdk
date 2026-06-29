package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.CloudEvent;
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
}
