package api.equinix.javasdk.messaging;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.messaging.model.json.EventJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link EventJson}.
 */
class MessagingEventDeserializationTest {

    private static ObjectMapper objectMapper;
    private static EventJson event;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = MessagingEventDeserializationTest.class
                .getResourceAsStream("/json/messaging/messaging_event_response.json");
        assertNotNull(is, "messaging_event_response.json fixture not found on classpath");
        event = objectMapper.readValue(is, EventJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(event.getUuid());
    }

    @Test
    void type_isDeserialized() {
        assertNotNull(event.getType());
    }

    @Test
    void timestamp_isDeserialized() {
        assertNotNull(event.getTimestamp());
    }

    @Test
    void source_isDeserialized() {
        assertNotNull(event.getSource());
    }
}
