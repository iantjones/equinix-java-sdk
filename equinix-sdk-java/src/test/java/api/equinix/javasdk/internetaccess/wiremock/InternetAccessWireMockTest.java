package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.model.json.creators.BgpRoutingProtocolRequest;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the Internet Access (EIA) v2 domain. EIA v2 exposes a single
 * operation — {@code POST /internetAccess/v2/services} — so these cover error mapping on that
 * create endpoint.
 */
class InternetAccessWireMockTest extends WireMockTestBase {

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private void createBgpService() {
        internetAccess.services().define()
                .name("WebServers")
                .type(ServiceTypeV2.SINGLE)
                .connection("9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81")
                .routingProtocol(BgpRoutingProtocolRequest.builder()
                        .customerAsn(16220L)
                        .exportPolicy(ExportPolicy.FULL)
                        .build())
                .create();
    }

    @Nested
    @DisplayName("Services - Error handling")
    class ServicesErrors {

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, "/internetAccess/v2/services",
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid request\"}]");

            assertThrows(EquinixServiceException.class, InternetAccessWireMockTest.this::createBgpService);
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/internetAccess/v2/services",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class, InternetAccessWireMockTest.this::createBgpService);
        }
    }
}
