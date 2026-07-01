package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.enums.UserStatus;
import api.equinix.javasdk.networkedge.model.VPN;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
        @DisplayName("deserializes the expanded VpnResponse audit / org metadata block")
        void deserializesAuditBlock() {
            stubSingleton(wireMock, "/ne/v1/vpn/.*",
                    "/json/networkedge/vpn_response.json");

            VPN vpn = networkEdge.vpns().getByUuid("vpn-1111-2222-3333-444455556666");

            // 4-byte ASNs / org id modelled as Long.
            assertEquals(65555L, vpn.getCustOrgId());

            // created-by audit block
            assertEquals("John", vpn.getCreatedByFirstName());
            assertEquals("Smith", vpn.getCreatedByLastName());
            assertEquals("alpha@beta.com", vpn.getCreatedByEmail());
            assertEquals(123L, vpn.getCreatedByUserKey());
            assertEquals(456L, vpn.getCreatedByAccountUcmId());
            assertEquals("jsmith", vpn.getCreatedByUserName());
            assertEquals(7863L, vpn.getCreatedByCustOrgId());
            assertEquals("My Awesome Org", vpn.getCreatedByCustOrgName());
            assertEquals(UserStatus.ACTIVATED, vpn.getCreatedByUserStatus());
            assertEquals("My Awesome Company", vpn.getCreatedByCompanyName());

            // updated-by audit block
            assertEquals("Jane", vpn.getUpdatedByFirstName());
            assertEquals("Doe", vpn.getUpdatedByLastName());
            assertEquals("gamma@delta.com", vpn.getUpdatedByEmail());
            assertEquals(789L, vpn.getUpdatedByUserKey());
            assertEquals(1011L, vpn.getUpdatedByAccountUcmId());
            assertEquals("jdoe", vpn.getUpdatedByUserName());
            assertEquals(7863L, vpn.getUpdatedByCustOrgId());
            assertEquals("My Awesome Org", vpn.getUpdatedByCustOrgName());
            assertEquals(UserStatus.DEACTIVATED, vpn.getUpdatedByUserStatus());
            assertEquals("My Awesome Company", vpn.getUpdatedByCompanyName());
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
    @DisplayName("define() / save()")
    class Create {

        // A valid UUID is required in the Location header: createReturningLocationUuid extracts
        // the new uuid via Constants.UUID_PATTERN (8-4-4-4-12 hex) from the 201 Location header,
        // then issues a follow-up GET for that uuid.
        private static final String NEW_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        @Test
        @DisplayName("create POSTs the VPN config and resolves the new uuid from the Location header")
        void createsVpn() {
            // POST /ne/v1/vpn -> 201 with Location header carrying the new uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/vpn/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Location", "https://localhost/ne/v1/vpn/" + NEW_UUID)));
            // GET /ne/v1/vpn/{uuid} -> returns the created object body.
            stubSingleton(wireMock, "/ne/v1/vpn/.*", "/json/networkedge/vpn_response.json");

            var vpn = networkEdge.vpns()
                    .define("test-vpn-config")
                    .onDeviceUuid("dev-1234-5678-90ab-cdef12345678")
                    .withPeerIp("203.0.113.10")
                    .save();

            // Regression guard: VPNCreatorJson previously had two fields both annotated
            // @JsonProperty("virtualDeviceUuid"), so serialization always threw. Fixed so siteName
            // uses @JsonProperty("siteName") and the create body serializes cleanly.
            assertNotNull(vpn);
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/vpn/?"))
                    .withRequestBody(matchingJsonPath("$.configName", equalTo("test-vpn-config")))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceUuid", equalTo("dev-1234-5678-90ab-cdef12345678")))
                    .withRequestBody(matchingJsonPath("$.peerIp", equalTo("203.0.113.10"))));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + NEW_UUID)));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        private static final String VPN_UUID = "vpn-1111-2222-3333-444455556666";

        @Test
        @DisplayName("update PUTs the mutated config to /ne/v1/vpn/{uuid} and re-fetches it")
        void updatesVpn() {
            // The instance update() flow is: getByUuid() (GET) -> update() builder (seeds a
            // VPNUpdaterJson from the current VPNJson via convertValue) -> save() which PUTs the
            // full body then GETs the refreshed object.
            stubSingleton(wireMock, "/ne/v1/vpn/.*", "/json/networkedge/vpn_response.json");
            wireMock.stubFor(put(urlPathMatching("/ne/v1/vpn/.*"))
                    .willReturn(aResponse().withStatus(200)));

            VPN vpn = networkEdge.vpns().getByUuid(VPN_UUID);
            VPN updated = vpn.update()
                    .withConfigName("renamed-vpn-config")
                    .withPeerIp("198.51.100.20")
                    .withRemoteAsn(65010L)
                    .save();

            assertNotNull(updated);
            // The PUT carries the mutated fields plus the fields carried over from the current
            // config (e.g. virtualDeviceUuid), since the updater is seeded from the existing json.
            wireMock.verify(putRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + VPN_UUID))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.configName", equalTo("renamed-vpn-config")))
                    .withRequestBody(matchingJsonPath("$.peerIp", equalTo("198.51.100.20")))
                    .withRequestBody(matchingJsonPath("$.remoteAsn", equalTo("65010")))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceUuid",
                            equalTo("dev-1234-5678-90ab-cdef12345678"))));
            // save() re-fetches the object after the PUT.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + VPN_UUID)));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        private static final String VPN_UUID = "vpn-1111-2222-3333-444455556666";

        @Test
        @DisplayName("delete DELETEs /ne/v1/vpn/{uuid} and returns true")
        void deletesVpn() {
            // delete() is an instance op, so resolve the VPN via getByUuid() first.
            stubSingleton(wireMock, "/ne/v1/vpn/.*", "/json/networkedge/vpn_response.json");
            stubDeleteNoContent(wireMock, "/ne/v1/vpn/.*");

            VPN vpn = networkEdge.vpns().getByUuid(VPN_UUID);
            assertTrue(vpn.delete());

            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + VPN_UUID)));
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
