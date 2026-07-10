package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.EquinixConfig;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.networkedge.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline (WireMock) scenario for the multi-domain session guarantee: one {@link Equinix}
 * session serves Fabric, NetworkEdge, and InternetAccess in a single flow over the SHARED core —
 * exactly one OAuth token is fetched, and every domain call rides that same bearer token.
 *
 * <p>{@code EquinixSessionWireMockTest} proves token sharing across repeated calls within one
 * domain; this scenario proves it across three different domains, which is the seam a real
 * multi-product user exercises (inventory sweep: connections + devices + EIA availability).</p>
 */
@DisplayName("Equinix session — three domains, one shared core, one OAuth token")
class EquinixSessionMultiDomainScenarioTest extends WireMockTestBase {

    private static final String BEARER = "Bearer test-token-abc123";

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("fabric + networkEdge + internetAccess calls share a single token on the wire")
    void oneTokenServesThreeDomains() throws Exception {
        stubPaginatedPost(wireMock, "/fabric/v4/connections/search",
                "/json/fabric/paginated_connections.json");
        stubPaginatedGet(wireMock, "/ne/v1/devices/?",
                "/json/networkedge/device_list_response.json");
        stubPaginatedGet(wireMock, "/internetAccess/v2/ibxs",
                "/json/internetaccess/paginated_ibxs.json");

        try (Equinix eq = new Equinix(testCredentials(),
                EquinixConfig.builder().autoLoadMetros(false).build())) {
            // Redirecting any session-obtained domain redirects the whole shared core.
            redirectToWireMock(eq.fabric());
            eq.authenticate();

            // One inventory sweep across three product domains.
            PaginatedFilteredList<Connection> connections = eq.fabric().connections().search();
            PaginatedList<Device> devices = eq.networkEdge().devices().list();
            PaginatedList<Ibx> eiaIbxs = eq.internetAccess().ibxs().availability(ConnectionType.IA_VC);

            assertEquals(2, connections.size(), "Fabric connections over the shared core");
            assertEquals(2, devices.size(), "NetworkEdge devices over the shared core");
            assertEquals(3, eiaIbxs.size(), "EIA IBX availability over the shared core");

            // The whole three-domain flow fetched exactly ONE OAuth token...
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));

            // ...and each domain's request carried that same token.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                    .withHeader("Authorization", equalTo(BEARER)));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices"))
                    .withHeader("Authorization", equalTo(BEARER)));
            wireMock.verify(getRequestedFor(urlPathEqualTo("/internetAccess/v2/ibxs"))
                    .withHeader("Authorization", equalTo(BEARER))
                    .withQueryParam("service.connection.type", equalTo("IA_VC")));
        }
    }
}
