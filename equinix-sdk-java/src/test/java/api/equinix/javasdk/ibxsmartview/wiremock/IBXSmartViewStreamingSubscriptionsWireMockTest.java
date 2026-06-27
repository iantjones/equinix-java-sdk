package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for IBX SmartView Streaming Subscriptions.
 */
class IBXSmartViewStreamingSubscriptionsWireMockTest extends WireMockTestBase {

    static IBXSmartView ibxSmartView;

    @BeforeAll
    static void setUp() {
        ibxSmartView = new IBXSmartView(testCredentials());
        redirectToWireMock(ibxSmartView);
        ibxSmartView.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (ibxSmartView != null) ibxSmartView.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns streaming subscription for valid UUID")
        void returnsSubscription() {
            stubSingleton(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    "/json/ibxsmartview/streaming_subscription_response.json");

            StreamingSubscription subscription = ibxSmartView.streamingSubscriptions().getByUuid("sub-12345-abcde");
            assertNotNull(subscription);
            assertEquals("sub-12345-abcde", subscription.getSubscriptionId());
            assertEquals("My Environment Subscription", subscription.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Subscription not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> ibxSmartView.streamingSubscriptions().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/smartview/v2/streaming/subscriptions/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> ibxSmartView.streamingSubscriptions().getByUuid("test-uuid"));
        }
    }
}
