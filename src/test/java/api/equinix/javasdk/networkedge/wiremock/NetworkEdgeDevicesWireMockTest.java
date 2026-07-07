package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.IPAddress;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.ACLInterfaceType;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.AllowedInterfaceResponse;
import api.equinix.javasdk.networkedge.model.implementation.DeviceACL;
import api.equinix.javasdk.networkedge.model.implementation.DeviceVendorConfig;
import api.equinix.javasdk.networkedge.model.implementation.DeviceReboot;
import api.equinix.javasdk.networkedge.model.implementation.DeviceUpgrade;
import api.equinix.javasdk.networkedge.model.implementation.DownloadableImage;
import api.equinix.javasdk.networkedge.model.implementation.ImageDownload;
import api.equinix.javasdk.networkedge.model.implementation.InterfaceStats;
import api.equinix.javasdk.networkedge.model.implementation.NetworkInterface;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge Devices.
 */
class NetworkEdgeDevicesWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns device for valid UUID")
        void returnsDevice() {
            stubSingleton(wireMock, "/ne/v1/devices/.*",
                    "/json/networkedge/device_response.json");

            Device device = networkEdge.devices().getByUuid("test-device-uuid");
            assertNotNull(device);
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/devices/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Device not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.devices().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("list() / list(RequestBuilder.Device)")
    class ListDevices {

        @Test
        @DisplayName("list() GETs /ne/v1/devices with no filter query params")
        void listsAllDevices() {
            stubPaginatedGet(wireMock, "/ne/v1/devices/?",
                    "/json/networkedge/device_list_response.json");

            PaginatedList<Device> devices = networkEdge.devices().list();

            assertNotNull(devices);
            assertEquals(2, devices.size());
            assertEquals("ed7891f4-7a67-11e9-9bea-1681be663d3e", devices.get(0).getUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices")));
        }

        @Test
        @DisplayName("list(RequestBuilder.Device) applies metroCode/status/accountUcmId/showOnlySubCustomerDevices query params")
        void listsFilteredDevices() {
            stubPaginatedGet(wireMock, "/ne/v1/devices/?",
                    "/json/networkedge/device_list_response.json");

            RequestBuilder.Device filter = RequestBuilder.device()
                    .inMetro(MetroCode.SV)
                    .inMetro(MetroCode.DC)
                    .havingStatus(DeviceStatus.PROVISIONED)
                    .forAccount("ucm-account-42")
                    .showOnlySubCustomerDevices(true);
            filter.build();

            PaginatedList<Device> devices = networkEdge.devices().list(filter);

            assertNotNull(devices);
            assertEquals(2, devices.size());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices"))
                    .withQueryParam("metroCode", equalTo("SV"))
                    .withQueryParam("metroCode", equalTo("DC"))
                    .withQueryParam("status", equalTo("PROVISIONED"))
                    .withQueryParam("accountUcmId", equalTo("ucm-account-42"))
                    .withQueryParam("showOnlySubCustomerDevices", equalTo("true")));
        }
    }

    @Nested
    @DisplayName("listInterfaces() / listAllowedInterfaces()")
    class Interfaces {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("listInterfaces() GETs {uuid}/interfaces and returns the interface list")
        void listsInterfaces() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces"))
                    .willReturn(okJson(loadFixture("/json/networkedge/network_interface_list_response.json"))));

            List<NetworkInterface> interfaces = networkEdge.devices().listInterfaces(UUID);

            assertNotNull(interfaces);
            assertEquals(2, interfaces.size());
            assertEquals("eth0", interfaces.get(0).getName());
            // Spec InterfaceBasicInfoResponse names the wire property "operationStatus";
            // the model keeps the historical getOperationalStatus() accessor.
            assertEquals(OperationalStatus.UP, interfaces.get(0).getOperationalStatus());
            assertEquals(OperationalStatus.DOWN, interfaces.get(1).getOperationalStatus());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces")));
        }

        @Test
        @DisplayName("listAllowedInterfaces() GETs /ne/v1/deviceTypes/{deviceType}/interfaces (overrideRootUri) with the config query params")
        void listsAllowedInterfaces() {
            // overrideRootUri:true -> the path is /ne/v1/deviceTypes/{deviceType}/interfaces, NOT under /devices.
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/deviceTypes/CSR1000V/interfaces"))
                    .willReturn(okJson(loadFixture("/json/networkedge/allowed_interfaces_response.json"))));

            RequestBuilder.AllowedInterfaces requestBuilder =
                    RequestBuilder.allowedInterfaces("CSR1000V", DeviceManagementType.SELF_CONFIGURED)
                            .withMode(LicenseType.SUB)
                            .withCluster(false)
                            .withCore(2);

            AllowedInterfaceResponse response = networkEdge.devices().listAllowedInterfaces(requestBuilder);

            assertNotNull(response);
            assertNotNull(response.getInterfaceProfiles());
            assertEquals(2, response.getInterfaceProfiles().size());
            assertEquals(3, response.getInterfaceProfiles().get(0).getCount());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/deviceTypes/CSR1000V/interfaces"))
                    .withQueryParam("deviceManagementType", equalTo("SELF-CONFIGURED"))
                    .withQueryParam("mode", equalTo("Subscription"))
                    .withQueryParam("cluster", equalTo("false"))
                    .withQueryParam("core", equalTo("2")));
        }
    }

    @Nested
    @DisplayName("listReloadHistory() / listUpgradeHistory()")
    class History {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("listReloadHistory() GETs {uuid}/softReboot and returns the reboot history (data[])")
        void listsReloadHistory() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/softReboot"))
                    .willReturn(okJson(loadFixture("/json/networkedge/reload_history_response.json"))));

            List<DeviceReboot> history = networkEdge.devices().listReloadHistory(UUID);

            assertNotNull(history);
            assertEquals(2, history.size());
            assertEquals(UUID, history.get(0).getDeviceUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/softReboot")));
        }

        @Test
        @DisplayName("listUpgradeHistory() GETs {uuid}/resourceUpgrade and returns the upgrade history (data[])")
        void listsUpgradeHistory() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/resourceUpgrade"))
                    .willReturn(okJson(loadFixture("/json/networkedge/upgrade_history_response.json"))));

            List<DeviceUpgrade> history = networkEdge.devices().listUpgradeHistory(UUID);

            assertNotNull(history);
            assertEquals(2, history.size());
            assertEquals(UUID, history.get(0).getVirtualDeviceUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/resourceUpgrade")));
        }
    }

    @Nested
    @DisplayName("listDownloadableImages() / getInterfaceStatistics() / getDeviceAcl()")
    class MiscReads {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("listDownloadableImages() GETs {deviceType}/repositories and returns the image list")
        void listsDownloadableImages() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/CSR1000V/repositories"))
                    .willReturn(okJson(loadFixture("/json/networkedge/downloadable_image_list_response.json"))));

            List<DownloadableImage> images = networkEdge.devices().listDownloadableImages("CSR1000V");

            assertNotNull(images);
            assertEquals(2, images.size());
            assertEquals("16.09.05", images.get(0).getVersion());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/CSR1000V/repositories")));
        }

        @Test
        @DisplayName("getInterfaceStatistics() GETs {uuid}/interfaces/{interfaceId}/stats with startDateTime/endDateTime query params")
        void getsInterfaceStatistics() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces/1/stats"))
                    .willReturn(okJson(loadFixture("/json/networkedge/interface_stats_response.json"))));

            InterfaceStats stats = networkEdge.devices()
                    .getInterfaceStatistics(UUID, "1", "2021-05-01T00:00:00Z", "2021-05-02T00:00:00Z");

            assertNotNull(stats);
            assertNotNull(stats.getStats());
            assertEquals("bps", stats.getStats().getUnit());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces/1/stats"))
                    .withQueryParam("startDateTime", equalTo("2021-05-01T00:00:00Z"))
                    .withQueryParam("endDateTime", equalTo("2021-05-02T00:00:00Z")));
        }

        @Test
        @DisplayName("getInterfaceStatistics() with null date bounds sends no date query params")
        void getsInterfaceStatisticsNoDates() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces/1/stats"))
                    .willReturn(okJson(loadFixture("/json/networkedge/interface_stats_response.json"))));

            InterfaceStats stats = networkEdge.devices()
                    .getInterfaceStatistics(UUID, "1", null, null);

            assertNotNull(stats);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/interfaces/1/stats"))
                    .withoutQueryParam("startDateTime")
                    .withoutQueryParam("endDateTime"));
        }

        @Test
        @DisplayName("getDeviceAcl() GETs {uuid}/acl and returns the device ACL state")
        void getsDeviceAcl() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .willReturn(okJson(loadFixture("/json/networkedge/device_acl_response.json"))));

            DeviceACL acl = networkEdge.devices().getDeviceAcl(UUID);

            assertNotNull(acl);
            assertNotNull(acl.getAclTemplate());
            assertEquals("be7ef79e-31e7-4769-be5b-e192496f48aa", acl.getAclTemplate().getUuid());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/ne/v1/devices/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> networkEdge.devices().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/devices/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.devices().getByUuid("test-uuid"));
        }
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        // Valid UUID returned in the CreateDevice response body (parsed into UUIDResult).
        private static final String NEW_UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("POSTs the create body, reads the uuid from the response, and GETs the new device")
        void createsDevice() throws Exception {
            // POST /ne/v1/devices -> 201 Created with a body carrying the new device uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/devices/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            // GET /ne/v1/devices/{uuid} -> the impl re-fetches the created device.
            stubSingleton(wireMock, "/ne/v1/devices/" + NEW_UUID, "/json/networkedge/device_response.json");

            // DeviceVendorConfig is a read-model (@Getter only), so build the spec-shaped
            // VendorConfig via Jackson — which also locks its wire property names.
            DeviceVendorConfig vendorConfig = Constants.mapper().readValue(
                    "{\"siteId\":\"567\",\"systemIpAddress\":\"192.168.7.100\",\"adminPassword\":\"srb@dm1n\"}",
                    DeviceVendorConfig.class);

            Device device = networkEdge.devices()
                    .define("My-CSR1000V-Device")
                    .withDeviceTypeCode("CSR1000V")
                    .withMetroCode(MetroCode.SV)
                    .withAccountNumber("123456")
                    .withNotification("ops@example.com")
                    .withVendorConfig(vendorConfig)
                    .create();

            assertNotNull(device);
            assertEquals(NEW_UUID, device.getUuid());

            // Verify the outgoing create request body (string fields kept to avoid enum-serialization ambiguity).
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/devices/?"))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceName", equalTo("My-CSR1000V-Device")))
                    .withRequestBody(matchingJsonPath("$.deviceTypeCode", equalTo("CSR1000V")))
                    .withRequestBody(matchingJsonPath("$.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.accountNumber", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.notifications[0]", equalTo("ops@example.com")))
                    // Spec VirtualDeviceRequest.vendorConfig — the create body must carry the nested object.
                    .withRequestBody(matchingJsonPath("$.vendorConfig.siteId", equalTo("567")))
                    .withRequestBody(matchingJsonPath("$.vendorConfig.systemIpAddress", equalTo("192.168.7.100")))
                    .withRequestBody(matchingJsonPath("$.vendorConfig.adminPassword", equalTo("srb@dm1n"))));
            // Verify the follow-up GET for the uuid parsed from the create response body.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + NEW_UUID)));
        }

        @Test
        @DisplayName("defineSecondary() POSTs a body carrying the primaryDeviceUuid then GETs the new device")
        void createsSecondaryDevice() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/devices/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            stubSingleton(wireMock, "/ne/v1/devices/" + NEW_UUID, "/json/networkedge/device_response.json");

            Device device = networkEdge.devices()
                    .defineSecondary("My-Secondary-Device", "aaaa1111-2222-3333-4444-555566667777")
                    .withMetroCode(MetroCode.DC)
                    .withNotification("ops@example.com")
                    .create();

            assertNotNull(device);
            assertEquals(NEW_UUID, device.getUuid());

            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/devices/?"))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceName", equalTo("My-Secondary-Device")))
                    .withRequestBody(matchingJsonPath("$.primaryDeviceUuid", equalTo("aaaa1111-2222-3333-4444-555566667777")))
                    .withRequestBody(matchingJsonPath("$.metroCode", equalTo("DC"))));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + NEW_UUID)));
        }

        @Test
        @DisplayName("withSystemIpAddress(IPAddress) POSTs a byte-identical create body to the String setter")
        void typedSystemIpMatchesStringPath() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/devices/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            stubSingleton(wireMock, "/ne/v1/devices/" + NEW_UUID, "/json/networkedge/device_response.json");

            // Same create issued twice: once via the String setter, once via the typed
            // IPAddress overload (which formats via IPAddress.toCidr()).
            networkEdge.devices()
                    .define("My-CSR1000V-Device")
                    .withDeviceTypeCode("CSR1000V")
                    .withMetroCode(MetroCode.SV)
                    .withSystemIpAddress("192.168.7.100")
                    .create();
            networkEdge.devices()
                    .define("My-CSR1000V-Device")
                    .withDeviceTypeCode("CSR1000V")
                    .withMetroCode(MetroCode.SV)
                    .withSystemIpAddress(IPAddress.parse("192.168.7.100"))
                    .create();

            var posts = wireMock.findAll(postRequestedFor(urlPathMatching("/ne/v1/devices/?")));
            assertEquals(2, posts.size());
            // The typed overload serializes byte-for-byte identically to the String path.
            assertEquals(posts.get(0).getBodyAsString(), posts.get(1).getBodyAsString());
            wireMock.verify(2, postRequestedFor(urlPathMatching("/ne/v1/devices/?"))
                    .withRequestBody(matchingJsonPath("$.systemIpAddress", equalTo("192.168.7.100"))));
        }

        @Test
        @DisplayName("defineSecondary() withSystemIpAddress(IPAddress) POSTs a byte-identical create body to the String setter")
        void typedSecondarySystemIpMatchesStringPath() {
            wireMock.stubFor(post(urlPathMatching("/ne/v1/devices/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            stubSingleton(wireMock, "/ne/v1/devices/" + NEW_UUID, "/json/networkedge/device_response.json");

            networkEdge.devices()
                    .defineSecondary("My-Secondary-Device", "aaaa1111-2222-3333-4444-555566667777")
                    .withMetroCode(MetroCode.DC)
                    .withSystemIpAddress("192.168.7.101")
                    .create();
            networkEdge.devices()
                    .defineSecondary("My-Secondary-Device", "aaaa1111-2222-3333-4444-555566667777")
                    .withMetroCode(MetroCode.DC)
                    .withSystemIpAddress(IPAddress.parse("192.168.7.101"))
                    .create();

            var posts = wireMock.findAll(postRequestedFor(urlPathMatching("/ne/v1/devices/?")));
            assertEquals(2, posts.size());
            assertEquals(posts.get(0).getBodyAsString(), posts.get(1).getBodyAsString());
            wireMock.verify(2, postRequestedFor(urlPathMatching("/ne/v1/devices/?"))
                    .withRequestBody(matchingJsonPath("$.systemIpAddress", equalTo("192.168.7.101"))));
        }
    }

    @Nested
    @DisplayName("update() / save()")
    class Update {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("PATCHes the merged update body then re-GETs the device")
        void updatesDevice() {
            // getByUuid() -> loads the existing device the updater is derived from.
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            // PATCH /ne/v1/devices/{uuid} -> 204 No Content (voidOp), then the impl re-GETs the device.
            wireMock.stubFor(patch(urlPathEqualTo("/ne/v1/devices/" + UUID))
                    .willReturn(aResponse().withStatus(204)));

            Device device = networkEdge.devices().getByUuid(UUID);
            Device updated = device.update()
                    .withDeviceName("Renamed-Device")
                    .addNotification("noc@example.com")
                    .save();

            assertNotNull(updated);
            assertEquals(UUID, updated.getUuid());

            // The updater is seeded from the existing json (virtualDeviceName aliases "name"), so the
            // PATCH body carries the caller's overrides.
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceName", equalTo("Renamed-Device")))
                    .withRequestBody(matchingJsonPath("$.notifications[?(@ == 'noc@example.com')]")));
            // A follow-up GET re-fetches the updated device.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID)));
        }
    }

    @Nested
    @DisplayName("updateAdditionalBandwidth() / updateLicenseToken()")
    class BandwidthAndLicense {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("PUTs additionalBandwidths body then re-GETs the device")
        void updatesAdditionalBandwidth() {
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            wireMock.stubFor(put(urlPathEqualTo("/ne/v1/devices/" + UUID + "/additionalBandwidths"))
                    .willReturn(aResponse().withStatus(204)));

            Device device = networkEdge.devices().getByUuid(UUID);
            Boolean result = device.updateAdditionalBandwidth(200);

            assertTrue(result);
            wireMock.verify(putRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/additionalBandwidths"))
                    .withRequestBody(matchingJsonPath("$.additionalBandwidth", equalTo("200"))));
            // updateAdditionalBandwidth re-fetches the device after the PUT.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID)));
        }

        @Test
        @DisplayName("PUTs licenseTokens body carrying the token")
        void updatesLicenseToken() {
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            // mapOp reads the response body into a String map, so return a flat JSON object.
            wireMock.stubFor(put(urlPathEqualTo("/ne/v1/devices/" + UUID + "/licenseTokens"))
                    .willReturn(okJson("{\"fileId\":\"lic-file-9999\"}")));

            DeviceWrapper device = (DeviceWrapper) networkEdge.devices().getByUuid(UUID);
            String fileId = device.updateLicenseToken("token-abc-123");

            assertEquals("lic-file-9999", fileId);
            wireMock.verify(putRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/licenseTokens"))
                    .withRequestBody(matchingJsonPath("$.token", equalTo("token-abc-123"))));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("DELETEs the device and returns true")
        void deletesDevice() {
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            wireMock.stubFor(delete(urlPathEqualTo("/ne/v1/devices/" + UUID))
                    .willReturn(aResponse().withStatus(204)));

            Device device = networkEdge.devices().getByUuid(UUID);
            Boolean result = device.delete();

            assertTrue(result);
            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID)));
        }
    }

    @Nested
    @DisplayName("Device actions: softReboot / ping / RMA / restore / imageDownload")
    class Actions {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("softReboot() POSTs to {uuid}/softReboot and returns true")
        void softReboots() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/" + UUID + "/softReboot"))
                    .willReturn(aResponse().withStatus(202)));

            Boolean result = networkEdge.devices().softReboot(UUID);

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/softReboot")));
        }

        @Test
        @DisplayName("ping() GETs {uuid}/ping and returns true")
        void pings() {
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/" + UUID + "/ping"))
                    .willReturn(aResponse().withStatus(200)));

            Device device = networkEdge.devices().getByUuid(UUID);
            Boolean result = device.ping();

            assertTrue(result);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/ping")));
        }

        @Test
        @DisplayName("createRMA() POSTs the RMA body to {uuid}/rma")
        void createsRma() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/" + UUID + "/rma"))
                    .willReturn(aResponse().withStatus(202)));

            DeviceRMARequest request = new DeviceRMARequest("17.09.01a")
                    .withLicenseFileId("329a0bcd-0b2f-4bc5-b875-b506aa4b9730")
                    .withVendorConfigValue("siteId", "567");

            Boolean result = networkEdge.devices().createRMA(UUID, request);

            assertTrue(result);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/rma"))
                    .withRequestBody(matchingJsonPath("$.version", equalTo("17.09.01a")))
                    .withRequestBody(matchingJsonPath("$.licenseFileId", equalTo("329a0bcd-0b2f-4bc5-b875-b506aa4b9730")))
                    .withRequestBody(matchingJsonPath("$.vendorConfig.siteId", equalTo("567"))));
        }

        @Test
        @DisplayName("restoreFromBackup(uuid, name) PATCHes devices/{backupUuid}/restore with the name body")
        void restoresFromBackupByUuid() {
            String backupUuid = "bkup-1111-2222-3333-444455556666";
            wireMock.stubFor(patch(urlPathEqualTo("/ne/v1/devices/" + backupUuid + "/restore"))
                    .willReturn(aResponse().withStatus(202)));

            stubSingleton(wireMock, "/ne/v1/devices/" + UUID, "/json/networkedge/device_response.json");
            DeviceWrapper device = (DeviceWrapper) networkEdge.devices().getByUuid(UUID);
            Boolean result = device.restoreFromBackup(backupUuid, "nightly-backup");

            assertTrue(result);
            // Per spec the {uuid} path segment is the BACKUP uuid; the body is the required name.
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/ne/v1/devices/" + backupUuid + "/restore"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("nightly-backup"))));
        }

        @Test
        @DisplayName("requestImageDownload() POSTs to {deviceType}/repositories/{version}/download and returns the link")
        void requestsImageDownload() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/CSR1000V/repositories/16.09.05/download"))
                    .willReturn(okJson(loadFixture("/json/networkedge/image_download_response.json"))));

            ImageDownload download = networkEdge.devices().requestImageDownload("CSR1000V", "16.09.05");

            assertNotNull(download);
            assertEquals("https://downloads.equinix.com/ne/CSR1000V/16.09.05/image.bin", download.getDownloadLink());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/CSR1000V/repositories/16.09.05/download")));
        }

        @Test
        @DisplayName("postLicenseFile() POSTs the file body to licenseFiles with metro/type/license query params and returns fileId")
        void postsLicenseFile() {
            // mapOp reads the response body into a String map, so return a flat JSON object.
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/licenseFiles"))
                    .willReturn(okJson("{\"fileId\":\"lic-file-1234\"}")));

            String fileId = networkEdge.devices()
                    .postLicenseFile(MetroCode.SV, "VSRX", LicenseType.SUB, "BASE64-LICENSE-CONTENTS");

            assertEquals("lic-file-1234", fileId);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/licenseFiles"))
                    .withQueryParam("metroCode", equalTo("SV"))
                    .withQueryParam("deviceTypeCode", equalTo("VSRX"))
                    .withRequestBody(matchingJsonPath("$.file", equalTo("BASE64-LICENSE-CONTENTS"))));
        }
    }

    @Nested
    @DisplayName("Device ACLs: get / add / update")
    class DeviceAcls {

        private static final String UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";

        @Test
        @DisplayName("addDeviceAcl() POSTs the aclDetails body then re-GETs the device ACL")
        void addsDeviceAcl() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .willReturn(aResponse().withStatus(202)));
            // addACL re-fetches via GET {uuid}/acl.
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID + "/acl",
                    "/json/networkedge/device_acl_response.json");

            DeviceACLRequest request = new DeviceACLRequest()
                    .withAcl(ACLInterfaceType.WAN, "be7ef79e-31e7-4769-be5b-e192496f48aa")
                    .withAcl(ACLInterfaceType.MGMT, "ce7ef79e-31e7-4769-be5b-e192496f48ab");

            DeviceACL acl = networkEdge.devices().addDeviceAcl(UUID, request);

            assertNotNull(acl);
            assertEquals("be7ef79e-31e7-4769-be5b-e192496f48aa", acl.getAclTemplate().getUuid());
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].interfaceType", equalTo("WAN")))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].uuid", equalTo("be7ef79e-31e7-4769-be5b-e192496f48aa")))
                    .withRequestBody(matchingJsonPath("$.aclDetails[1].interfaceType", equalTo("MGMT")))
                    .withRequestBody(matchingJsonPath("$.aclDetails[1].uuid", equalTo("ce7ef79e-31e7-4769-be5b-e192496f48ab"))));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl")));
        }

        @Test
        @DisplayName("addDeviceAcl(accountUcmId) sends the accountUcmId query param")
        void addsDeviceAclForAccount() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .willReturn(aResponse().withStatus(202)));
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID + "/acl",
                    "/json/networkedge/device_acl_response.json");

            DeviceACLRequest request = new DeviceACLRequest()
                    .withAcl(ACLInterfaceType.WAN, "be7ef79e-31e7-4769-be5b-e192496f48aa");

            DeviceACL acl = networkEdge.devices().addDeviceAcl(UUID, request, "ucm-account-42");

            assertNotNull(acl);
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .withQueryParam("accountUcmId", equalTo("ucm-account-42"))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].interfaceType", equalTo("WAN"))));
        }

        @Test
        @DisplayName("updateDeviceAcl() PATCHes the aclDetails body then re-GETs the device ACL")
        void updatesDeviceAcl() {
            wireMock.stubFor(patch(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .willReturn(aResponse().withStatus(202)));
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID + "/acl",
                    "/json/networkedge/device_acl_response.json");

            DeviceACLRequest request = new DeviceACLRequest()
                    .withAcl(ACLInterfaceType.WAN, "be7ef79e-31e7-4769-be5b-e192496f48aa");

            DeviceACL acl = networkEdge.devices().updateDeviceAcl(UUID, request);

            assertNotNull(acl);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].interfaceType", equalTo("WAN")))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].uuid", equalTo("be7ef79e-31e7-4769-be5b-e192496f48aa"))));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl")));
        }

        @Test
        @DisplayName("updateDeviceAcl(accountUcmId) sends the accountUcmId query param")
        void updatesDeviceAclForAccount() {
            wireMock.stubFor(patch(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .willReturn(aResponse().withStatus(202)));
            stubSingleton(wireMock, "/ne/v1/devices/" + UUID + "/acl",
                    "/json/networkedge/device_acl_response.json");

            DeviceACLRequest request = new DeviceACLRequest()
                    .withAcl(ACLInterfaceType.MGMT, "ce7ef79e-31e7-4769-be5b-e192496f48ab");

            DeviceACL acl = networkEdge.devices().updateDeviceAcl(UUID, request, "ucm-account-42");

            assertNotNull(acl);
            wireMock.verify(patchRequestedFor(urlPathEqualTo("/ne/v1/devices/" + UUID + "/acl"))
                    .withQueryParam("accountUcmId", equalTo("ucm-account-42"))
                    .withRequestBody(matchingJsonPath("$.aclDetails[0].interfaceType", equalTo("MGMT"))));
        }
    }
}
