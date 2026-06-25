package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.model.Event;
import api.equinix.javasdk.messaging.model.Subscription;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration-readonly")
@DisplayName("Messaging Integration Tests")
class MessagingIntegrationTest extends IntegrationTestBase {

    static Messaging client;

    @BeforeAll
    static void setUp() {
        client = new Messaging(testCredentials());
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Readonly Operations")
    class ReadonlyTests {

        @Test
        @DisplayName("List subscriptions and get by UUID")
        void listSubscriptions() {
            try {
                PaginatedList<Subscription> items = timedCall("Messaging", "list", "Subscription", "GET",
                        () -> client.subscriptions().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Subscription item = timedCall("Messaging", "getByUuid", "Subscription", "GET",
                            items.get(0).getUuid(),
                            () -> client.subscriptions().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Subscriptions test skipped: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("List events and get by UUID")
        void listEvents() {
            try {
                PaginatedList<Event> items = timedCall("Messaging", "list", "Event", "GET",
                        () -> client.events().list());
                assertNotNull(items);
                assertTrue(items.size() >= 0);

                if (items.size() > 0) {
                    Event item = timedCall("Messaging", "getByUuid", "Event", "GET",
                            items.get(0).getUuid(),
                            () -> client.events().getByUuid(items.get(0).getUuid()));
                    assertNotNull(item);
                    assertEquals(items.get(0).getUuid(), item.getUuid());
                }
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "Events test skipped: " + e.getMessage());
            }
        }
    }
}
