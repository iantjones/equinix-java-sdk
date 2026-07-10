package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.BillingAccount;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Billing Accounts (Billing v1) client, focused on
 * the {@code downloadInvoiceDocument} action.
 *
 * <p>Backed by the Billing v1 API at {@code /v1/finance/accounts}. The download endpoint resolves to
 * {@code GET /v1/finance/accounts/{accountNumber}/{invoiceId}?documentId=...} and returns the raw
 * document bytes (a PDF), which the SDK surfaces as a {@code byte[]}.</p>
 */
class CustomerPortalBillingAccountsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String ACCOUNT_NUMBER = "123456";
    private static final String INVOICE_ID = "INV-2024-000451";
    private static final String DOCUMENT_ID = "DOC-987654";

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

    private static String downloadPath() {
        return "/v1/finance/accounts/" + ACCOUNT_NUMBER + "/" + INVOICE_ID;
    }

    /** rootUri {@code finance/accounts}, defaultVersion 1 → {@code /v1/finance/accounts}. */
    private static final String LIST_PATH = "/v1/finance/accounts";
    private static final String ACCOUNT_PATH = "/v1/finance/accounts/" + ACCOUNT_NUMBER;
    private static final String LIST_FIXTURE = "/json/customerportal/paginated_billing_accounts.json";
    private static final String ACCOUNT_FIXTURE = "/json/customerportal/billing_account_response.json";

    @Nested
    @DisplayName("summaries()")
    class Summaries {

        @Test
        @DisplayName("GETs /v1/finance/accounts and returns the paginated summaries")
        void listsSummaries() {
            stubPaginatedGet(wireMock, LIST_PATH, LIST_FIXTURE);

            PaginatedList<BillingAccount> accounts = customerPortal.billingAccounts().summaries();

            assertNotNull(accounts);
            assertEquals(2, accounts.size());
            assertEquals("123456", accounts.get(0).getAccountNumber());
            assertEquals("Acme Cloud Services Inc.", accounts.get(0).getAccountName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH)));
        }

        @Test
        @DisplayName("with a sort specifier passes it as the sorts query param")
        void listsSummariesSorted() {
            stubPaginatedGet(wireMock, LIST_PATH, LIST_FIXTURE);

            PaginatedList<BillingAccount> accounts = customerPortal.billingAccounts().summaries("-ACCOUNT_NUMBER");

            assertNotNull(accounts);
            assertEquals(2, accounts.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("sorts", equalTo("-ACCOUNT_NUMBER")));
        }

        @Test
        @DisplayName("with a null sort specifier omits the sorts query param")
        void listsSummariesNullSort() {
            stubPaginatedGet(wireMock, LIST_PATH, LIST_FIXTURE);

            PaginatedList<BillingAccount> accounts = customerPortal.billingAccounts().summaries(null);

            assertNotNull(accounts);
            assertEquals(2, accounts.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withoutQueryParam("sorts"));
        }
    }

    @Nested
    @DisplayName("Multi-page summaries paging")
    class Paging {

        // summaries() is a plain paginated GET: dispatch stamps offset=0/limit=100 onto the
        // first request, and page 2 is requested by advancing the offset/limit QUERY PARAMETERS
        // from the SERVER-reported pagination.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "accountNumber": "PAGE1_ACCT" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "accountNumber": "PAGE2_ACCT" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the offset query param")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<BillingAccount> accounts = customerPortal.billingAccounts().summaries();
            assertEquals(1, accounts.size());
            assertTrue(accounts.hasNextPage());

            accounts.loadAll();

            assertEquals(2, accounts.size());
            assertEquals("PAGE1_ACCT", accounts.get(0).getAccountNumber());
            assertEquals("PAGE2_ACCT", accounts.get(1).getAccountNumber());
            assertFalse(accounts.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }
    }

    @Nested
    @DisplayName("getByAccountNumber()")
    class GetByAccountNumber {

        @Test
        @DisplayName("GETs /v1/finance/accounts/{accountNumber} and returns the account")
        void getsByNumber() {
            stubSingleton(wireMock, ACCOUNT_PATH, ACCOUNT_FIXTURE);

            BillingAccount account = customerPortal.billingAccounts().getByAccountNumber(ACCOUNT_NUMBER);

            assertNotNull(account);
            assertEquals("123456", account.getAccountNumber());
            assertEquals("Acme Cloud Services Inc.", account.getAccountName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACCOUNT_PATH))
                    .withoutQueryParam("months"));
        }

        @Test
        @DisplayName("with months passes them as the months query param")
        void getsByNumberWithMonths() {
            stubSingleton(wireMock, ACCOUNT_PATH, ACCOUNT_FIXTURE);

            BillingAccount account = customerPortal.billingAccounts()
                    .getByAccountNumber(ACCOUNT_NUMBER, "2017-12-03,2018-01-03");

            assertNotNull(account);
            assertEquals("123456", account.getAccountNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACCOUNT_PATH))
                    .withQueryParam("months", equalTo("2017-12-03,2018-01-03")));
        }

        @Test
        @DisplayName("with null months omits the months query param")
        void getsByNumberNullMonths() {
            stubSingleton(wireMock, ACCOUNT_PATH, ACCOUNT_FIXTURE);

            BillingAccount account = customerPortal.billingAccounts()
                    .getByAccountNumber(ACCOUNT_NUMBER, null);

            assertNotNull(account);
            assertEquals("123456", account.getAccountNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACCOUNT_PATH))
                    .withoutQueryParam("months"));
        }
    }

    @Nested
    @DisplayName("downloadInvoiceDocument()")
    class DownloadInvoiceDocument {

        @Test
        @DisplayName("GETs the accountNumber/invoiceId path with documentId query and returns the raw bytes")
        void returnsDocumentBytes() {
            // A minimal PDF-ish binary payload; the SDK returns the body verbatim as byte[].
            byte[] pdfBytes = ("%PDF-1.4\n%âãÏÓ\ninvoice-document-body\n%%EOF")
                    .getBytes(StandardCharsets.ISO_8859_1);

            wireMock.stubFor(get(urlPathEqualTo(downloadPath()))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/pdf")
                            .withBody(pdfBytes)));

            byte[] result = customerPortal.billingAccounts()
                    .downloadInvoiceDocument(ACCOUNT_NUMBER, INVOICE_ID, DOCUMENT_ID);

            assertNotNull(result);
            assertArrayEquals(pdfBytes, result);

            wireMock.verify(getRequestedFor(urlPathEqualTo(downloadPath()))
                    .withQueryParam("documentId", equalTo(DOCUMENT_ID)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            wireMock.stubFor(get(urlPathEqualTo(downloadPath()))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Document not found\"}]")));

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.billingAccounts()
                            .downloadInvoiceDocument(ACCOUNT_NUMBER, INVOICE_ID, DOCUMENT_ID));
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            wireMock.stubFor(get(urlPathEqualTo(downloadPath()))
                    .willReturn(aResponse()
                            .withStatus(401)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]")));

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.billingAccounts()
                            .downloadInvoiceDocument(ACCOUNT_NUMBER, INVOICE_ID, DOCUMENT_ID));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            wireMock.stubFor(get(urlPathEqualTo(downloadPath()))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]")));

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.billingAccounts()
                            .downloadInvoiceDocument(ACCOUNT_NUMBER, INVOICE_ID, DOCUMENT_ID));
        }
    }
}
