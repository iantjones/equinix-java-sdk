package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.PermissibleLocation;
import api.equinix.javasdk.customerportal.model.json.creators.OrderHistorySearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal OrderHistory (retrieve-orders v1) client.
 *
 * <p>Covers the two read ops declared in {@code apiParams_CustomerPortal.json} under
 * {@code OrderHistory} (rootUri {@code retrieve-orders}, defaultVersion 1, format
 * {@code v{version}/{rootUri}/{requestUri}}):</p>
 * <ul>
 *   <li>{@code search(OrderHistorySearchRequest)} &rarr; {@code POST /v1/retrieve-orders}</li>
 *   <li>{@code listLocations()} &rarr; {@code GET /v1/retrieve-orders/locations}</li>
 * </ul>
 */
class CustomerPortalOrderHistoryWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String SEARCH_PATH = "/v1/retrieve-orders";
    private static final String LOCATIONS_PATH = "/v1/retrieve-orders/locations";

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
        @DisplayName("POSTs the search body to /v1/retrieve-orders and returns the content records")
        void returnsContent() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/customerportal/order_history_search_response.json");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                    .filters(OrderHistorySearchRequest.Filters.builder()
                            .ibxs(List.of("LD5", "SV1"))
                            .productTypes(List.of("CROSS_CONNECT"))
                            .orderStatus(List.of("IN_PROGRESS"))
                            .dateRange("LAST_30_DAYS")
                            .build())
                    .source(List.of("orderNumber"))
                    .q("1-2345")
                    .sorts(List.of(new OrderHistorySearchRequest.Sort("createdAt", "DESC")))
                    .page(new OrderHistorySearchRequest.PageRequest(0, 20))
                    .build();

            List<? extends OrderHistoryItem> results = customerPortal.orderHistory().search(request);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("1-234567890", results.get(0).getOrderNumber());
            assertEquals("IN_PROGRESS", results.get(0).getOrderStatus());
            assertEquals(List.of("LD5", "LD6"), results.get(0).getIbx());
            assertEquals("Acme Corp", results.get(0).getAccount().getName());
            assertEquals("Ada Lovelace", results.get(0).getOrderingContact().getName());
            assertEquals("orderFullDetails", results.get(0).getLinks().get(1).getRel());
            assertEquals("1-234567891", results.get(1).getOrderNumber());
            assertEquals("CLOSED", results.get(1).getOrderStatus());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.q", equalTo("1-2345")))
                    .withRequestBody(matchingJsonPath("$.source[0]", equalTo("orderNumber")))
                    .withRequestBody(matchingJsonPath("$.filters.ibxs[0]", equalTo("LD5")))
                    .withRequestBody(matchingJsonPath("$.filters.ibxs[1]", equalTo("SV1")))
                    .withRequestBody(matchingJsonPath("$.filters.productTypes[0]", equalTo("CROSS_CONNECT")))
                    .withRequestBody(matchingJsonPath("$.filters.orderStatus[0]", equalTo("IN_PROGRESS")))
                    .withRequestBody(matchingJsonPath("$.filters.dateRange", equalTo("LAST_30_DAYS")))
                    .withRequestBody(matchingJsonPath("$.sorts[0].name", equalTo("createdAt")))
                    .withRequestBody(matchingJsonPath("$.sorts[0].direction", equalTo("DESC")))
                    .withRequestBody(matchingJsonPath("$.page.number", equalTo("0")))
                    .withRequestBody(matchingJsonPath("$.page.size", equalTo("20"))));
        }

        @Test
        @DisplayName("omits null optional filter fields from the serialized body")
        void omitsNullOptionalFields() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/customerportal/order_history_search_response.json");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                    .filters(OrderHistorySearchRequest.Filters.builder()
                            .ibxs(List.of("LD5"))
                            .build())
                    .build();

            customerPortal.orderHistory().search(request);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.filters.ibxs[0]", equalTo("LD5")))
                    .withRequestBody(notMatching("(?s).*\"q\".*"))
                    .withRequestBody(notMatching("(?s).*\"sorts\".*"))
                    .withRequestBody(notMatching("(?s).*\"productTypes\".*"))
                    .withRequestBody(notMatching("(?s).*\"dateRange\".*")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, SEARCH_PATH,
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder().build();

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.orderHistory().search(request));
        }
    }

    @Nested
    @DisplayName("listLocations()")
    class ListLocations {

        @Test
        @DisplayName("GETs /v1/retrieve-orders/locations and returns the permissible locations")
        void returnsLocations() {
            stubPaginatedGet(wireMock, LOCATIONS_PATH,
                    "/json/customerportal/order_history_locations_response.json");

            List<? extends PermissibleLocation> locations = customerPortal.orderHistory().listLocations();

            assertNotNull(locations);
            assertEquals(2, locations.size());
            assertEquals("LD5", locations.get(0).getIbx().getCode());
            assertEquals("London", locations.get(0).getIbx().getMetro());
            assertEquals("EMEA", locations.get(0).getIbx().getRegion());
            assertEquals(List.of("LD5:01:000123", "LD5:01:000124"), locations.get(0).getCages());
            assertEquals("SV1", locations.get(1).getIbx().getCode());
            assertEquals("Silicon Valley", locations.get(1).getIbx().getMetro());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LOCATIONS_PATH)));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, LOCATIONS_PATH,
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.orderHistory().listLocations());
        }
    }
}
