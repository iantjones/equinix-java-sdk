package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Resellers.
 *
 * <p>The Resellers client exposes single-resource retrieval through
 * {@code getResellerCustomer(accountNumber, customerAccountNumber)} rather than a
 * plain {@code getByUuid(uuid)}; these tests exercise that accessor.</p>
 */
class CustomerPortalResellersWireMockTest extends WireMockTestBase {

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
    @DisplayName("getResellerCustomer()")
    class GetResellerCustomer {

        @Test
        @DisplayName("returns reseller customer for valid account numbers")
        void returnsResellerCustomer() {
            stubSingleton(wireMock, "/v2/resellers/.*",
                    "/json/customerportal/reseller_customer_response.json");

            ResellerCustomer customer = customerPortal.resellers()
                    .getResellerCustomer("256891", "778231");
            assertNotNull(customer);
            assertEquals("778231", customer.getCustomerAccountNumber());
            assertEquals("Northwind Trading Co.", customer.getCustomerAccountName());
            assertEquals("256891", customer.getAccountNumber());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/resellers/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Reseller customer not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.resellers().getResellerCustomer("256891", "invalid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/resellers/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.resellers().getResellerCustomer("256891", "778231"));
        }
    }
}
