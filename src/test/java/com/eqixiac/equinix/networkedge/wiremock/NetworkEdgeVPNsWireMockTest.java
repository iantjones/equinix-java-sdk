package com.eqixiac.equinix.networkedge.wiremock;

import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.IPAddress;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.enums.UserStatus;
import com.eqixiac.equinix.networkedge.enums.VPNStatus;
import com.eqixiac.equinix.networkedge.model.VPN;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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
    @DisplayName("list()")
    class ListAll {

        @Test
        @DisplayName("unfiltered list GETs /ne/v1/vpn and maps the paginated body")
        void listsAllVpns() {
            // ListVPNs -> GET /ne/v1/vpn (rootUri "vpn", no requestUri suffix).
            stubPaginatedGet(wireMock, "/ne/v1/vpn",
                    "/json/networkedge/vpn_list_response.json");

            PaginatedList<VPN> vpns = networkEdge.vpns().list();

            assertNotNull(vpns);
            assertEquals(2, vpns.size());
            assertEquals("vpn-1111-2222-3333-444455556666", vpns.get(0).getUuid());
            assertEquals("test-vpn-config", vpns.get(0).getConfigName());
            assertEquals("vpn-aaaa-bbbb-cccc-ddddeeeeffff", vpns.get(1).getUuid());
            assertEquals("second-vpn-config", vpns.get(1).getConfigName());

            // Unfiltered list() sends no VPN-specific query parameters.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("statusList", absent())
                    .withQueryParam("virtualDeviceUuid", absent()));
        }
    }

    @Nested
    @DisplayName("list(RequestBuilder.VPN)")
    class ListFiltered {

        @Test
        @DisplayName("filtered list applies statusList + virtualDeviceUuid query params")
        void listsFilteredVpns() {
            stubPaginatedGet(wireMock, "/ne/v1/vpn",
                    "/json/networkedge/vpn_list_response.json");

            PaginatedList<VPN> vpns = networkEdge.vpns().list(
                    RequestBuilder.vpn()
                            .withStatus(VPNStatus.PROVISIONED)
                            .forDeviceUuid("dev-1234-5678-90ab-cdef12345678")
                            .build());

            assertNotNull(vpns);
            assertEquals(2, vpns.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("statusList", equalTo("PROVISIONED"))
                    .withQueryParam("virtualDeviceUuid", equalTo("dev-1234-5678-90ab-cdef12345678")));
        }

        @Test
        @DisplayName("multiple statuses are sent as repeated statusList query params")
        void listsMultipleStatuses() {
            stubPaginatedGet(wireMock, "/ne/v1/vpn",
                    "/json/networkedge/vpn_list_response.json");

            PaginatedList<VPN> vpns = networkEdge.vpns().list(
                    RequestBuilder.vpn()
                            .withStatus(VPNStatus.PROVISIONED)
                            .withStatus(VPNStatus.PROVISIONING)
                            .build());

            assertNotNull(vpns);
            // Each enum is emitted as its own statusList value (ApacheUtils repeats the pair),
            // so the wire form is ?statusList=PROVISIONED&statusList=PROVISIONING.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("statusList", havingExactly("PROVISIONED", "PROVISIONING")));
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

        @Test
        @DisplayName("withPeerIp/withRemoteIpAddress/withTunnelIp(IPAddress) POST a byte-identical body to the String setters")
        void typedIpOverloadsMatchStringPath() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/vpn/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Location", "https://localhost/ne/v1/vpn/" + NEW_UUID)));
            stubSingleton(wireMock, "/ne/v1/vpn/.*", "/json/networkedge/vpn_response.json");

            // Same create issued twice: once via the String setters, once via the typed
            // IPAddress overloads (which format via IPAddress.toCidr()).
            networkEdge.vpns()
                    .define("test-vpn-config")
                    .onDeviceUuid("dev-1234-5678-90ab-cdef12345678")
                    .withPeerIp("203.0.113.10")
                    .withRemoteIpAddress("192.0.2.5")
                    .withTunnelIp("172.16.0.30/30")
                    .save();
            networkEdge.vpns()
                    .define("test-vpn-config")
                    .onDeviceUuid("dev-1234-5678-90ab-cdef12345678")
                    .withPeerIp(IPAddress.parse("203.0.113.10"))
                    .withRemoteIpAddress(IPAddress.parse("192.0.2.5"))
                    .withTunnelIp(IPAddress.parse("172.16.0.30/30"))
                    .save();

            var posts = wireMock.findAll(postRequestedFor(urlPathMatching("/ne/v1/vpn/?")));
            assertEquals(2, posts.size());
            // The typed overloads serialize byte-for-byte identically to the String path.
            assertEquals(posts.get(0).getBodyAsString(), posts.get(1).getBodyAsString());
            // And both carry the expected wire values (the CIDR subnet on tunnelIp survives).
            wireMock.verify(2, postRequestedFor(urlPathMatching("/ne/v1/vpn/?"))
                    .withRequestBody(matchingJsonPath("$.peerIp", equalTo("203.0.113.10")))
                    .withRequestBody(matchingJsonPath("$.remoteIpAddress", equalTo("192.0.2.5")))
                    .withRequestBody(matchingJsonPath("$.tunnelIp", equalTo("172.16.0.30/30"))));
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

        @Test
        @DisplayName("updater withPeerIp/withRemoteIpAddress/withTunnelIp(IPAddress) PUT a byte-identical body to the String setters")
        void typedIpOverloadsMatchStringPathOnUpdate() {
            stubSingleton(wireMock, "/ne/v1/vpn/.*", "/json/networkedge/vpn_response.json");
            wireMock.stubFor(put(urlPathMatching("/ne/v1/vpn/.*"))
                    .willReturn(aResponse().withStatus(200)));

            VPN vpn = networkEdge.vpns().getByUuid(VPN_UUID);

            // Same update issued twice: once via the String setters, once via the typed
            // IPAddress overloads (which format via IPAddress.toCidr()).
            vpn.update()
                    .withPeerIp("198.51.100.20")
                    .withRemoteIpAddress("192.0.2.5")
                    .withTunnelIp("172.16.0.30/30")
                    .save();
            vpn.update()
                    .withPeerIp(IPAddress.parse("198.51.100.20"))
                    .withRemoteIpAddress(IPAddress.parse("192.0.2.5"))
                    .withTunnelIp(IPAddress.parse("172.16.0.30/30"))
                    .save();

            var puts = wireMock.findAll(putRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + VPN_UUID)));
            assertEquals(2, puts.size());
            assertEquals(puts.get(0).getBodyAsString(), puts.get(1).getBodyAsString());
            wireMock.verify(2, putRequestedFor(urlPathEqualTo("/ne/v1/vpn/" + VPN_UUID))
                    .withRequestBody(matchingJsonPath("$.peerIp", equalTo("198.51.100.20")))
                    .withRequestBody(matchingJsonPath("$.remoteIpAddress", equalTo("192.0.2.5")))
                    .withRequestBody(matchingJsonPath("$.tunnelIp", equalTo("172.16.0.30/30"))));
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
    @DisplayName("refresh()")
    class Refresh {

        private static final String VPN_UUID = "vpn-1111-2222-3333-444455556666";
        private static final String PATH = "/ne/v1/vpn/" + VPN_UUID;

        @Test
        @DisplayName("re-GETs the VPN and updates the wrapper's state in place")
        void refreshesInPlace() {
            // First GET returns the original state; the second GET — triggered by
            // wrapper.refresh() — returns a DIFFERENT payload (renamed config, new peerIp).
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("vpn-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/networkedge/vpn_response.json")))
                    .willSetStateTo("state-changed"));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("vpn-refresh")
                    .whenScenarioStateIs("state-changed")
                    .willReturn(okJson(loadFixture("/json/networkedge/vpn_response_refreshed.json"))));

            VPN vpn = networkEdge.vpns().getByUuid(VPN_UUID);
            assertEquals("test-vpn-config", vpn.getConfigName());
            assertEquals("203.0.113.10", vpn.getPeerIp());

            assertTrue(vpn.refresh());

            // The same wrapper instance now reflects the re-fetched server state.
            assertEquals("renamed-vpn-config", vpn.getConfigName());
            assertEquals("198.51.100.20", vpn.getPeerIp());
            assertEquals(VPN_UUID, vpn.getUuid());

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "vpn-1111-2222-3333-444455556666",
                    "siteName": "test-site",
                    "status": "PROVISIONED",
                    "virtualDeviceUuid": "dev-1234-5678-90ab-cdef12345678",
                    "configName": "page1-vpn-config",
                    "peerIp": "203.0.113.10",
                    "remoteAsn": 65001,
                    "localAsn": 65000
                  } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 1, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "vpn-aaaa-bbbb-cccc-ddddeeeeffff",
                    "siteName": "second-site",
                    "status": "PROVISIONING",
                    "virtualDeviceUuid": "dev-9999-8888-7777-666655554444",
                    "configName": "page2-vpn-config",
                    "peerIp": "198.51.100.20",
                    "remoteAsn": 65010,
                    "localAsn": 65000
                  } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset/limit query params")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<VPN> vpns = networkEdge.vpns().list();
            assertEquals(1, vpns.size());
            assertTrue(vpns.hasNextPage());

            vpns.loadAll();

            assertEquals(2, vpns.size());
            assertEquals("page1-vpn-config", vpns.get(0).getConfigName());
            assertEquals("page2-vpn-config", vpns.get(1).getConfigName());
            assertFalse(vpns.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/ne/v1/vpn"))
                    .withQueryParam("offset", equalTo("1"))
                    .withQueryParam("limit", equalTo("1")));
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
