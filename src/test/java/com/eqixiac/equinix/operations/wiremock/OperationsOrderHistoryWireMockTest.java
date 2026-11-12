package com.eqixiac.equinix.operations.wiremock;

import com.eqixiac.equinix.operations.enums.OrderHistoryLinkRel;
import com.eqixiac.equinix.operations.enums.OrderHistoryStatus;
import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.operations.model.OrderHistoryItem;
import com.eqixiac.equinix.operations.model.PermissibleLocation;
import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.operations.enums.OrderHistoryDateRange;
import com.eqixiac.equinix.operations.enums.OrderHistoryProductType;
import com.eqixiac.equinix.operations.enums.OrderHistorySearchSource;
import com.eqixiac.equinix.operations.enums.OrderHistorySortField;
import com.eqixiac.equinix.operations.model.OrderHistoryPage;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the Operations OrderHistory (retrieve-orders v1) client.
 *
 * <p>Covers the two read ops declared in {@code apiParams_Operations.json} under
 * {@code OrderHistory} (rootUri {@code retrieve-orders}, defaultVersion 1, format
 * {@code v{version}/{rootUri}/{requestUri}}):</p>
 * <ul>
 *   <li>{@code search(OrderHistorySearchRequest)} &rarr; {@code POST /v1/retrieve-orders}</li>
 *   <li>{@code listLocations()} &rarr; {@code GET /v1/retrieve-orders/locations}</li>
 * </ul>
 */
class OperationsOrderHistoryWireMockTest extends WireMockTestBase {

    static Operations operations;

    private static final String SEARCH_PATH = "/v1/retrieve-orders";
    private static final String LOCATIONS_PATH = "/v1/retrieve-orders/locations";

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
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("reads the field names the live service returns (accountName/accountNumber, orderingContacts, notificationContacts)")
        void readsLiveFieldNames() {
            // Field names and types captured from the live service on 2026-11-11; values are synthetic.
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/operations/order_history_search_response_live.json");

            List<? extends OrderHistoryItem> results = operations.orderHistory()
                    .search(OrderHistorySearchRequest.builder().build());

            assertEquals(1, results.size());
            OrderHistoryItem item = results.get(0);
            assertEquals("123456", item.getAccount().getNumber());
            assertEquals("Example Holdings", item.getAccount().getName());
            assertEquals("Ada", item.getOrderingContact().getFirstName());
            assertEquals("ada@example.com", item.getOrderingContact().getEmail());
            assertEquals(1, item.getNotificationContacts().size());
            assertEquals("grace@example.com", item.getNotificationContacts().get(0).getEmail());
            assertEquals("Portal", item.getOrderSource());
            assertEquals(List.of("SMART_HANDS"), item.getType());
        }

        @Test
        @DisplayName("searchPage exposes the {links, content, page} envelope (live shape)")
        void searchPageExposesPaging() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/operations/order_history_search_response_live.json");

            OrderHistoryPage page = operations.orderHistory()
                    .searchPage(OrderHistorySearchRequest.builder().build());

