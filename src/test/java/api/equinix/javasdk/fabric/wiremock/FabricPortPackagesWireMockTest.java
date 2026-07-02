package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.fabric.enums.PortPackageType;
import api.equinix.javasdk.fabric.enums.PortServiceType;
import api.equinix.javasdk.fabric.model.PortPackage;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Port Packages (read-only: list).
 *
 * <p>The list op is {@code GetPortPackages} with {@code rootUri: "portPackages"} and no
 * {@code requestUri}/{@code overrideRootUri}, so it resolves to {@code GET /fabric/v4/portPackages}
 * and returns a non-paginated {@code { "data": [...] }} envelope.
 */
class FabricPortPackagesWireMockTest extends WireMockTestBase {

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
    @DisplayName("list()")
    class ListPortPackages {

        @Test
        @DisplayName("GETs /fabric/v4/portPackages and returns the port packages")
        void returnsPortPackages() {
            stubPaginatedGet(wireMock, "/fabric/v4/portPackages", "/json/fabric/port_packages_response.json");

            List<PortPackage> packages = fabric.portPackages().list();

            assertNotNull(packages);
            assertEquals(2, packages.size());

            PortPackage first = packages.get(0);
            assertEquals("STANDARD", first.getCode());
            assertEquals(PortPackageType.PORT_PACKAGE, first.getType());
            assertEquals(Integer.valueOf(10000), first.getVcBandwidthMax());
            assertTrue(first.getVcRemoteSupported());
            assertEquals(List.of(PortServiceType.MSP), first.getSupportedServiceTypes());

            assertEquals("UNTAGGED", packages.get(1).getCode());

            // overrideRootUri is NOT set: path is the plain rootUri, not nested under a parent.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/fabric/v4/portPackages")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/portPackages.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class, () -> fabric.portPackages().list());
        }
    }
}
