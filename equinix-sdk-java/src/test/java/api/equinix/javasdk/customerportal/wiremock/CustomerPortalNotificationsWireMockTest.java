package api.equinix.javasdk.customerportal.wiremock;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;
import org.junit.jupiter.api.*;

import java.util.List;

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
}
