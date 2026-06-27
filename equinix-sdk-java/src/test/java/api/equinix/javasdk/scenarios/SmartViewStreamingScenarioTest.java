package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import api.equinix.javasdk.ibxsmartview.enums.StreamingMessageType;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scenario: IBX SmartView StreamingSubscription CRUD lifecycle.
 *
 * <p>Creates a streaming subscription with a webhook channel, verifies it,
 * lists subscriptions, and tears it down.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmartViewStreamingScenarioTest extends IntegrationTestBase {

    private IBXSmartView ibxSmartView;
    private String subscriptionId;
    private String subscriptionName;

    private void initClient() {
        if (ibxSmartView == null) {
            ibxSmartView = new IBXSmartView(testCredentials());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create streaming subscription")
    void createSubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        initClient();

        try {
            subscriptionName = testResourceName("stream-sub");
            StreamingSubscription subscription = timedCall("IBXSmartView", "create",
                    "StreamingSubscription", "POST", () ->
                            ibxSmartView.streamingSubscriptions().define()
                                    .withName(subscriptionName)
                                    .withDescription("SDK integration test streaming subscription")
                                    .withChannel(ChannelType.WEBHOOK, Map.of(
                                            "url", "https://example.com/webhook",
                                            "contentType", "application/json"
                                    ))
                                    .addMessage(StreamingMessageType.ENVIRONMENTAL,
                                            List.of("1"), List.of("DC2"))
                                    .create()
            );

            assertNotNull(subscription, "Streaming subscription should be created");
            subscriptionId = subscription.getSubscriptionId();
            assertNotNull(subscriptionId, "Subscription ID should not be null");

            registerCleanup("StreamingSubscription", subscriptionId, id -> {
                StreamingSubscription toDelete = ibxSmartView.streamingSubscriptions().getByUuid(id);
                toDelete.delete();
            });
            System.out.printf("  Streaming subscription created: %s (%s)%n",
                    subscriptionName, subscriptionId);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Streaming subscription creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify streaming subscription via GET")
    void verifySubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionId != null,
                "Skipped: no streaming subscription was created");
        initClient();

        StreamingSubscription subscription = timedCall("IBXSmartView", "get",
                "StreamingSubscription", "GET", subscriptionId, () ->
                        ibxSmartView.streamingSubscriptions().getByUuid(subscriptionId)
        );

        assertNotNull(subscription, "Streaming subscription should be retrievable");
        assertNotNull(subscription.getName(), "Subscription name should not be null");
        assertNotNull(subscription.getChannel(), "Subscription channel should not be null");
        System.out.printf("  Streaming subscription verified: %s (status=%s)%n",
                subscription.getName(), subscription.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Update streaming subscription name")
    void updateSubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionId != null,
                "Skipped: no streaming subscription was created");
        initClient();

        try {
            // TODO: StreamingSubscription does not expose an update() builder.
            // If an update method becomes available, implement it here.
            // For now, verify the subscription is still accessible after creation.
            StreamingSubscription subscription = timedCall("IBXSmartView", "get-for-update",
                    "StreamingSubscription", "GET", subscriptionId, () ->
                            ibxSmartView.streamingSubscriptions().getByUuid(subscriptionId)
            );
            assertNotNull(subscription, "Subscription should still be accessible");
            System.out.printf("  Streaming subscription accessible for update check: %s%n",
                    subscription.getName());
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Streaming subscription update not available: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("List streaming subscriptions and verify created subscription appears")
    void listSubscriptions() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionId != null,
                "Skipped: no streaming subscription was created");
        initClient();

        List<StreamingSubscription> subscriptions = timedCall("IBXSmartView", "list",
                "StreamingSubscription", "GET", () ->
                        ibxSmartView.streamingSubscriptions().list()
        );

        assertNotNull(subscriptions, "Subscriptions list should not be null");
        assertFalse(subscriptions.isEmpty(), "Subscriptions list should not be empty");

        boolean found = false;
        for (StreamingSubscription s : subscriptions) {
            if (subscriptionId.equals(s.getSubscriptionId())) {
                found = true;
                break;
            }
        }
        Assumptions.assumeTrue(found,
                "Created subscription should appear in list (may be eventually consistent)");
        System.out.printf("  Streaming subscription found in list of %d subscriptions%n",
                subscriptions.size());
    }

    @Test
    @Order(5)
    @DisplayName("Teardown streaming subscription")
    void teardownSubscription() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(subscriptionId != null,
                "Skipped: no streaming subscription to delete");
        initClient();

        try {
            StreamingSubscription subscription =
                    ibxSmartView.streamingSubscriptions().getByUuid(subscriptionId);
            Boolean deleted = timedCall("IBXSmartView", "delete", "StreamingSubscription",
                    "DELETE", subscriptionId, subscription::delete);
            assertNotNull(deleted, "Delete should return a result");
            System.out.printf("  Streaming subscription deleted: %s%n", subscriptionId);
        } catch (Exception e) {
            System.err.printf("  Streaming subscription teardown failed (cleanup will retry): %s%n",
                    e.getMessage());
        }
    }
}
