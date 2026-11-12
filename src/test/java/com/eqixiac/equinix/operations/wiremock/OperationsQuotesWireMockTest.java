package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.enums.QuoteStatus;
import com.eqixiac.equinix.operations.model.Quote;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Operations Quotes.
 */
class OperationsQuotesWireMockTest extends WireMockTestBase {

    static Operations operations;

    @BeforeAll
    static void setUp() {
        operations = new Operations(testCredentials());
        redirectToWireMock(operations);
        operations.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (operations != null) operations.close();
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
                    "/json/operations/quote_response.json");

            Quote quote = operations.quotes().getByUuid("1-1234567891011");
            assertNotNull(quote);
            assertEquals("1-1234567891011", quote.getQuoteId());
            assertEquals("AAA Corporation Ltd", quote.getAccountName());
        }

        @Test
        @DisplayName("GETs /v2/quotes/{quoteId} without any ibxs query param")
        void issuesGetToExactPath() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/operations/quote_response.json");

            operations.quotes().getByUuid("1-1234567891011");

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/quotes/1-1234567891011"))
                    .withQueryParam("ibxs", absent()));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v2/quotes/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Quote not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> operations.quotes().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("getByUuid(quoteId, ibxs)")
    class GetByUuidWithIbxs {

        @Test
        @DisplayName("forwards the ibxs list as repeated query params")
        void forwardsIbxs() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/operations/quote_response.json");

            Quote quote = operations.quotes()
                    .getByUuid("1-1234567891011", List.of("AM1", "SV5"));

            assertNotNull(quote);
            assertEquals("1-1234567891011", quote.getQuoteId());
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/quotes/1-1234567891011"))
                    .withQueryParam("ibxs", havingExactly("AM1", "SV5")));
        }

        @Test
        @DisplayName("null ibxs falls back to the un-scoped GET (no ibxs query param)")
        void nullIbxsFallsBack() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/operations/quote_response.json");

            Quote quote = operations.quotes().getByUuid("1-1234567891011", null);

            assertNotNull(quote);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/quotes/1-1234567891011"))
                    .withQueryParam("ibxs", absent()));
        }

        @Test
        @DisplayName("empty ibxs falls back to the un-scoped GET (no ibxs query param)")
        void emptyIbxsFallsBack() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/operations/quote_response.json");

            Quote quote = operations.quotes().getByUuid("1-1234567891011", List.of());

            assertNotNull(quote);
            wireMock.verify(getRequestedFor(urlPathEqualTo("/v2/quotes/1-1234567891011"))
                    .withQueryParam("ibxs", absent()));
        }
    }

    @Nested
    @DisplayName("QuoteWrapper.refresh()")
    class Refresh {

        @Test
        @DisplayName("re-GETs /v2/quotes/{quoteId} and swaps the wrapper's state in place")
        void refreshReloadsInPlace() {
            stubSingleton(wireMock, "/v2/quotes/.*",
                    "/json/operations/quote_response.json");

            Quote quote = operations.quotes().getByUuid("1-1234567891011");
            assertEquals(QuoteStatus.SUBMITTED, quote.getStatus());

            // The quote is approved server-side: the most-recently-registered stub wins, so the
            // refresh GET sees the APPROVED state.
            wireMock.stubFor(get(urlPathEqualTo("/v2/quotes/1-1234567891011"))
                    .willReturn(okJson("{\"quoteId\":\"1-1234567891011\",\"status\":\"APPROVED\"}")));

            quote.refresh();

            assertEquals(QuoteStatus.APPROVED, quote.getStatus());
            assertEquals("1-1234567891011", quote.getQuoteId());

            // Exactly two GETs: the original read plus the refresh re-read of the same path.
            wireMock.verify(2, getRequestedFor(urlPathEqualTo("/v2/quotes/1-1234567891011")));
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
                    () -> operations.quotes().getByUuid("test-uuid"));
        }
    }
}
