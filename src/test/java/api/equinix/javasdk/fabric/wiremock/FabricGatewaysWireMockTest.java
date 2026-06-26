package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.FabricGateway;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Gateways.
 */
class FabricGatewaysWireMockTest extends WireMockTestBase {

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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns fabric gateway for valid UUID")
        void returnsGateway() {
            stubSingleton(wireMock, "/fabric/v4/gateways/.*",
                    "/json/fabric/fabric_gateway_response.json");

            FabricGateway gateway = fabric.fabricGateways().getByUuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            assertNotNull(gateway);
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", gateway.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/gateways/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Gateway not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.fabricGateways().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/gateways/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.fabricGateways().getByUuid("test-uuid"));
        }
    }
}
