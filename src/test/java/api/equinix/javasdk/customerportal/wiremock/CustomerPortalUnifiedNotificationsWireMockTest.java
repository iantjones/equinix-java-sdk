package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.enums.NotificationCategory;
import api.equinix.javasdk.customerportal.enums.NotificationSortBy;
import api.equinix.javasdk.customerportal.enums.NotificationSortDirection;
import api.equinix.javasdk.customerportal.enums.NotificationType;
import api.equinix.javasdk.customerportal.model.UnifiedNotification;
import api.equinix.javasdk.customerportal.model.json.creators.UnifiedNotificationSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal Unified Notifications (v2) client.
 *
 * <p>Backed by the unified notifications v2 search API. The {@code UnifiedNotifications} resource
 * sets {@code rootUri} to {@code notifications/v2} and {@code overrideUriFormat} to
 * {@code {$rootUri}/{$requestUri}}, so the single read/search op resolves to:</p>
 * <ul>
 *   <li>{@code getNotifications} → {@code POST /notifications/v2/events/findAll}</li>
 * </ul>
 */
class CustomerPortalUnifiedNotificationsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

    private static final String SEARCH_PATH = "/notifications/v2/events/findAll";
    private static final String SEARCH_FIXTURE = "/json/customerportal/paginated_unified_notifications.json";

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
    @DisplayName("getNotifications(request)")
    class GetNotifications {

        @Test
        @DisplayName("POSTs the filter/sort/pagination body to /notifications/v2/events/findAll and maps the data list")
        void postsFullBodyAndMaps() {
            stubPaginatedPost(wireMock, SEARCH_PATH, SEARCH_FIXTURE);

            UnifiedNotificationSearchRequest request = UnifiedNotificationSearchRequest.builder()
                    .filter(UnifiedNotificationSearchRequest.Filter.builder()
                            .category(List.of(NotificationCategory.MAINTENANCE, NotificationCategory.INCIDENT))
                            .type(List.of(NotificationType.IBX_MAINTENANCE))
                            .notificationNumber(List.of("5-987654321"))
                            .accountNumber(List.of("123456"))
                            .createdDateTime(List.of("2024-11-05T09:30:00.000Z"))
                            .build())
                    .sort(List.of(new UnifiedNotificationSearchRequest.SortCriteria(
                            NotificationSortDirection.DESC, NotificationSortBy.createdDateTime)))
                    .pagination(new UnifiedNotificationSearchRequest.PaginationRequest(0, 20))
                    .build();

            List<? extends UnifiedNotification> items = customerPortal.unifiedNotifications().getNotifications(request);

            assertNotNull(items);
            assertEquals(2, items.size());
            assertEquals("un-0001", items.get(0).getId());
            assertEquals("5-987654321", items.get(0).getNotificationNumber());
            assertEquals(NotificationCategory.MAINTENANCE, items.get(0).getCategory());
            assertEquals(NotificationType.IBX_MAINTENANCE, items.get(0).getType());
            assertEquals("Scheduled electrical maintenance at SV5", items.get(0).getSummary());
            assertEquals(List.of("SV5", "SV1"), items.get(0).getIbxs());
            assertEquals("un-0002", items.get(1).getId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.filter.category[0]", equalTo("MAINTENANCE")))
                    .withRequestBody(matchingJsonPath("$.filter.category[1]", equalTo("INCIDENT")))
                    .withRequestBody(matchingJsonPath("$.filter.type[0]", equalTo("IBX_MAINTENANCE")))
                    .withRequestBody(matchingJsonPath("$.filter.notificationNumber[0]", equalTo("5-987654321")))
                    .withRequestBody(matchingJsonPath("$.filter.accountNumber[0]", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.filter.createdDateTime[0]", equalTo("2024-11-05T09:30:00.000Z")))
                    .withRequestBody(matchingJsonPath("$.sort[0].direction", equalTo("DESC")))
                    .withRequestBody(matchingJsonPath("$.sort[0].property", equalTo("createdDateTime")))
                    .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                    .withRequestBody(matchingJsonPath("$.pagination.limit", equalTo("20"))));
        }

        @Test
        @DisplayName("with an empty request omits the NON_NULL top-level blocks from the body")
        void postsEmptyBody() {
            stubPaginatedPost(wireMock, SEARCH_PATH, SEARCH_FIXTURE);

            List<? extends UnifiedNotification> items = customerPortal.unifiedNotifications()
                    .getNotifications(UnifiedNotificationSearchRequest.builder().build());

            assertNotNull(items);
            assertEquals(2, items.size());

            wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH))
                    .withRequestBody(matchingJsonPath("$.filter", absent()))
                    .withRequestBody(matchingJsonPath("$.sort", absent()))
                    .withRequestBody(matchingJsonPath("$.pagination", absent())));
        }
    }
}
