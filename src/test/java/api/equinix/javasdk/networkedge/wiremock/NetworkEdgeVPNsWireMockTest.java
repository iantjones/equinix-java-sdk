package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.VPN;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge VPNs.
 */
class NetworkEdgeVPNsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns VPN for valid UUID")
        void returnsVpn() {
            stubSingleton(wireMock, "/ne/v1/vpn/.*",
                    "/json/networkedge/vpn_response.json");

            VPN vpn = networkEdge.vpns().getByUuid("vpn-1111-2222-3333-444455556666");
            assertNotNull(vpn);
            assertEquals("vpn-1111-2222-3333-444455556666", vpn.getUuid());
            assertEquals("test-vpn-config", vpn.getConfigName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/vpn/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"VPN not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.vpns().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/vpn/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.vpns().getByUuid("test-uuid"));
        }
    }
}
