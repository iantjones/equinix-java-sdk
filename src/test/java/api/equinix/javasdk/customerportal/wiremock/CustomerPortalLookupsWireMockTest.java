package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.ConnectionService;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.PatchPanel;
import api.equinix.javasdk.customerportal.model.Provider;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal colocation lookup (lookup v2) client, focused on
 * the optional filter query params on {@code listLocations} ({@code ibxs}, {@code providerAccountNumber},
 * {@code aSideIbx}, {@code connectionService}, {@code details}).
 */
class CustomerPortalLookupsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    @BeforeAll
    static void setUp() {
        customerPortal = new CustomerPortal(testCredentials());
        redirectToWireMock(customerPortal);
        customerPortal.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (customerPortal != null) customerPortal.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("listLocations(permissionCode) forwards only permissionCode")
    void listLocations_permissionCodeOnly() {
        wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                .willReturn(okJson("{\"crossConnects\":[{\"ibx\":\"LD5\"}]}")));

        List<? extends LookupLocation> locations = customerPortal.lookups().listLocations("CROSS_CONNECT");

        assertNotNull(locations);
        assertEquals(1, locations.size());
        assertEquals("LD5", locations.get(0).getIbx());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/locations"))
                .withQueryParam("permissionCode", equalTo("CROSS_CONNECT"))
                .withQueryParam("ibxs", absent())
                .withQueryParam("details", absent()));
    }

    @Test
    @DisplayName("listLocations with optional filters forwards all supplied query params")
    void listLocations_withFilters() {
        wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                .willReturn(okJson("{\"crossConnects\":[{\"ibx\":\"LD5\"}]}")));

        customerPortal.lookups().listLocations("CROSS_CONNECT", List.of("LD5"),
                "10000001", "LD5", "DOT1Q", true);

        wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/locations"))
                .withQueryParam("permissionCode", equalTo("CROSS_CONNECT"))
                .withQueryParam("ibxs", equalTo("LD5"))
                .withQueryParam("providerAccountNumber", equalTo("10000001"))
                .withQueryParam("aSideIbx", equalTo("LD5"))
                .withQueryParam("connectionService", equalTo("DOT1Q"))
                .withQueryParam("details", equalTo("true")));
    }

    @Nested
    @DisplayName("listPatchPanels")
    class ListPatchPanels {

        @Test
        @DisplayName("GET /colocations/v2/patchPanels with cabinetId query param")
        void listPatchPanels_forwardsCabinetId() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/patchPanels"))
                    .willReturn(okJson("[{\"patchPanelId\":\"PP-1\",\"availablePortCount\":8}]")));

            List<? extends PatchPanel> panels =
                    customerPortal.lookups().listPatchPanels("cabinet-123");

            assertNotNull(panels);
            assertEquals(1, panels.size());
            assertEquals("PP-1", panels.get(0).getPatchPanelId());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/patchPanels"))
                    .withQueryParam("cabinetId", equalTo("cabinet-123")));
        }
    }

    @Nested
    @DisplayName("getPatchPanelById")
    class GetPatchPanelById {

        @Test
        @DisplayName("GET /colocations/v2/patchPanels/{patchPanelId}")
        void getPatchPanelById_hitsDetailPath() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/patchPanels/PP-99"))
                    .willReturn(okJson("{\"patchPanelId\":\"PP-99\",\"ibx\":\"LD5\",\"cabinetId\":\"cabinet-123\"}")));

            PatchPanel panel = customerPortal.lookups().getPatchPanelById("PP-99");

            assertNotNull(panel);
            assertEquals("PP-99", panel.getPatchPanelId());
            assertEquals("LD5", panel.getIbx());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/patchPanels/PP-99")));
        }
    }

    @Nested
    @DisplayName("listProviders")
    class ListProviders {

        @Test
        @DisplayName("GET /colocations/v2/providers with cageId and accountNumber query params")
        void listProviders_forwardsCageIdAndAccountNumber() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/providers"))
                    .willReturn(okJson("[{\"providerAccountName\":\"Acme\","
                            + "\"providerAccountNumber\":\"10000001\"}]")));

            List<? extends Provider> providers =
                    customerPortal.lookups().listProviders("cage-1", "10000001");

            assertNotNull(providers);
            assertEquals(1, providers.size());
            assertEquals("10000001", providers.get(0).getProviderAccountNumber());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/providers"))
                    .withQueryParam("cageId", equalTo("cage-1"))
                    .withQueryParam("accountNumber", equalTo("10000001")));
        }
    }

    @Nested
    @DisplayName("listConnectionServices")
    class ListConnectionServices {

        @Test
        @DisplayName("GET /colocations/v2/connectionServices with ibx query param")
        void listConnectionServices_forwardsIbx() {
            wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/connectionServices"))
                    .willReturn(okJson("[{\"name\":\"DOT1Q\"}]")));

            List<? extends ConnectionService> services =
                    customerPortal.lookups().listConnectionServices("LD5");

            assertNotNull(services);
            assertEquals(1, services.size());
            assertEquals("DOT1Q", services.get(0).getName());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/connectionServices"))
                    .withQueryParam("ibx", equalTo("LD5")));
        }
    }
}
