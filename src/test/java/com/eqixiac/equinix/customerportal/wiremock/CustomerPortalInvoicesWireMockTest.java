package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.enums.ActivityType;
import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.customerportal.enums.Frequency;
import com.eqixiac.equinix.customerportal.enums.Region;
import com.eqixiac.equinix.customerportal.enums.SubChannel;
import com.eqixiac.equinix.customerportal.enums.TransactionType;
import com.eqixiac.equinix.customerportal.client.RequestBuilder;
import com.eqixiac.equinix.customerportal.model.InvoiceDetail;
import com.eqixiac.equinix.customerportal.model.InvoiceSummary;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
            InvoiceSummary summary = summaries.get(0);
            assertEquals(TransactionType.INVOICE, summary.getTransactionType());
            assertEquals("Equinix Inc.", summary.getBusinessLegalEntity());
            assertEquals(0, summary.getTotalPriorAdjustmentAmount().compareTo(new java.math.BigDecimal("-250.00")));
            assertNotNull(summary.getPriorAdjustmentInfo());
            assertEquals(1, summary.getPriorAdjustmentInfo().size());
            assertEquals("CM-2024-000451", summary.getPriorAdjustmentInfo().get(0).getTransactionId());
        }

        @Test
        @DisplayName("filtered summaries issue GET /v2/invoices with the builder query params")
        void filteredSummaries() {
            stubPaginatedGet(wireMock, "/v2/invoices",
                    "/json/customerportal/paginated_invoice_summaries.json");

            RequestBuilder.Invoice filter = RequestBuilder.Invoice.builder()
                    .withStart(LocalDate.of(2024, 1, 1))
                    .withEnd(LocalDate.of(2024, 12, 31))
                    .withAccountNumber("128745")
                    .withAccountNumber("128746")
                    .withTransactionId("INV-2024-00198734")
                    .build();

            PaginatedList<InvoiceSummary> summaries = customerPortal.invoices().summaries(filter);
            assertNotNull(summaries);
            assertEquals(1, summaries.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/invoices"))
                    .withQueryParam("startDate", equalTo("2024-01-01"))
                    .withQueryParam("endDate", equalTo("2024-12-31"))
                    .withQueryParam("accountNumbers", equalTo("128745"))
                    .withQueryParam("accountNumbers", equalTo("128746"))
                    .withQueryParam("transactionIds", equalTo("INV-2024-00198734")));
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
            InvoiceDetail detail = details.get(0);
            assertEquals(ActivityType.RECURRING_CHARGE, detail.getActivityType());
            assertEquals(Frequency.MONTHLY, detail.getFrequency());
            assertEquals(Region.AMER, detail.getRegion());
            assertEquals("US", detail.getCountryCode());
            assertEquals(Channel.PORTAL, detail.getChannel());
            assertEquals(SubChannel.ECP, detail.getSubChannel());
            assertEquals("Cabinet", detail.getProductName());
            assertEquals(LocalDate.of(2024, 10, 28), detail.getOrderBookedDate());
            assertEquals(LocalDate.of(2024, 11, 1), detail.getRecurringStartDate());
            assertEquals("CM-2024-000451", detail.getPriorAdjustmentReference());
            assertNotNull(detail.getTermsOfUse());
            assertEquals(1, detail.getTermsOfUse().size());
            assertEquals(36, detail.getTermsOfUse().get(0).getValue());
        }

        @Test
        @DisplayName("filtered details issue GET /v2/invoices/details with the builder query params")
        void filteredDetails() {
            stubPaginatedGet(wireMock, "/v2/invoices/details",
                    "/json/customerportal/paginated_invoice_details.json");

            RequestBuilder.Invoice filter = RequestBuilder.Invoice.builder()
                    .withStart(LocalDate.of(2024, 10, 1))
                    .withEnd(LocalDate.of(2024, 10, 31))
                    .withAccountNumber("128745")
                    .withTransactionId("INV-2024-00198734")
                    .withTransactionId("INV-2024-00198735")
                    .build();

            PaginatedList<InvoiceDetail> details = customerPortal.invoices().details(filter);
            assertNotNull(details);
            assertEquals(1, details.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/invoices/details"))
                    .withQueryParam("startDate", equalTo("2024-10-01"))
                    .withQueryParam("endDate", equalTo("2024-10-31"))
                    .withQueryParam("accountNumbers", equalTo("128745"))
                    .withQueryParam("transactionIds", equalTo("INV-2024-00198734"))
                    .withQueryParam("transactionIds", equalTo("INV-2024-00198735")));
        }
    }

    @Nested
    @DisplayName("Multi-page paging")
    class Paging {

        // Both invoice reads are plain paginated GETs: dispatch stamps offset=0/limit=100 onto
        // the first request, and page 2 is requested by advancing the offset/limit QUERY
        // PARAMETERS from the SERVER-reported pagination.
        private static final String SUMMARIES_PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "transactionId": "PAGE1_INV" } ]
                }
                """;

        private static final String SUMMARIES_PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "transactionId": "PAGE2_INV" } ]
                }
                """;

        private static final String DETAILS_PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "transactionId": "PAGE1_DET" } ]
                }
                """;

        private static final String DETAILS_PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "transactionId": "PAGE2_DET" } ]
                }
                """;

        @Test
        @DisplayName("summaries().loadAll() fetches page 2 by advancing the offset query param")
        void summariesLoadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/v2/invoices"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(SUMMARIES_PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/v2/invoices"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(SUMMARIES_PAGE_2)));

            PaginatedList<InvoiceSummary> summaries = customerPortal.invoices().summaries();
            assertEquals(1, summaries.size());
            assertTrue(summaries.hasNextPage());

            summaries.loadAll();

            assertEquals(2, summaries.size());
            assertEquals("PAGE1_INV", summaries.get(0).getTransactionId());
            assertEquals("PAGE2_INV", summaries.get(1).getTransactionId());
            assertFalse(summaries.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v2/invoices"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        @DisplayName("details().loadAll() fetches page 2 by advancing the offset query param")
        void detailsLoadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/v2/invoices/details"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(DETAILS_PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/v2/invoices/details"))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(DETAILS_PAGE_2)));

            PaginatedList<InvoiceDetail> details = customerPortal.invoices().details();
            assertEquals(1, details.size());
            assertTrue(details.hasNextPage());

            details.loadAll();

            assertEquals(2, details.size());
            assertEquals("PAGE1_DET", details.get(0).getTransactionId());
            assertEquals("PAGE2_DET", details.get(1).getTransactionId());
            assertFalse(details.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v2/invoices/details"))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("401 on summaries() throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/v2/invoices",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.invoices().summaries());
        }

        @Test
        @DisplayName("404 on summaries() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/invoices",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.invoices().summaries());
        }

        @Test
        @DisplayName("500 on details() throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v2/invoices/details",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.invoices().details());
        }
    }
}
