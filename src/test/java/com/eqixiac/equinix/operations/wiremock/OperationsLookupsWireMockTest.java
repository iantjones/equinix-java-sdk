package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.model.ConnectionService;
import com.eqixiac.equinix.operations.model.LookupLocation;
import com.eqixiac.equinix.operations.model.PatchPanel;
import com.eqixiac.equinix.operations.model.Provider;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the Operations colocation lookup (lookup v2) client, focused on
 * the optional filter query params on {@code listLocations} ({@code ibxs}, {@code providerAccountNumber},
 * {@code aSideIbx}, {@code connectionService}, {@code details}).
 */
class OperationsLookupsWireMockTest extends WireMockTestBase {

    static Operations operations;

    @BeforeAll
    static void setUp() {
        operations = new Operations(testCredentials());
        redirectToWireMock(operations);
        operations.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (operations != null) operations.close();
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

        List<? extends LookupLocation> locations = operations.lookups().listLocations("CROSS_CONNECT");

        assertNotNull(locations);
        assertEquals(1, locations.size());
        assertEquals("LD5", locations.get(0).getIbx());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/locations"))
                .withQueryParam("permissionCode", equalTo("CROSS_CONNECT"))
                .withQueryParam("ibxs", absent())
                .withQueryParam("details", absent()));
    }

    @Test
    @DisplayName("listLocations reads the live per-permission keys workVisits / shipments")
    void listLocations_readsPerPermissionKeys() {
        // Observed live 2026-11-11: the spec documents only crossConnects.
        wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                .withQueryParam("permissionCode", equalTo("SHIPMENTS"))
                .willReturn(okJson("{\"shipments\":[{\"ibx\":\"DC2\"}]}")));
        wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                .withQueryParam("permissionCode", equalTo("WORK_VISIT"))
                .willReturn(okJson("{\"workVisits\":[{\"ibx\":\"SV1\"},{\"ibx\":\"SV5\"}]}")));

        assertEquals("DC2", operations.lookups().listLocations("SHIPMENTS").get(0).getIbx());
        assertEquals(2, operations.lookups().listLocations("WORK_VISIT").size());
    }

    @Test
    @DisplayName("listLocations with optional filters forwards all supplied query params")
    void listLocations_withFilters() {
        wireMock.stubFor(get(urlPathEqualTo("/colocations/v2/locations"))
                .willReturn(okJson("{\"crossConnects\":[{\"ibx\":\"LD5\"}]}")));

        operations.lookups().listLocations("CROSS_CONNECT", List.of("LD5"),
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
                    operations.lookups().listPatchPanels("cabinet-123");

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

            PatchPanel panel = operations.lookups().getPatchPanelById("PP-99");

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
                    operations.lookups().listProviders("cage-1", "10000001");

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
                    .willReturn(okJson("[{\"name\":\"SINGLE_MODE_FIBER\"}]")));

            List<? extends ConnectionService> services =
                    operations.lookups().listConnectionServices("LD5");

            assertNotNull(services);
            assertEquals(1, services.size());
            assertEquals(com.eqixiac.equinix.operations.enums.ConnectionService.SINGLE_MODE_FIBER,
                    services.get(0).getName());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/colocations/v2/connectionServices"))
                    .withQueryParam("ibx", equalTo("LD5")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on getPatchPanelById() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/colocations/v2/patchPanels/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Patch panel not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> operations.lookups().getPatchPanelById("missing-panel"));
        }

        @Test
        @DisplayName("401 on listLocations() throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/colocations/v2/locations",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> operations.lookups().listLocations("CROSS_CONNECT"));
        }

        @Test
        @DisplayName("500 on listProviders() throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/colocations/v2/providers",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> operations.lookups().listProviders("cage-1", "10000001"));
        }
    }
}
