package api.equinix.javasdk.messaging.wiremock;

import api.equinix.javasdk.Messaging;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Messaging domain.
 */
class MessagingWireMockTest extends WireMockTestBase {

    static Messaging messaging;

    @BeforeAll
    static void setUp() {
        messaging = new Messaging(testCredentials());
        redirectToWireMock(messaging);
        messaging.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (messaging != null) messaging.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("Subscriptions - Error handling")
    class SubscriptionsErrors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/messaging/v1/subscriptions/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Subscription not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> messaging.subscriptions().getByUuid("invalid-uuid"));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/messaging/v1/subscriptions/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> messaging.subscriptions().getByUuid("test-uuid"));
        }
    }
}
