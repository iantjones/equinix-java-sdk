package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.Quote;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Quotes.
 */
class CustomerPortalQuotesWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns quote for valid UUID")
        void returnsQuote() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/customerportal/quote_response.json");

            Quote quote = customerPortal.quotes().getByUuid("d0e1f2a3-b4c5-4d6e-7f80-910213243546");
            assertNotNull(quote);
            assertEquals("d0e1f2a3-b4c5-4d6e-7f80-910213243546", quote.getUuid());
            assertEquals("QT-2024-0019873", quote.getQuoteNumber());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/quotes/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Quote not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.quotes().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/quotes/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.quotes().getByUuid("test-uuid"));
        }
    }
}
