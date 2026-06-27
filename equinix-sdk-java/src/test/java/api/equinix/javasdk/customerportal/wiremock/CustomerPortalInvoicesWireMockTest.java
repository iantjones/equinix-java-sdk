package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.enums.ActivityType;
import api.equinix.javasdk.customerportal.enums.Frequency;
import api.equinix.javasdk.customerportal.enums.TransactionType;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for CustomerPortal Invoices.
 *
 * <p>Exercises the paginated list accessors {@code invoices().summaries()} and
 * {@code invoices().details()}. The Invoices functional area uses the CustomerPortal
 * default API version 2 ({@code /v2/invoices} and {@code /v2/invoices/details}).</p>
 */
class CustomerPortalInvoicesWireMockTest extends WireMockTestBase {

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

    // Regression guard: summaries()/details() with no filter previously passed an immutable
    // Collections.emptyMap() as the query parameters, which pagination then mutated ->
    // UnsupportedOperationException. EquinixRequest.setQueryParameters now defensively copies into
    // a mutable map, so the no-filter paginated reads work.

    @Nested
    @DisplayName("summaries()")
    class Summaries {

        @Test
        @DisplayName("returns the invoice summary page")
        void summaries() {
            stubPaginatedGet(wireMock, "/v2/invoices",
                    "/json/customerportal/paginated_invoice_summaries.json");

            PaginatedList<InvoiceSummary> summaries = customerPortal.invoices().summaries();
            assertNotNull(summaries);
            assertEquals(1, summaries.size());
            assertEquals(TransactionType.INVOICE, summaries.get(0).getTransactionType());
        }
    }

    @Nested
    @DisplayName("details()")
    class Details {

        @Test
        @DisplayName("returns the invoice detail page")
        void details() {
            stubPaginatedGet(wireMock, "/v2/invoices/details",
                    "/json/customerportal/paginated_invoice_details.json");

            PaginatedList<InvoiceDetail> details = customerPortal.invoices().details();
            assertNotNull(details);
            assertEquals(1, details.size());
            assertEquals(ActivityType.RECURRING_CHARGE, details.get(0).getActivityType());
            assertEquals(Frequency.MONTHLY, details.get(0).getFrequency());
        }
    }
}
