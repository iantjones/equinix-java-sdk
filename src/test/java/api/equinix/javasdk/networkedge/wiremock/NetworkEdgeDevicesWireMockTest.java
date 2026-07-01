package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.enums.ACLInterfaceType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.implementation.DeviceACL;
import api.equinix.javasdk.networkedge.model.implementation.ImageDownload;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceRMARequest;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceWrapper;
import org.junit.jupiter.api.*;

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
        void createsDevice() {
            // POST /ne/v1/devices -> 201 Created with a body carrying the new device uuid.
            wireMock.stubFor(post(urlPathMatching("/ne/v1/devices/?"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"uuid\":\"" + NEW_UUID + "\"}")));
            // GET /ne/v1/devices/{uuid} -> the impl re-fetches the created device.
            stubSingleton(wireMock, "/ne/v1/devices/" + NEW_UUID, "/json/networkedge/device_response.json");

            Device device = networkEdge.devices()
                    .define("My-CSR1000V-Device")
                    .withDeviceTypeCode("CSR1000V")
                    .withMetroCode(MetroCode.SV)
                    .withAccountNumber("123456")
                    .withNotification("ops@example.com")
                    .create();

            assertNotNull(device);
            assertEquals(NEW_UUID, device.getUuid());

            // Verify the outgoing create request body (string fields kept to avoid enum-serialization ambiguity).
            wireMock.verify(postRequestedFor(urlPathMatching("/ne/v1/devices/?"))
                    .withRequestBody(matchingJsonPath("$.virtualDeviceName", equalTo("My-CSR1000V-Device")))
                    .withRequestBody(matchingJsonPath("$.deviceTypeCode", equalTo("CSR1000V")))
                    .withRequestBody(matchingJsonPath("$.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.accountNumber", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.notifications[0]", equalTo("ops@example.com"))));
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
