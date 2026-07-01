package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;

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
