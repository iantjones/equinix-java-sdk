package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge Device Links.
 */
class NetworkEdgeDeviceLinksWireMockTest extends WireMockTestBase {

    static NetworkEdge networkEdge;

    @BeforeAll
    static void setUp() {
        networkEdge = new NetworkEdge(testCredentials());
        redirectToWireMock(networkEdge);
        networkEdge.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (networkEdge != null) networkEdge.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns device link for valid UUID")
        void returnsDeviceLink() {
            stubSingleton(wireMock, "/ne/v1/links/.*",
                    "/json/networkedge/devicelink_response.json");

            DeviceLink deviceLink = networkEdge.deviceLinks().getByUuid("d1e2f3a4-b5c6-7890-abcd-1234567890ab");
            assertNotNull(deviceLink);
            assertEquals("d1e2f3a4-b5c6-7890-abcd-1234567890ab", deviceLink.getUuid());
            assertEquals("test-device-link", deviceLink.getGroupName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/links/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Device link not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.deviceLinks().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/links/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.deviceLinks().getByUuid("test-uuid"));
        }
    }
}
