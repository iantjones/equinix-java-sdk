package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.enums.SmartHandsStatus;
import api.equinix.javasdk.customerportal.enums.SmartHandsType;
import api.equinix.javasdk.customerportal.model.SmartHands;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Smart Hands requests.
 *
 * <p>Exercises the single-resource accessor {@code smartHandsRequests().getByUuid(uuid)}.
 * The SmartHands functional area is keyed to API version 1 ({@code /v1/smartHands/...}).</p>
 */
class CustomerPortalSmartHandsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns smart hands request for valid UUID")
        void returnsSmartHands() {
            stubSingleton(wireMock, "/v1/smartHands/.*",
                    "/json/customerportal/smart_hands_response.json");

            SmartHands smartHands = customerPortal.smartHandsRequests()
                    .getByUuid("e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091");

            assertNotNull(smartHands);
            assertEquals("e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091", smartHands.getUuid());
            assertEquals("SH-2024-0008923", smartHands.getRequestId());
            assertEquals(SmartHandsType.STANDARD, smartHands.getType());
            assertEquals(SmartHandsStatus.IN_PROGRESS, smartHands.getStatus());
            assertEquals("SV5", smartHands.getIbxCode());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/smartHands/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Smart Hands request not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.smartHandsRequests().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/smartHands/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.smartHandsRequests().getByUuid("test-uuid"));
        }
    }
}
