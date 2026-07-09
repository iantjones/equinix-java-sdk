package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedPost;
import static api.equinix.javasdk.core.ResponseStubs.stubSingleton;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed coverage for the Equinix Internet Access (EIA) v2 service <em>read</em> surface:
 * {@code getByUuid} ({@code GET /internetAccess/v2/services/{serviceId}}) and {@code search}
 * ({@code POST /internetAccess/v2/services/search}).
 *
 * <p>The {@code Services} group in {@code apiParams_InternetAccess.json} uses {@code defaultVersion: 2}
 * and the domain {@code internetAccess/v{version}/{rootUri}/{requestUri}} uriFormat, so paths resolve
 * under {@code /internetAccess/v2/services}. These tests pin the exact path and verb for each op, and
 * for the POST-search assert the serialized {@code {"filter":{"and":[...]}}} request body.</p>
 */
class InternetAccessServiceSearchWireMockTest extends WireMockTestBase {

    private static final String SERVICE_ID = "e1f2a3b4-c5d6-4e7f-8091-021324354657";
    private static final String SERVICE_PATH = "/internetAccess/v2/services/" + SERVICE_ID;
    private static final String SEARCH_PATH = "/internetAccess/v2/services/search";

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
    class GetByUuid {

        @Test
        void issuesGetToServiceIdPathAndDeserializes() {
            stubSingleton(wireMock, SERVICE_PATH,
                    "/json/internetaccess/internet_access_service_response.json");

            InternetAccessService service = internetAccess.services().getByUuid(SERVICE_ID);

            assertNotNull(service);
            assertEquals(SERVICE_ID, service.getUuid());
            assertEquals("WebServers", service.getName());
            assertEquals(Long.valueOf(1000), service.getBandwidth());

            wireMock.verify(getRequestedFor(urlPathEqualTo(SERVICE_PATH)));
        }
    }

    @Nested
    class Search {

        @Test
        void issuesPostToSearchPathAndDeserializesPage() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_services.json");

            ServiceSearchRequest request = new ServiceSearchRequest()
                    .equals("/state", "ACTIVE");

            PaginatedFilteredList<InternetAccessService> services =
                    internetAccess.services().search(request);

            assertEquals(2, services.size());
            assertEquals("WebServers", services.get(0).getName());
            assertEquals("AppServers", services.get(1).getName());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH)));
        }

        @Test
        void serializesFilterExpressionsInRequestBody() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_services.json");

            ServiceSearchRequest request = new ServiceSearchRequest()
                    .equals("/state", "ACTIVE")
                    .equals("/type", "SINGLE");

            internetAccess.services().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/state\", \"operator\": \"=\", \"values\": [\"ACTIVE\"] },"
                                    + "{ \"property\": \"/type\", \"operator\": \"=\", \"values\": [\"SINGLE\"] }"
                                    + "] } }")));
        }

        @Test
        void searchWithMultipleValuesSerializesValuesArray() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/internetaccess/paginated_services.json");

            ServiceSearchRequest request = new ServiceSearchRequest()
                    .equals("/type", "SINGLE", "REDUNDANT");

            internetAccess.services().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/type\", \"operator\": \"=\", \"values\": [\"SINGLE\", \"REDUNDANT\"] }"
                                    + "] } }")));
        }
    }

    @Nested
    class Paging {

        // internetaccessv2 paginates the services search via offset/limit QUERY PARAMETERS (the
        // body carries only the filter), so page 2 must be requested by advancing the query
        // offset from the SERVER-reported pagination while re-sending the same filter body.
        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "11111111-1111-4111-8111-111111111111", "name": "PAGE1_SERVICE" } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 100, "limit": 100, "total": 150 },
                  "data": [ { "uuid": "22222222-2222-4222-8222-222222222222", "name": "PAGE2_SERVICE" } ]
                }
                """;

        @Test
        void loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // Regression: the services search body carries no pagination (per spec), so
            // multi-page was previously impossible by design — page 2 threw from the paging pipeline.
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson(PAGE_2)));

            ServiceSearchRequest request = new ServiceSearchRequest()
                    .equals("/state", "ACTIVE");

            PaginatedFilteredList<InternetAccessService> services = internetAccess.services().search(request);
            assertEquals(1, services.size());
            assertTrue(services.hasNextPage());

            services.loadAll();

            assertEquals(2, services.size());
            assertEquals("PAGE1_SERVICE", services.get(0).getName());
            assertEquals("PAGE2_SERVICE", services.get(1).getName());
            assertFalse(services.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried,
            // and the SAME filter body re-sent.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withRequestBody(equalToJson(
                            "{ \"filter\": { \"and\": ["
                                    + "{ \"property\": \"/state\", \"operator\": \"=\", \"values\": [\"ACTIVE\"] }"
                                    + "] } }")));
        }
    }
}
