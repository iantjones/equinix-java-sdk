package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixRateLimitException;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.internetaccess.enums.PriceCategory;
import com.eqixiac.equinix.internetaccess.enums.ProductType;
import com.eqixiac.equinix.internetaccess.model.Price;
import com.eqixiac.equinix.internetaccess.model.json.creators.PriceSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.eqixiac.equinix.core.ResponseStubs.stubPaginatedPost;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed coverage for the Equinix Internet Access (EIA) v1 price <em>search</em> surface:
 * {@code search} ({@code POST /internetAccess/v1/prices/search}).
 *
 * <p>The {@code Prices} group in {@code apiParams_InternetAccess.json} sets {@code rootUri: "prices"}
 * and {@code defaultVersion: 1}, and the {@code SearchPrices} endpoint uses {@code httpMethod: POST}
 * with {@code requestUri: "search"}. Under the domain
 * {@code internetAccess/v{version}/{rootUri}/{requestUri}} uriFormat, this resolves to
 * {@code /internetAccess/v1/prices/search}. These tests pin the exact path and verb, assert the
 * deserialized page, and for the POST-search verify the serialized
 * {@code {"filter":{"and":[...]}}} request body.</p>
 */
class InternetAccessPricesWireMockTest extends WireMockTestBase {

    private static final String SEARCH_PATH = "/internetAccess/v1/prices/search";

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    class Search {

        @Test
        void issuesPostToV1SearchPathAndDeserializesPage() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_prices.json");

            PriceSearchRequest request = new PriceSearchRequest()
                    .equals("/account/accountNumber", "2-57689234");

            PaginatedFilteredList<Price> prices =
                    internetAccess.prices().search(request);

            assertEquals(2, prices.size());

            Price first = prices.get(0);
            assertNotNull(first);
            assertEquals("IA_C_100", first.getCode());
            assertEquals("Internet Access Connection 100 Mbps", first.getName());
            assertEquals("USD", first.getCurrency());
            assertEquals(ProductType.INTERNET_ACCESS_PRODUCT, first.getType());
            assertEquals(PriceCategory.CUSTOMER, first.getCategory());

            Price second = prices.get(1);
            assertEquals("IP_BLOCK_29", second.getCode());
            assertEquals(ProductType.IP_BLOCK_PRODUCT, second.getType());
            assertEquals(PriceCategory.COUNTRY, second.getCategory());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH)));
        }

        @Test
        void serializesFilterExpressionsInRequestBody() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_prices.json");

            PriceSearchRequest request = new PriceSearchRequest()
                    .equals("/account/accountNumber", "2-57689234")
                    .equals("/service/connection/type", "IA_C")
                    .equals("/service/bandwidth", "100");

            internetAccess.prices().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/account/accountNumber\", \"operator\": \"=\", \"values\": [\"2-57689234\"] },"
                                    + "{ \"property\": \"/service/connection/type\", \"operator\": \"=\", \"values\": [\"IA_C\"] },"
                                    + "{ \"property\": \"/service/bandwidth\", \"operator\": \"=\", \"values\": [\"100\"] }"
                                    + "] } }")));
        }

        @Test
        void searchWithMultipleValuesSerializesValuesArray() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_prices.json");

            PriceSearchRequest request = new PriceSearchRequest()
                    .equals("/service/bandwidth", "100", "1000");

            internetAccess.prices().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/service/bandwidth\", \"operator\": \"=\", \"values\": [\"100\", \"1000\"] }"
                                    + "] } }")));
        }
    }

    @Nested
    class Paging {

        // internetaccessv1 paginates the prices search via offset/limit QUERY PARAMETERS (the
        // FilterBody carries only the filter), so page 2 must be requested by advancing the query
        // offset from the SERVER-reported pagination while re-sending the same filter body.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "code": "PAGE1_PRICE", "currency": "USD" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "code": "PAGE2_PRICE", "currency": "USD" } ]
                }
                """;

        @Test
        void loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // Regression: the prices search body carries no pagination (per spec), so multi-page
            // was previously impossible by design — page 2 threw from the paging pipeline.
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            PriceSearchRequest request = new PriceSearchRequest()
                    .equals("/account/accountNumber", "2-57689234");

            PaginatedFilteredList<Price> prices = internetAccess.prices().search(request);
            assertEquals(1, prices.size());
            assertTrue(prices.hasNextPage());

            prices.loadAll();

            assertEquals(2, prices.size());
            assertEquals("PAGE1_PRICE", prices.get(0).getCode());
            assertEquals("PAGE2_PRICE", prices.get(1).getCode());
            assertFalse(prices.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried,
            // and the SAME filter body re-sent.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/account/accountNumber\", \"operator\": \"=\", \"values\": [\"2-57689234\"] }"
                                    + "] } }")));
        }
    }

    @Nested
    class Errors {

        private void search() {
            internetAccess.prices().search(new PriceSearchRequest()
                    .equals("/account/accountNumber", "2-57689234"));
        }

        @Test
        void forbidden403_throwsEquinixAuthorizationException() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    403, "[{\"errorCode\":\"EQ-3000403\",\"errorMessage\":\"Access denied\"}]");

            assertThrows(EquinixAuthorizationException.class, this::search);
        }

        @Test
        void rateLimited429_throwsEquinixRateLimitException() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    429, "[{\"errorCode\":\"EQ-3000429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class, this::search);
        }
    }
}