            assertEquals(1, page.getContent().size());
            assertNotNull(page.getTotalElements());
            assertNotNull(page.getTotalPages());
            assertNotNull(page.getNumber());
            assertNotNull(page.getSize());
            assertFalse(page.getLinks().isEmpty());
        }

        @Test
        @DisplayName("POSTs the search body to /v1/retrieve-orders and returns the content records")
        void returnsContent() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/operations/order_history_search_response.json");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                    .filters(OrderHistorySearchRequest.Filters.builder()
                            .ibxs(List.of("LD5", "SV1"))
                            .productTypes(OrderHistoryProductType.CROSS_CONNECT, OrderHistoryProductType.INBOUND_SHIPMENT)
                            .orderStatus(OrderHistoryStatus.IN_PROGRESS)
                            .dateRange(OrderHistoryDateRange.PAST_30_DAYS)
                            .fromDate(LocalDate.of(2026, 1, 2))
                            .build())
                    .source(OrderHistorySearchSource.ORDER_NUMBER)
                    .q("1-2345")
                    .sorts(List.of(new OrderHistorySearchRequest.Sort(OrderHistorySortField.CREATED_ON, SortOrder.DESC)))
                    .page(new OrderHistorySearchRequest.PageRequest(0, 20))
                    .build();

            List<? extends OrderHistoryItem> results = operations.orderHistory().search(request);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("1-234567890", results.get(0).getOrderNumber());
            assertEquals(OrderHistoryStatus.IN_PROGRESS, results.get(0).getOrderStatus());
            assertEquals(List.of("LD5", "LD6"), results.get(0).getIbx());
            assertEquals("Acme Corp", results.get(0).getAccount().getName());
            assertEquals("Ada Lovelace", results.get(0).getOrderingContact().getName());
            assertEquals(OrderHistoryLinkRel.ORDER_FULL_DETAILS, results.get(0).getLinks().get(1).getRel());
            assertEquals("1-234567891", results.get(1).getOrderNumber());
            assertEquals(OrderHistoryStatus.CLOSED, results.get(1).getOrderStatus());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.q", equalTo("1-2345")))
                    .withRequestBody(matchingJsonPath("$.source[0]", equalTo("ORDER_NUMBER")))
                    .withRequestBody(matchingJsonPath("$.filters.ibxs[0]", equalTo("LD5")))
                    .withRequestBody(matchingJsonPath("$.filters.ibxs[1]", equalTo("SV1")))
                    .withRequestBody(matchingJsonPath("$.filters.productTypes[0]", equalTo("CROSS_CONNECT")))
                    .withRequestBody(matchingJsonPath("$.filters.productTypes[1]", equalTo("INBOUND_SHIPMENT")))
                    .withRequestBody(matchingJsonPath("$.filters.fromDate", equalTo("2026-01-02")))
                    .withRequestBody(matchingJsonPath("$.filters.orderStatus[0]", equalTo("IN_PROGRESS")))
                    .withRequestBody(matchingJsonPath("$.filters.dateRange", equalTo("PAST_30_DAYS")))
                    .withRequestBody(matchingJsonPath("$.sorts[0].name", equalTo("CREATED_ON")))
                    .withRequestBody(matchingJsonPath("$.sorts[0].direction", equalTo("DESC")))
                    .withRequestBody(matchingJsonPath("$.page.number", equalTo("0")))
                    .withRequestBody(matchingJsonPath("$.page.size", equalTo("20"))));
        }

        @Test
        @DisplayName("omits null optional filter fields from the serialized body")
        void omitsNullOptionalFields() {
            stubPaginatedPost(wireMock, SEARCH_PATH,
                    "/json/operations/order_history_search_response.json");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder()
                    .filters(OrderHistorySearchRequest.Filters.builder()
                            .ibxs(List.of("LD5"))
                            .build())
                    .build();

            operations.orderHistory().search(request);

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
                    404, "{\"errors\":[{\"code\":\"EQX-123-404\",\"message\":\"Not found\"}],\"ticketId\":\"190619221547671\"}");

            OrderHistorySearchRequest request = OrderHistorySearchRequest.builder().build();

            assertThrows(EquinixNotFoundException.class,
                    () -> operations.orderHistory().search(request));
        }
    }

    @Nested
    @DisplayName("listLocations()")
    class ListLocations {

        @Test
        @DisplayName("GETs /v1/retrieve-orders/locations and returns the permissible locations")
        void returnsLocations() {
            stubPaginatedGet(wireMock, LOCATIONS_PATH,
                    "/json/operations/order_history_locations_response.json");

            List<? extends PermissibleLocation> locations = operations.orderHistory().listLocations();

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
                    500, "{\"errors\":[{\"code\":\"EQX-123-500\",\"message\":\"Internal server error\"}],\"ticketId\":\"190619221547671\"}");

            assertThrows(EquinixServerException.class,
                    () -> operations.orderHistory().listLocations());
        }
    }
}
