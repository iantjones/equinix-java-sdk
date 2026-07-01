package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.enums.PriceCategory;
import api.equinix.javasdk.internetaccess.enums.ProductType;
import api.equinix.javasdk.internetaccess.model.Price;
import api.equinix.javasdk.internetaccess.model.json.creators.PriceSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedPost;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
