package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.customerportal.enums.NotificationType;
import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the CustomerPortal notifications (v1) client, focused on the
 * query-parameter forwarding for search: {@code sorts} (from the request body builder) plus the
 * {@code offset}/{@code limit} paging params.
 */
class CustomerPortalNotificationsWireMockTest extends WireMockTestBase {

    static CustomerPortal customerPortal;

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

    @Test
    @DisplayName("searchIbx with offset/limit forwards the paging query params")
    void searchIbx_forwardsOffsetAndLimit() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/notifications/ibx/search"))
                .willReturn(okJson("{\"data\":[{\"id\":\"n1\"}]}")));

        List<? extends Notification> items = customerPortal.notifications()
                .searchIbx(NotificationSearchRequest.builder().sorts(List.of("-startDate")).build(), 20, 50);

        assertNotNull(items);
        assertEquals(1, items.size());
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/notifications/ibx/search"))
                .withQueryParam("offset", equalTo("20"))
                .withQueryParam("limit", equalTo("50"))
                .withQueryParam("sorts", equalTo("-startDate")));
    }

    @Test
    @DisplayName("searchNetwork without paging sends no offset/limit query params")
    void searchNetwork_noPagingParams() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/notifications/network/search"))
                .willReturn(okJson("{\"data\":[]}")));

        customerPortal.notifications().searchNetwork(NotificationSearchRequest.builder().build());

        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/notifications/network/search"))
                .withQueryParam("offset", absent())
                .withQueryParam("limit", absent()));
    }

    @Nested
    @DisplayName("searchIbx(request)")
    class SearchIbx {

        @Test
        @DisplayName("POSTs the typed filter body to /v1/notifications/ibx/search")
        void searchIbx_postsFilterBody() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/notifications/ibx/search"))
                    .willReturn(okJson("{\"data\":[{\"id\":\"5-1\",\"type\":\"IBX_MAINTENANCE\"}]}")));

            NotificationSearchRequest request = NotificationSearchRequest.builder()
                    .filter(NotificationSearchRequest.Filter.builder()
                            .ibxs(List.of("SV5", "SV1"))
                            .types(List.of("IBX_MAINTENANCE"))
                            .statuses(List.of("NEW"))
                            .dateRange(new NotificationSearchRequest.DateRange(
                                    "2024-11-01T00:00:00.000Z", "2024-11-30T00:00:00.000Z"))
                            .build())
                    .build();

            List<? extends Notification> items = customerPortal.notifications().searchIbx(request);

            assertNotNull(items);
            assertEquals(1, items.size());
            assertEquals("5-1", items.get(0).getId());

            // sorts is @JsonIgnore-d out of the body; no query params on the no-paging overload.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/notifications/ibx/search"))
                    .withQueryParam("offset", absent())
                    .withQueryParam("limit", absent())
                    .withQueryParam("sorts", absent())
                    .withRequestBody(matchingJsonPath("$.filter.ibxs[0]", equalTo("SV5")))
                    .withRequestBody(matchingJsonPath("$.filter.ibxs[1]", equalTo("SV1")))
                    .withRequestBody(matchingJsonPath("$.filter.types[0]", equalTo("IBX_MAINTENANCE")))
                    .withRequestBody(matchingJsonPath("$.filter.statuses[0]", equalTo("NEW")))
                    .withRequestBody(matchingJsonPath("$.filter.dateRange.fromDate", equalTo("2024-11-01T00:00:00.000Z")))
                    .withRequestBody(matchingJsonPath("$.filter.dateRange.toDate", equalTo("2024-11-30T00:00:00.000Z"))));
        }
    }

    @Nested
    @DisplayName("searchNetwork(request, offset, limit)")
    class SearchNetwork {

        @Test
        @DisplayName("POSTs productTypes filter body and forwards paging + sorts query params")
        void searchNetwork_postsBodyAndPaging() {
            wireMock.stubFor(post(urlPathEqualTo("/v1/notifications/network/search"))
                    .willReturn(okJson("{\"data\":[{\"id\":\"n-9\",\"type\":\"NETWORK_INCIDENT\"}]}")));

            NotificationSearchRequest request = NotificationSearchRequest.builder()
                    .sorts(List.of("-startDate"))
                    .filter(NotificationSearchRequest.Filter.builder()
                            .productTypes(List.of("FABRIC"))
                            .statuses(List.of("NEW"))
                            .build())
                    .build();

            List<? extends Notification> items = customerPortal.notifications().searchNetwork(request, 5, 10);

            assertNotNull(items);
            assertEquals(1, items.size());
            assertEquals("n-9", items.get(0).getId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/notifications/network/search"))
                    .withQueryParam("offset", equalTo("5"))
                    .withQueryParam("limit", equalTo("10"))
                    .withQueryParam("sorts", equalTo("-startDate"))
                    .withRequestBody(matchingJsonPath("$.filter.productTypes[0]", equalTo("FABRIC")))
                    .withRequestBody(matchingJsonPath("$.filter.statuses[0]", equalTo("NEW"))));
        }
    }

    @Nested
    @DisplayName("get-by-id")
    class GetById {

        @Test
        @DisplayName("getIbxById GETs /v1/notifications/ibx/{id} and maps the body")
        void getIbxById_getsAndMaps() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/notifications/ibx/5-122719992195"))
                    .willReturn(okJson(loadFixture("/json/customerportal/notification_response.json"))));

            Notification notification = customerPortal.notifications().getIbxById("5-122719992195");

            assertNotNull(notification);
            assertEquals("5-122719992195", notification.getId());
            assertEquals(NotificationType.IBX_MAINTENANCE, notification.getType());
            assertEquals(List.of("SV5", "SV1"), notification.getIbxs());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/notifications/ibx/5-122719992195")));
        }

        @Test
        @DisplayName("getNetworkById GETs /v1/notifications/network/{id}")
        void getNetworkById_gets() {
            wireMock.stubFor(get(urlPathEqualTo("/v1/notifications/network/n-42"))
                    .willReturn(okJson("{\"id\":\"n-42\",\"type\":\"NETWORK_INCIDENT\",\"status\":\"NEW\"}")));

            Notification notification = customerPortal.notifications().getNetworkById("n-42");

            assertNotNull(notification);
            assertEquals("n-42", notification.getId());
            assertEquals(NotificationType.NETWORK_INCIDENT, notification.getType());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/notifications/network/n-42")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 on getIbxById() throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/notifications/ibx/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Notification not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> customerPortal.notifications().getIbxById("missing-id"));
        }

        @Test
        @DisplayName("401 on getNetworkById() throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/v1/notifications/network/.*",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> customerPortal.notifications().getNetworkById("n-42"));
        }

        @Test
        @DisplayName("500 on searchIbx() (POST) throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/notifications/ibx/search",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> customerPortal.notifications()
                            .searchIbx(NotificationSearchRequest.builder().build()));
        }
    }
}
