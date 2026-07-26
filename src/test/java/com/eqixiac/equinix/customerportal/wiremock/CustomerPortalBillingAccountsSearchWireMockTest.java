package com.eqixiac.equinix.customerportal.wiremock;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.model.BillingAccountV2;
import com.eqixiac.equinix.customerportal.model.json.creators.BillingAccountSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Billing Account Search (BAS v2) client.
 *
 * <p>Backed by the Platform Billing Account v2 API at {@code /billing/v2/billingAccounts}. Because
 * the {@code BillingAccountsSearch} resource sets {@code overrideUriFormat} to
 * {@code {$rootUri}/{$requestUri}}, the {@code v{version}} prefix is dropped and the rootUri is used
 * verbatim. The three read/search ops resolve to:</p>
 * <ul>
 *   <li>{@code search}            → {@code POST /billing/v2/billingAccounts/search}</li>
 *   <li>{@code getByAccountNumber}→ {@code GET  /billing/v2/billingAccounts/accountNumber/{accountNumber}}</li>
 *   <li>{@code getByAccountId}    → {@code GET  /billing/v2/billingAccounts/{accountId}}</li>
 * </ul>
 */
class CustomerPortalBillingAccountsSearchWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String ROOT = "/billing/v2/billingAccounts";
    private static final String SEARCH_PATH = ROOT + "/search";
    private static final String ACCOUNT_NUMBER = "123456";
    private static final String ACCOUNT_ID = "acct-0001";
    private static final String BY_NUMBER_PATH = ROOT + "/accountNumber/" + ACCOUNT_NUMBER;
    private static final String BY_ID_PATH = ROOT + "/" + ACCOUNT_ID;

    private static final String SEARCH_FIXTURE = "/json/customerportal/paginated_billing_accounts_v2.json";
    private static final String ACCOUNT_FIXTURE = "/json/customerportal/billing_account_v2_response.json";

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
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("POSTs /billing/v2/billingAccounts/search and returns the paginated accounts")
        void searchesAccounts() {
            stubPaginatedPost(wireMock, SEARCH_PATH, SEARCH_FIXTURE);

            BillingAccountSearchRequest request = BillingAccountSearchRequest.builder()
                    .ibxCode("SV5")
                    .metroCode("SV")
                    .accountStatus(List.of("ACTIVE", "INACTIVE"))
                    .projectId("proj-42")
                    .limit(20)
                    .offset(0)
                    .build();

            PaginatedList<BillingAccountV2> accounts = customerPortal.billingAccountsSearch().search(request);

            assertNotNull(accounts);
            assertEquals(2, accounts.size());
            assertEquals("acct-0001", accounts.get(0).getAccountId());
            assertEquals("123456", accounts.get(0).getAccountNumber());
            assertEquals("Acme Cloud Services Inc.", accounts.get(0).getAccountName());
            assertEquals("acct-0002", accounts.get(1).getAccountId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.ibxCode", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.metroCode", equalTo("SV")))
                    .withRequestBody(matchingJsonPath("$.accountStatus[0]", equalTo("ACTIVE")))
                    .withRequestBody(matchingJsonPath("$.accountStatus[1]", equalTo("INACTIVE")))
                    .withRequestBody(matchingJsonPath("$.projectId", equalTo("proj-42")))
                    .withRequestBody(matchingJsonPath("$.limit", equalTo("20")))
                    .withRequestBody(matchingJsonPath("$.offset", equalTo("0"))));
        }

        @Test
        @DisplayName("with an empty request omits the NON_NULL criteria from the body")
        void searchesWithEmptyRequest() {
            stubPaginatedPost(wireMock, SEARCH_PATH, SEARCH_FIXTURE);

            PaginatedList<BillingAccountV2> accounts = customerPortal.billingAccountsSearch()
                    .search(BillingAccountSearchRequest.builder().build());

            assertNotNull(accounts);
            assertEquals(2, accounts.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.ibxCode", absent()))
                    .withRequestBody(matchingJsonPath("$.metroCode", absent()))
                    .withRequestBody(matchingJsonPath("$.accountStatus", absent()))
                    .withRequestBody(matchingJsonPath("$.projectId", absent()))
                    .withRequestBody(matchingJsonPath("$.limit", absent()))
                    .withRequestBody(matchingJsonPath("$.offset", absent())));
        }
    }

    @Nested
    @DisplayName("Multi-page search paging")
    class Paging {

        // basv2's SearchRequest carries its pagination as TOP-LEVEL body members (offset/limit),
        // so page 2 must be requested by re-sending the body with the offset advanced from the
        // SERVER-reported pagination.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "accountId": "PAGE1_ACCT", "accountNumber": "111" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "accountId": "PAGE2_ACCT", "accountNumber": "222" } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing the body's offset member (regression: ClassCastException on page 2)")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offset", equalTo("0")))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offset", equalTo("100")))
                    .willReturn(okJson(PAGE_2)));

            BillingAccountSearchRequest request = BillingAccountSearchRequest.builder()
                    .ibxCode("SV5")
                    .limit(100)
                    .offset(0)
                    .build();

            PaginatedList<BillingAccountV2> accounts = customerPortal.billingAccountsSearch().search(request);
            assertEquals(1, accounts.size());
            assertTrue(accounts.hasNextPage());

            accounts.loadAll();

            assertEquals(2, accounts.size());
            assertEquals("PAGE1_ACCT", accounts.get(0).getAccountId());
            assertEquals("PAGE2_ACCT", accounts.get(1).getAccountId());
            assertFalse(accounts.hasNextPage());

            // Page 2 body: offset advanced from the server-reported pagination, limit carried,
            // and the same filter criteria re-sent.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.offset", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.limit", equalTo("100")))
                    .withRequestBody(matchingJsonPath("$.ibxCode", equalTo("SV5"))));
        }
    }

    @Nested
    @DisplayName("getByAccountNumber()")
    class GetByAccountNumber {

        @Test
        @DisplayName("GETs /billing/v2/billingAccounts/accountNumber/{accountNumber} and returns the account")
        void getsByNumber() {
            stubSingleton(wireMock, BY_NUMBER_PATH, ACCOUNT_FIXTURE);

            BillingAccountV2 account = customerPortal.billingAccountsSearch().getByAccountNumber(ACCOUNT_NUMBER);

            assertNotNull(account);
            assertEquals("acct-0001", account.getAccountId());
            assertEquals("123456", account.getAccountNumber());
            assertEquals("Acme Cloud Services Inc.", account.getAccountName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BY_NUMBER_PATH)));
        }
    }

    @Nested
    @DisplayName("getByAccountId()")
    class GetByAccountId {

        @Test
        @DisplayName("GETs /billing/v2/billingAccounts/{accountId} and returns the account")
        void getsById() {
            stubSingleton(wireMock, BY_ID_PATH, ACCOUNT_FIXTURE);

            BillingAccountV2 account = customerPortal.billingAccountsSearch().getByAccountId(ACCOUNT_ID);

            assertNotNull(account);
            assertEquals("acct-0001", account.getAccountId());
            assertEquals("123456", account.getAccountNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BY_ID_PATH)));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on getByAccountId() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, ROOT + "/[^/]+",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Account not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.billingAccountsSearch().getByAccountId("missing-acct"));
        }

        @Test
        @DisplayName("401 on getByAccountNumber() throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, ROOT + "/accountNumber/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.billingAccountsSearch().getByAccountNumber(ACCOUNT_NUMBER));
        }

        @Test
        @DisplayName("500 on search() (POST) throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.billingAccountsSearch()
                            .search(BillingAccountSearchRequest.builder().build()));
        }
    }
}
