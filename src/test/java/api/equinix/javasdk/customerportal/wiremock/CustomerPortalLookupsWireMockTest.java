package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.LookupLocation;
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
}
