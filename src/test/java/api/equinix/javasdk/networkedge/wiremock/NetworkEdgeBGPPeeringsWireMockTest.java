package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge BGP Peerings.
 */
class NetworkEdgeBGPPeeringsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns BGP peering for valid UUID")
        void returnsBgpPeering() {
            stubSingleton(wireMock, "/ne/v1/bgp/.*",
                    "/json/networkedge/bgppeering_response.json");

            BGPPeering peering = networkEdge.bgpPeerings().getByUuid("bgp-1111-2222-3333-444455556666");
            assertNotNull(peering);
            assertEquals("bgp-1111-2222-3333-444455556666", peering.getUuid());
            assertEquals("test-connection", peering.getConnectionName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/bgp/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"BGP peering not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.bgpPeerings().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/bgp/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.bgpPeerings().getByUuid("test-uuid"));
        }
    }
}
