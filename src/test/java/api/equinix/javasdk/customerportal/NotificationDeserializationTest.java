package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.customerportal.enums.NotificationStatus;
import api.equinix.javasdk.customerportal.enums.NotificationType;
import api.equinix.javasdk.core.internal.Constants;
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
    void id_isDeserialized() {
        assertEquals("5-122719992195", notification.getId());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(NotificationType.IBX_MAINTENANCE, notification.getType());
    }

    @Test
    void startTimestamp_isDeserialized() {
        assertEquals("2024-11-12T18:00:00.000Z", notification.getStartTimestamp());
    }

    @Test
    void endTimestamp_isDeserialized() {
        assertEquals("2024-11-13T02:00:00.000Z", notification.getEndTimestamp());
    }

    @Test
    void ibxs_areDeserialized() {
        assertNotNull(notification.getIbxs());
        assertEquals(2, notification.getIbxs().size());
        assertTrue(notification.getIbxs().contains("SV5"));
    }

    @Test
    void status_isDeserialized() {
        assertEquals(NotificationStatus.NEW, notification.getStatus());
    }

    @Test
    void summary_isDeserialized() {
        assertEquals("Scheduled power maintenance in SV5. Redundant power feeds will remain active.",
                notification.getSummary());
    }

    @Test
    void emails_areDeserialized() {
        assertNotNull(notification.getEmails());
        assertEquals(1, notification.getEmails().size());
        assertEquals("SV METRO AREA MAINTENANCE NOTIFICATION - [5-122719992195]",
                notification.getEmails().get(0).getSubject());
        assertEquals("2024-11-12T18:05:00.000Z", notification.getEmails().get(0).getTimestamp());
    }
}
