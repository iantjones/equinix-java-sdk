package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Gateway Packages
 * (read-only list at the overridden root URI /fabric/v4/gatewayPackages).
 */
class FabricGatewayPackagesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("gatewayPackages()")
    class ListGatewayPackages {

        @Test
        @DisplayName("returns a paginated list of gateway packages")
        void returnsGatewayPackages() {
            stubPaginatedGet(wireMock, "/fabric/v4/gatewayPackages",
                    "/json/fabric/paginated_gateway_packages.json");

            PaginatedList<GatewayPackage> packages = fabric.fabricGateways().gatewayPackages();

            assertNotNull(packages);
            assertEquals(2, packages.size());
            GatewayPackage first = packages.get(0);
            assertEquals(GatewayPackageCode.LIMITED, first.getCode());
            assertEquals(1000, first.getTotalIPv4RoutesMax());
            assertFalse(first.getHaSupported());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/gatewayPackages")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/gatewayPackages",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.fabricGateways().gatewayPackages());
        }
    }
}
