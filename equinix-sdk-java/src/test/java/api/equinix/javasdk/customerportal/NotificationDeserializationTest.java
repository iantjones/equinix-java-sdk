package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.NotificationCategory;
import api.equinix.javasdk.customerportal.model.json.NotificationJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeserializationTest {

    private static ObjectMapper objectMapper;
    private static NotificationJson notification;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;
        InputStream is = NotificationDeserializationTest.class.getResourceAsStream("/json/customerportal/notification_response.json");
        assertNotNull(is, "notification_response.json fixture not found on classpath");
        notification = objectMapper.readValue(is, NotificationJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("a7b8c9d0-e1f2-4a3b-4c5d-6e7f80910213", notification.getUuid());
    }

    @Test
    void type_isDeserialized() {
        assertEquals("MAINTENANCE", notification.getType());
    }

    @Test
    void category_isDeserialized() {
        assertEquals(NotificationCategory.MAINTENANCE, notification.getCategory());
    }

    @Test
    void message_isDeserialized() {
        assertEquals("Scheduled power maintenance in SV5 on 2024-12-20 from 02:00-06:00 UTC. Redundant power feeds will remain active. No customer impact expected.", notification.getMessage());
    }

    @Test
    void timestamp_isDeserialized() {
        assertEquals("2024-11-12T18:00:00.000Z", notification.getTimestamp());
    }

    @Test
    void read_isDeserialized() {
        assertNotNull(notification.getRead());
        assertFalse(notification.getRead());
    }
}
