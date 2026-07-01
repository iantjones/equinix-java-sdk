package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.*;
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
