package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.MarketplaceSubscriptionState;
import api.equinix.javasdk.fabric.model.MarketplaceSubscription;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Marketplace Subscriptions (read-only).
 */
class FabricMarketplaceSubscriptionsWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs /marketplaceSubscriptions/{uuid} and returns the single subscription")
        void returnsSubscription() {
            stubSingleton(wireMock,
                    "/fabric/v4/marketplaceSubscriptions/195be615-a8ad-4c33-8e9c-c7612fbf6c9f",
                    "/json/fabric/marketplace_subscription_response.json");

            MarketplaceSubscription subscription = fabric.marketplaceSubscriptions()
                    .getByUuid("195be615-a8ad-4c33-8e9c-c7612fbf6c9f");

            assertNotNull(subscription);
            assertEquals("195be615-a8ad-4c33-8e9c-c7612fbf6c9f", subscription.getUuid());
            assertEquals("MARKETPLACE_SUBSCRIPTION", subscription.getType());
            assertEquals(MarketplaceSubscriptionState.ACTIVE, subscription.getState());
            assertEquals("AWS", subscription.getMarketplace());
            assertEquals("offer-12345", subscription.getOfferId());
            assertTrue(subscription.getIsAutoRenew());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/fabric/v4/marketplaceSubscriptions/195be615-a8ad-4c33-8e9c-c7612fbf6c9f")));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/marketplaceSubscriptions/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.marketplaceSubscriptions().getByUuid("missing-uuid"));
        }
    }
}
