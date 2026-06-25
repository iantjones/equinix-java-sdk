package api.equinix.javasdk.messaging;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.messaging.model.json.SubscriptionJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link SubscriptionJson}.
 */
class MessagingSubscriptionDeserializationTest {

    private static ObjectMapper objectMapper;
    private static SubscriptionJson subscription;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = MessagingSubscriptionDeserializationTest.class
                .getResourceAsStream("/json/messaging/messaging_subscription_response.json");
        assertNotNull(is, "messaging_subscription_response.json fixture not found on classpath");
        subscription = objectMapper.readValue(is, SubscriptionJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(subscription.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertNotNull(subscription.getName());
    }

    @Test
    void type_isDeserialized() {
        assertNotNull(subscription.getType());
    }

    @Test
    void status_isDeserialized() {
        assertNotNull(subscription.getStatus());
    }

    @Test
    void eventTypes_isDeserialized() {
        assertNotNull(subscription.getEventTypes());
        assertFalse(subscription.getEventTypes().isEmpty());
    }
}
