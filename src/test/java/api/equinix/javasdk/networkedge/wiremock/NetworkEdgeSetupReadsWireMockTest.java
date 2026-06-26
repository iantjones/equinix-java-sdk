package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.enums.DeviceCategory;
import api.equinix.javasdk.networkedge.enums.Vendor;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.Metro;
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
}
