package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    @DisplayName("list()")
    class ListAll {

        @Test
        @DisplayName("GETs /ne/v1/links with no filter query params")
        void listsAllDeviceLinks() {
            stubPaginatedGet(wireMock, "/ne/v1/links/?",
                    "/json/networkedge/devicelink_list_response.json");

            PaginatedList<DeviceLink> deviceLinks = networkEdge.deviceLinks().list();

            assertNotNull(deviceLinks);
            assertEquals(2, deviceLinks.size());
            assertEquals("d1e2f3a4-b5c6-7890-abcd-1234567890ab", deviceLinks.get(0).getUuid());
            assertEquals("test-device-link", deviceLinks.get(0).getGroupName());
            assertEquals("second-device-link", deviceLinks.get(1).getGroupName());

            // GET verb + exact path; the unfiltered overload sends no filter params.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/links"))
                    .withQueryParam("metro", absent())
                    .withQueryParam("virtualDeviceUuid", absent())
                    .withQueryParam("accountUcmId", absent())
                    .withQueryParam("groupUuid", absent())
                    .withQueryParam("groupName", absent()));
        }
    }

    @Nested
    @DisplayName("list(RequestBuilder.DeviceLink)")
    class ListFiltered {

        @Test
        @DisplayName("GETs /ne/v1/links carrying the builder's filter query params")
        void listsFilteredDeviceLinks() {
            stubPaginatedGet(wireMock, "/ne/v1/links/?",
                    "/json/networkedge/devicelink_list_response.json");

            RequestBuilder.DeviceLink filter = RequestBuilder.deviceLink()
                    .inMetro(MetroCode.SV)
                    .forDeviceUuid("aaaa1111-bbbb-2222-cccc-3333dddd4444")
                    .forAccount("account-ucm-123")
                    .forGroupUuid("d1e2f3a4-b5c6-7890-abcd-1234567890ab")
                    .forGroupName("test-device-link")
                    .build();

            PaginatedList<DeviceLink> deviceLinks = networkEdge.deviceLinks().list(filter);

            assertNotNull(deviceLinks);
            assertEquals(2, deviceLinks.size());

            // GET verb + exact path; each builder field maps to its wire query-param name.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/links"))
                    .withQueryParam("metro", equalTo("SV"))
                    .withQueryParam("virtualDeviceUuid", equalTo("aaaa1111-bbbb-2222-cccc-3333dddd4444"))
                    .withQueryParam("accountUcmId", equalTo("account-ucm-123"))
                    .withQueryParam("groupUuid", equalTo("d1e2f3a4-b5c6-7890-abcd-1234567890ab"))
                    .withQueryParam("groupName", equalTo("test-device-link")));
        }
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
    @DisplayName("define() / save()")
    class Create {

        private static final String NEW_UUID = "d1e2f3a4-b5c6-7890-abcd-1234567890ab";

        @Test
        @DisplayName("POSTs the create body, then GETs the new device link")
        void createsDeviceLink() {
            // POST /ne/v1/links -> 200 with a UUIDResult body carrying the new uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/links/?"))
                    .willReturn(okJson("{\"uuid\":\"" + NEW_UUID + "\"}")));
            // The create() impl follows the POST with a GET of the new uuid.
            stubSingleton(wireMock, "/ne/v1/links/.*", "/json/networkedge/devicelink_response.json");

            DeviceLink deviceLink = networkEdge.deviceLinks()
                    .define("test-device-link")
                    .onSubnet("10.0.0.0/24")
                    .withRedundancyType(RedundancyType.PRIMARY)
                    .withLink("Acme Corp", "50", "MB", MetroCode.SV)
                    .forDevice("aaaa1111-bbbb-2222-cccc-3333dddd4444", 65000L, 6)
                    .save();

            assertNotNull(deviceLink);
            // Reflects the fixture body returned by the follow-up GET.
            assertEquals(NEW_UUID, deviceLink.getUuid());
            assertEquals("test-device-link", deviceLink.getGroupName());

            // Verify the serialized create request body.
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/links/?"))
                    .withRequestBody(equalToJson(
                            "{" +
                                    "\"groupName\":\"test-device-link\"," +
                                    "\"subnet\":\"10.0.0.0/24\"," +
                                    "\"redundancyType\":\"PRIMARY\"," +
                                    "\"metroLinks\":[{" +
                                    "\"accountNumber\":\"Acme Corp\"," +
                                    "\"throughput\":\"50\"," +
                                    "\"throughputUnit\":\"MB\"," +
                                    "\"metroCode\":\"SV\"}]," +
                                    "\"linkDevices\":[{" +
                                    "\"deviceUuid\":\"aaaa1111-bbbb-2222-cccc-3333dddd4444\"," +
                                    "\"asn\":65000," +
                                    "\"interfaceId\":6}]" +
                                    "}", true, true)));
            // And the follow-up GET of the returned uuid.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/links/" + NEW_UUID)));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        private static final String UUID = "d1e2f3a4-b5c6-7890-abcd-1234567890ab";

        @Test
        @DisplayName("PATCHes the changed fields, then GETs the updated device link")
        void updatesDeviceLink() {
            // GET to fetch the existing link (update() seeds the updater from this body).
            stubSingleton(wireMock, "/ne/v1/links/.*", "/json/networkedge/devicelink_response.json");
            // PATCH /ne/v1/links/{uuid} -> 200 (voidOp), then update() re-GETs the link.
            wireMock.stubFor(patch(urlPathMatching("/ne/v1/links/" + UUID))
                    .willReturn(okJson(loadFixture("/json/networkedge/devicelink_response.json"))));

            DeviceLink deviceLink = networkEdge.deviceLinks().getByUuid(UUID);

            DeviceLink updated = deviceLink.update()
                    .withGroupName("renamed-device-link")
                    .onSubnet("10.1.0.0/24")
                    .save();

            assertNotNull(updated);

            // The updater is seeded from the fetched body, so the PATCH carries the whole
            // resource with the two changed fields overlaid — assert those changed fields.
            wireMock.verify(patchRequestedFor(urlPathMatching("/ne/v1/links/" + UUID))
                    .withRequestBody(matchingJsonPath("$.groupName", equalTo("renamed-device-link")))
                    .withRequestBody(matchingJsonPath("$.subnet", equalTo("10.1.0.0/24"))));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        private static final String UUID = "d1e2f3a4-b5c6-7890-abcd-1234567890ab";

        @Test
        @DisplayName("DELETEs the device link by uuid")
        void deletesDeviceLink() {
            // Fetch the instance so we have a DeviceLink to call delete() on.
            stubSingleton(wireMock, "/ne/v1/links/.*", "/json/networkedge/devicelink_response.json");
            wireMock.stubFor(delete(urlPathMatching("/ne/v1/links/" + UUID))
                    .willReturn(noContent()));

            DeviceLink deviceLink = networkEdge.deviceLinks().getByUuid(UUID);
            Boolean result = deviceLink.delete();

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/ne/v1/links/" + UUID)));
        }
    }

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private static final String UUID = "d1e2f3a4-b5c6-7890-abcd-1234567890ab";
        private static final String PATH = "/ne/v1/links/" + UUID;

        @Test
        @DisplayName("re-GETs the device link and updates the wrapper's state in place")
        void refreshesInPlace() {
            // First GET returns the original state; the second GET — triggered by
            // wrapper.refresh() — returns a DIFFERENT payload (renamed group, new subnet).
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("devicelink-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/networkedge/devicelink_response.json")))
                    .willSetStateTo("state-changed"));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("devicelink-refresh")
                    .whenScenarioStateIs("state-changed")
                    .willReturn(okJson(loadFixture("/json/networkedge/devicelink_response_refreshed.json"))));

            DeviceLink deviceLink = networkEdge.deviceLinks().getByUuid(UUID);
            assertEquals("test-device-link", deviceLink.getGroupName());
            assertEquals("10.0.0.0/24", deviceLink.getSubnet());

            assertTrue(deviceLink.refresh());

            // The same wrapper instance now reflects the re-fetched server state.
            assertEquals("renamed-device-link", deviceLink.getGroupName());
            assertEquals("10.1.0.0/24", deviceLink.getSubnet());
            assertEquals(UUID, deviceLink.getUuid());

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
                    "uuid": "d1e2f3a4-b5c6-7890-abcd-1234567890ab",
                    "groupName": "page1-device-link",
                    "subnet": "10.0.0.0/24",
                    "status": "PROVISIONED",
                    "redundancyType": "PRIMARY",
                    "metroLinks": [ {
                      "accountName": "Acme Corp",
                      "metroCode": "SV",
                      "metroName": "Silicon Valley",
                      "throughput": "50",
                      "throughputUnit": "MB"
                    } ],
                    "linkDevices": [ {
                      "deviceUuid": "aaaa1111-bbbb-2222-cccc-3333dddd4444",
                      "interfaceId": 6
                    } ]
                  } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 1, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "e2f3a4b5-c6d7-8901-bcde-2345678901bc",
                    "groupName": "page2-device-link",
                    "subnet": "10.1.0.0/24",
                    "status": "PROVISIONED",
                    "redundancyType": "PRIMARY",
                    "metroLinks": [ {
                      "accountName": "Globex LLC",
                      "metroCode": "DC",
                      "metroName": "Washington DC",
                      "throughput": "100",
                      "throughputUnit": "MB"
                    } ],
                    "linkDevices": [ {
                      "deviceUuid": "bbbb2222-cccc-3333-dddd-4444eeee5555",
                      "interfaceId": 7
                    } ]
                  } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset/limit query params")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/links"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/links"))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<DeviceLink> deviceLinks = networkEdge.deviceLinks().list();
            assertEquals(1, deviceLinks.size());
            assertTrue(deviceLinks.hasNextPage());

            deviceLinks.loadAll();

            assertEquals(2, deviceLinks.size());
            assertEquals("page1-device-link", deviceLinks.get(0).getGroupName());
            assertEquals("page2-device-link", deviceLinks.get(1).getGroupName());
            assertFalse(deviceLinks.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/ne/v1/links"))
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
            stubErrorInline(wireMock, "/ne/v1/links/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.deviceLinks().getByUuid("test-uuid"));
        }
    }
}
