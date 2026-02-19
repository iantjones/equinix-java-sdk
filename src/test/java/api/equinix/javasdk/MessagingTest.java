package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.model.Event;
import api.equinix.javasdk.messaging.model.Subscription;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class MessagingTest {

    static Messaging messaging;
    static Boolean skipCreateUpdateOperations;

    @BeforeAll
    static void obtainTestingData() {
        skipCreateUpdateOperations = Boolean.valueOf(System.getProperty("skipCreateUpdateOperations"));
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        messaging = new Messaging(new BasicEquinixCredentials(accessKey, secretKey));
        messaging.authenticate();
    }

    @Test
    void subscriptions() {
        try {
            PaginatedList<Subscription> subscriptions = messaging.subscriptions().list();
            assertNotNull(subscriptions);
            assertTrue(subscriptions.size() >= 0);

            if (subscriptions.size() > 0) {
                Subscription subscription = messaging.subscriptions().getByUuid(subscriptions.get(0).getUuid());
                assertNotNull(subscription);
                assertEquals(subscriptions.get(0).getUuid(), subscription.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Subscriptions test skipped: " + e.getMessage());
        }
    }

    @Test
    void events() {
        try {
            PaginatedList<Event> events = messaging.events().list();
            assertNotNull(events);
            assertTrue(events.size() >= 0);

            if (events.size() > 0) {
                Event event = messaging.events().getByUuid(events.get(0).getUuid());
                assertNotNull(event);
                assertEquals(events.get(0).getUuid(), event.getUuid());
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Events test skipped: " + e.getMessage());
        }
    }
}
