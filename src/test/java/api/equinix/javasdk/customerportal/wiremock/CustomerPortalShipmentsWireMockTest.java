package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.Shipment;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Shipments.
 */
class CustomerPortalShipmentsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns shipment for valid UUID")
        void returnsShipment() {
            stubSingleton(wireMock, "/v2/shipments/.*",
                    "/json/customerportal/shipment_response.json");

            Shipment shipment = customerPortal.shipments().getByUuid("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f809102");
            assertNotNull(shipment);
            assertEquals("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f809102", shipment.getUuid());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/shipments/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Shipment not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.shipments().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/shipments/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.shipments().getByUuid("test-uuid"));
        }
    }
}
