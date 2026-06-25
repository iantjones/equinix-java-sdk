package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Cross Connects.
 */
class CustomerPortalCrossConnectsWireMockTest extends WireMockTestBase {

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

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns cross connect for valid UUID")
        void returnsCrossConnect() {
            stubSingleton(wireMock, "/v2/crossConnects/.*",
                    "/json/customerportal/cross_connect_response.json");

            CrossConnect cc = customerPortal.crossConnects().getByUuid("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
            assertNotNull(cc);
            assertEquals("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d", cc.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/crossConnects/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Cross connect not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.crossConnects().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/v2/crossConnects/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.crossConnects().getByUuid("test-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/crossConnects/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.crossConnects().getByUuid("test-uuid"));
        }
    }
}
