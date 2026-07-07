package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.BGPStatus;
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
            assertEquals("conn-aaaa-bbbb-cccc-ddddeeeeffff", peering.getConnectionUuid());
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
            // getUuid()/getConnectionUuid() reflect the fixture body returned by the follow-up GET.
            assertEquals("bgp-1111-2222-3333-444455556666", peering.getUuid());
            assertEquals("conn-aaaa-bbbb-cccc-ddddeeeeffff", peering.getConnectionUuid());

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
    @DisplayName("update() / save()")
    class Update {

        private static final String UUID = "bgp-1111-2222-3333-444455556666";

        @Test
        @DisplayName("PUTs the merged update body then re-GETs the peering")
        void updatesBgpPeering() {
            // getByUuid() -> loads the existing peering the updater is derived from.
            stubSingleton(wireMock, "/ne/v1/bgp/" + UUID,
                    "/json/networkedge/bgppeering_response.json");
            // PUT /ne/v1/bgp/{uuid} -> 204 No Content (voidOp), then the impl re-GETs the peering.
            wireMock.stubFor(put(urlPathEqualTo("/ne/v1/bgp/" + UUID))
                    .willReturn(aResponse().withStatus(204)));

            BGPPeering peering = networkEdge.bgpPeerings().getByUuid(UUID);
            BGPPeering updated = peering.update()
                    .withRemoteAsn(65010L)
                    .withAuthenticationKey("new-secret")
                    .save();

            assertNotNull(updated);
            assertEquals(UUID, updated.getUuid());

            // The updater is seeded from the existing json, so the PUT body carries the merged
            // fields with the caller's overrides applied.
            wireMock.verify(putRequestedFor(urlPathEqualTo("/ne/v1/bgp/" + UUID))
                    .withRequestBody(matchingJsonPath("$.remoteAsn", equalTo("65010")))
                    .withRequestBody(matchingJsonPath("$.authenticationKey", equalTo("new-secret")))
                    .withRequestBody(matchingJsonPath("$.localIpAddress", equalTo("169.254.0.1/30")))
                    .withRequestBody(matchingJsonPath("$.remoteIpAddress", equalTo("169.254.0.2")))
                    .withRequestBody(matchingJsonPath("$.localAsn", equalTo("65000"))));
            // A follow-up GET re-fetches the updated peering.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/bgp/" + UUID)));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        private static final String UUID = "bgp-1111-2222-3333-444455556666";

        @Test
        @DisplayName("DELETEs the peering and returns true")
        void deletesBgpPeering() {
            // getByUuid() -> load the instance to delete.
            stubSingleton(wireMock, "/ne/v1/bgp/" + UUID,
                    "/json/networkedge/bgppeering_response.json");
            // DELETE /ne/v1/bgp/{uuid} -> 204 No Content.
            wireMock.stubFor(delete(urlPathEqualTo("/ne/v1/bgp/" + UUID))
                    .willReturn(aResponse().withStatus(204)));

            BGPPeering peering = networkEdge.bgpPeerings().getByUuid(UUID);
            Boolean result = peering.delete();

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/ne/v1/bgp/" + UUID)));
        }
    }

    @Nested
    @DisplayName("list()")
    class ListAll {

        @Test
        @DisplayName("GETs /ne/v1/bgp with no query params and maps the paginated body")
        void listsAllBgpPeerings() {
            // ListBGP -> GET /ne/v1/bgp (rootUri "bgp", no requestUri).
            stubPaginatedGet(wireMock, "/ne/v1/bgp/?",
                    "/json/networkedge/bgppeering_list_response.json");

            PaginatedList<BGPPeering> peerings = networkEdge.bgpPeerings().list();

            assertNotNull(peerings);
            assertEquals(2, peerings.size());
            assertEquals("bgp-1111-2222-3333-444455556666", peerings.get(0).getUuid());
            assertEquals("conn-aaaa-bbbb-cccc-ddddeeeeffff", peerings.get(0).getConnectionUuid());
            assertEquals("bgp-7777-8888-9999-aaaabbbbcccc", peerings.get(1).getUuid());
            assertEquals("conn-1111-2222-3333-444455556666", peerings.get(1).getConnectionUuid());

            // Verb + path, and that the unfiltered call carries none of the filter params.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/bgp"))
                    .withQueryParam("virtualDeviceUuid", absent())
                    .withQueryParam("connectionUuid", absent())
                    .withQueryParam("status", absent())
                    .withQueryParam("accountUcmId", absent()));
        }
    }

    @Nested
    @DisplayName("list(RequestBuilder.BGP)")
    class ListFiltered {

        @Test
        @DisplayName("GETs /ne/v1/bgp applying the builder's query params")
        void listsWithFilters() {
            stubPaginatedGet(wireMock, "/ne/v1/bgp/?",
                    "/json/networkedge/bgppeering_list_response.json");

            PaginatedList<BGPPeering> peerings = networkEdge.bgpPeerings().list(
                    RequestBuilder.bgp()
                            .forDevice("dev-1234-5678-90ab-cdef12345678")
                            .forConnection("conn-aaaa-bbbb-cccc-ddddeeeeffff")
                            .havingStatus(BGPStatus.PROVISIONED)
                            .forAccount("ucm-account-42"));

            assertNotNull(peerings);
            assertEquals(2, peerings.size());

            // Each with* on the builder maps to a query param on the GET.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/bgp"))
                    .withQueryParam("virtualDeviceUuid", equalTo("dev-1234-5678-90ab-cdef12345678"))
                    .withQueryParam("connectionUuid", equalTo("conn-aaaa-bbbb-cccc-ddddeeeeffff"))
                    .withQueryParam("status", equalTo("PROVISIONED"))
                    .withQueryParam("accountUcmId", equalTo("ucm-account-42")));
        }

        @Test
        @DisplayName("omits query params not set on the builder")
        void listsWithPartialFilters() {
            stubPaginatedGet(wireMock, "/ne/v1/bgp/?",
                    "/json/networkedge/bgppeering_list_response.json");

            PaginatedList<BGPPeering> peerings = networkEdge.bgpPeerings().list(
                    RequestBuilder.bgp().forConnection("conn-aaaa-bbbb-cccc-ddddeeeeffff"));

            assertNotNull(peerings);
            assertEquals(2, peerings.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/bgp"))
                    .withQueryParam("connectionUuid", equalTo("conn-aaaa-bbbb-cccc-ddddeeeeffff"))
                    .withQueryParam("virtualDeviceUuid", absent())
                    .withQueryParam("status", absent())
                    .withQueryParam("accountUcmId", absent()));
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
