package api.equinix.javasdk.fabric.wiremock;
import api.equinix.javasdk.fabric.enums.Marketplace;
import api.equinix.javasdk.fabric.enums.MarketplaceOfferType;

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
            assertEquals(MarketplaceSubscriptionState.ACTIVE, subscription.getState());
            assertEquals(Marketplace.AWS, subscription.getMarketplace());
            assertEquals(MarketplaceOfferType.PRIVATE_OFFER, subscription.getOfferType());
            assertEquals("offer-12345", subscription.getOfferId());
            assertTrue(subscription.getIsAutoRenew());

            assertNotNull(subscription.getTrial());
            assertTrue(subscription.getTrial().getEnabled());
            assertEquals("2026-08-21T10:30:00Z", subscription.getTrial().getExpiryDateTime());

            assertEquals(java.util.List.of("SV", "DC"), subscription.getMetroCodes());

            assertNotNull(subscription.getEntitlements());
            assertEquals(2, subscription.getEntitlements().size());
            assertEquals("a15b6b20-b765-4bf7-a661-a3e9372d5435", subscription.getEntitlements().get(0).getUuid());
            assertEquals(1, subscription.getEntitlements().get(0).getQuantityEntitled());
            assertEquals(0, subscription.getEntitlements().get(0).getQuantityConsumed());
            assertEquals(1, subscription.getEntitlements().get(0).getQuantityAvailable());
            assertEquals("XF_ROUTER", subscription.getEntitlements().get(0).getAsset().getType());
            assertEquals("STANDARD", subscription.getEntitlements().get(0).getAsset().getAssetPackage().getCode());
            assertEquals("IP_VC", subscription.getEntitlements().get(1).getAsset().getType());
            assertEquals(500, subscription.getEntitlements().get(1).getAsset().getBandwidth());

            // Wire property is lowercase "changelog" on this resource (spec SubscriptionResponse).
            assertNotNull(subscription.getChangeLog());
            assertEquals("adminuser", subscription.getChangeLog().getCreatedBy());

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
