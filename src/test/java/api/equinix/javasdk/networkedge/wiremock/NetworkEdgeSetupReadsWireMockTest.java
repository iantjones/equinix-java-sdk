package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.FileProcessType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.implementation.AgreementStatus;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge read-only setup resources:
 * Accounts (setup().listAccounts), Metros (setup().listMetros) and
 * DeviceTypes (devices().listDeviceTypes).
 *
 * <p>These resources have no getByUuid(String); the simplest read operation
 * each exposes is a list, so each list is exercised plus a 500 error case.</p>
 */
class NetworkEdgeSetupReadsWireMockTest extends WireMockTestBase {

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
    @DisplayName("setup().listAccounts(metroCode)")
    class ListAccounts {

        @Test
        @DisplayName("returns accounts for a metro")
        void returnsAccounts() {
            // ListAccounts -> GET /ne/v1/accounts/{metroCode}
            stubSingleton(wireMock, "/ne/v1/accounts/SV",
                    "/json/networkedge/account_list_response.json");

            List<Account> accounts = networkEdge.setup().listAccounts(MetroCode.SV);

            assertNotNull(accounts);
            assertEquals(2, accounts.size());
            Account first = accounts.get(0);
            assertEquals("Acme Corp", first.getAccountName());
            assertEquals(123456, first.getAccountNumber());
            assertEquals("Active", first.getAccountStatus());
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/accounts/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.setup().listAccounts(MetroCode.SV));
        }
    }

    @Nested
    @DisplayName("setup().listMetros()")
    class ListMetros {

        @Test
        @DisplayName("returns metros where Network Edge is present")
        void returnsMetros() {
            // ListMetros -> GET /ne/v1/metros
            stubPaginatedGet(wireMock, "/ne/v1/metros",
                    "/json/networkedge/metro_list_response.json");

            PaginatedList<Metro> metros = networkEdge.setup().listMetros();

            assertNotNull(metros);
            assertEquals(2, metros.size());
            Metro first = metros.get(0);
            assertEquals(MetroCode.SV, first.getMetroCode());
            assertEquals(Region.AMER, first.getRegion());
            assertEquals("Silicon Valley", first.getMetroDescription());
            assertTrue(first.getClusterSupported());
        }

        @Test
        @DisplayName("listMetrosByRegion(region) sends the region filter and returns metros")
        void listMetrosByRegion() {
            // Regression guard: the region-filtered path used to pass an immutable Map.of("region",...)
            // as the query parameters, which pagination then mutated -> UnsupportedOperationException.
            // EquinixRequest.setQueryParameters now defensively copies into a mutable map.
            stubPaginatedGet(wireMock, "/ne/v1/metros",
                    "/json/networkedge/metro_list_response.json");

            var metros = networkEdge.setup().listMetrosByRegion(Region.AMER);
            assertNotNull(metros);
            assertEquals(2, metros.size());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/metros"))
                    .withQueryParam("region", equalTo("AMER")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/metros",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.setup().listMetros());
        }
    }

    @Nested
    @DisplayName("devices().listDeviceTypes()")
    class ListDeviceTypes {

        @Test
        @DisplayName("returns available device types")
        void returnsDeviceTypes() {
            // ListDeviceTypes -> GET /ne/v1/deviceTypes (overrideRootUri)
            stubPaginatedGet(wireMock, "/ne/v1/deviceTypes",
                    "/json/networkedge/device_type_list_response.json");

            PaginatedList<DeviceType> deviceTypes = networkEdge.devices().listDeviceTypes();

            assertNotNull(deviceTypes);
            assertEquals(2, deviceTypes.size());
            DeviceType first = deviceTypes.get(0);
            assertEquals("CSR1000V", first.getDeviceTypeCode());
            assertEquals("Cisco CSR 1000V", first.getName());
            assertEquals(Vendor.CISCO, first.getVendor());
            assertEquals(DeviceCategory.ROUTER, first.getCategory());
            assertEquals(10, first.getMaxInterfaceCount());
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/deviceTypes",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.devices().listDeviceTypes());
        }
    }

    @Nested
    @DisplayName("setup().createAgreement(accountNumber, termsVersionId)")
    class CreateAgreement {

        @Test
        @DisplayName("POSTs the accountNumber/apttusId body then re-fetches the status")
        void createsAgreement() {
            // CreateAgreement -> POST /ne/v1/agreements/accounts {accountNumber, apttusId}
            // The impl then re-fetches via GetAgreementStatus -> GET /ne/v1/agreements/accounts?accountNumber=..
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/agreements/accounts"))
                    .willReturn(okJson(loadFixture("/json/networkedge/agreement_status_response.json"))));
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/agreements/accounts"))
                    .willReturn(okJson(loadFixture("/json/networkedge/agreement_status_response.json"))));

            AgreementStatus status = networkEdge.setup().createAgreement("123456", "a1b2c3d4-e5f6-7890-abcd-ef1234567890");

            assertNotNull(status);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", status.getTermsVersionId());
            assertTrue(status.getValid());

            // The create body carries the account number and the terms version id (serialized as apttusId).
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/agreements/accounts"))
                    .withRequestBody(equalToJson("{\"accountNumber\":\"123456\",\"apttusId\":\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"}", true, true)));
            // And the follow-up read filters by the same account number.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/agreements/accounts"))
                    .withQueryParam("accountNumber", equalTo("123456")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/agreements/accounts",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.setup().createAgreement("123456", "terms-1"));
        }
    }

    @Nested
    @DisplayName("setup().getOrderSummary(requestBuilder)")
    class GetOrderSummary {

        @Test
        @DisplayName("GETs /orderSummaries with the built query parameters and returns the document bytes")
        void returnsOrderSummary() {
            // GetOrderSummary -> GET /ne/v1/orderSummaries (overrideRootUri) returning a binary document.
            byte[] pdfBytes = "%PDF-1.4 order-summary".getBytes();
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/orderSummaries"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/pdf")
                            .withBody(pdfBytes)));

            RequestBuilder.OrderSummary request = RequestBuilder.orderSummary()
                    .withAccountNumber(123456)
                    .withMetro(MetroCode.SV)
                    .withVendorPackage("STD")
                    .withLicenseType(LicenseType.SUB)
                    .withThroughput(500)
                    .withThroughputUnit(api.equinix.javasdk.core.enums.BandwidthUnit.MBPS)
                    .withTermLength(12)
                    .withCore(2)
                    .withDeviceManagementType(DeviceManagementType.SELF_CONFIGURED);
            request.build();

            byte[] summary = networkEdge.setup().getOrderSummary(request);

            assertNotNull(summary);
            assertArrayEquals(pdfBytes, summary);

            // licenseType uses the query form (Subscription), deviceManagementType uses the hyphenated form.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/orderSummaries"))
                    .withQueryParam("accountNumber", equalTo("123456"))
                    .withQueryParam("metro", equalTo("SV"))
                    .withQueryParam("vendorPackage", equalTo("STD"))
                    .withQueryParam("licenseType", equalTo("Subscription"))
                    .withQueryParam("throughput", equalTo("500"))
                    .withQueryParam("termLength", equalTo("12"))
                    .withQueryParam("core", equalTo("2"))
                    .withQueryParam("deviceManagementType", equalTo("SELF-CONFIGURED")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/orderSummaries",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            RequestBuilder.OrderSummary request = RequestBuilder.orderSummary().withMetro(MetroCode.SV);
            request.build();

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.setup().getOrderSummary(request));
        }
    }

    @Nested
    @DisplayName("setup().uploadFile(...)")
    class UploadFile {

        // NOTE: The upload endpoint is a plain JSON POST in this SDK (the file contents are carried
        // as the "file" body field), not a multipart/form-data submission, so the serialized body is
        // fully assertable here.

        @Test
        @DisplayName("POSTs /files with the metro/deviceType/process/management/license/file body and returns fileUuid")
        void uploadsFile() {
            // UploadFile -> POST /ne/v1/files returning {fileUuid}.
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/files"))
                    .willReturn(okJson(loadFixture("/json/networkedge/file_upload_response.json"))));

            String fileUuid = networkEdge.setup().uploadFile(
                    MetroCode.SV,
                    "C8000V",
                    FileProcessType.LICENSE,
                    DeviceManagementType.SELF_CONFIGURED,
                    LicenseType.SUB,
                    "license-file-contents");

            assertEquals("f1e2d3c4-b5a6-7890-abcd-1234567890ff", fileUuid);

            // metroCode/processType/licenseType serialize by enum name; deviceManagementType uses its @JsonValue form.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/files"))
                    .withRequestBody(equalToJson(
                            "{" +
                                    "\"metroCode\":\"SV\"," +
                                    "\"deviceTypeCode\":\"C8000V\"," +
                                    "\"processType\":\"LICENSE\"," +
                                    "\"deviceManagementType\":\"SELF-CONFIGURED\"," +
                                    "\"licenseType\":\"SUB\"," +
                                    "\"file\":\"license-file-contents\"" +
                                    "}", true, true)));
        }

        @Test
        @DisplayName("omits null optional fields from the request body")
        void omitsNullOptionalFields() {
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/files"))
                    .willReturn(okJson(loadFixture("/json/networkedge/file_upload_response.json"))));

            String fileUuid = networkEdge.setup().uploadFile(
                    MetroCode.SV,
                    "C8000V",
                    FileProcessType.CLOUD_INIT,
                    null,
                    null,
                    "cloud-init-contents");

            assertEquals("f1e2d3c4-b5a6-7890-abcd-1234567890ff", fileUuid);

            // deviceManagementType/licenseType were null and must not appear in the serialized body.
            // equalToJson with ignoreExtraElements=false (2nd flag) proves the exact key set.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/files"))
                    .withRequestBody(equalToJson(
                            "{" +
                                    "\"metroCode\":\"SV\"," +
                                    "\"deviceTypeCode\":\"C8000V\"," +
                                    "\"processType\":\"CLOUD_INIT\"," +
                                    "\"file\":\"cloud-init-contents\"" +
                                    "}", true, false)));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/files",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.setup().uploadFile(MetroCode.SV, "C8000V",
                            FileProcessType.LICENSE, null, null, "contents"));
        }
    }
}
