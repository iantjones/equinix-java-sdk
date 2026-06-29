package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    @DisplayName("define() / save()")
    class Create {

        // Valid UUID returned in the 202 BgpAsyncResponse body (Constants.UUID_PATTERN = 8-4-4-4-12 hex).
        private static final String NEW_UUID = "b1c2d3e4-f5a6-7890-bcde-f12345678901";

        @Test
        @DisplayName("POSTs the create body, reads the uuid from the 202 BgpAsyncResponse body, and GETs the new BGP peering")
        void createsBgpPeering() {
            // POST /ne/v1/bgp -> 202 Accepted with a BgpAsyncResponse body carrying the new uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/bgp/?"))
                    .willReturn(aResponse()
                            .withStatus(202)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            // GET /ne/v1/bgp/{uuid} -> returns the created object body.
            stubSingleton(wireMock, "/ne/v1/bgp/" + NEW_UUID, "/json/networkedge/bgppeering_response.json");

            BGPPeering peering = networkEdge.bgpPeerings()
                    .define()
                    .forConnection("conn-aaaa-bbbb-cccc-ddddeeeeffff")
                    .withLocalIpAddress("169.254.0.1/30")
                    .withRemoteIpAddress("169.254.0.2")
                    .withLocalAsn(65000L)
                    .withRemoteAsn(65001L)
                    .save();

            assertNotNull(peering);
            // getUuid()/getConnectionName() reflect the fixture body returned by the follow-up GET.
            assertEquals("bgp-1111-2222-3333-444455556666", peering.getUuid());
            assertEquals("test-connection", peering.getConnectionName());

            // Verify the outgoing create request body.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/bgp/?"))
                    .withRequestBody(matchingJsonPath("$.connectionUuid", equalTo("conn-aaaa-bbbb-cccc-ddddeeeeffff")))
                    .withRequestBody(matchingJsonPath("$.localIpAddress", equalTo("169.254.0.1/30")))
                    .withRequestBody(matchingJsonPath("$.remoteIpAddress", equalTo("169.254.0.2")))
                    .withRequestBody(matchingJsonPath("$.localAsn", equalTo("65000")))
                    .withRequestBody(matchingJsonPath("$.remoteAsn", equalTo("65001"))));
            // Verify the follow-up GET for the uuid parsed from the 202 response body.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/bgp/" + NEW_UUID)));
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
