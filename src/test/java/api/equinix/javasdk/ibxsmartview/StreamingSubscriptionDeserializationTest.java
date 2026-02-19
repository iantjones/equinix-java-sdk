package api.equinix.javasdk.ibxsmartview;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.ibxsmartview.enums.ChannelType;
import api.equinix.javasdk.ibxsmartview.enums.StreamingMessageType;
import api.equinix.javasdk.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link StreamingSubscriptionJson}.
 * Loads the streaming_subscription_response.json fixture and verifies all fields
 * including nested channel and messages list.
 */
class StreamingSubscriptionDeserializationTest {

    private static ObjectMapper objectMapper;
    private static StreamingSubscriptionJson subscription;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream is = StreamingSubscriptionDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/streaming_subscription_response.json");
        assertNotNull(is, "streaming_subscription_response.json fixture not found on classpath");

        subscription = objectMapper.readValue(is, StreamingSubscriptionJson.class);
    }

    @Test
    void subscriptionId_isDeserialized() {
        assertEquals("sub-12345-abcde", subscription.getSubscriptionId());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("My Environment Subscription", subscription.getName());
    }

    @Test
    void description_isDeserialized() {
        assertEquals("Subscription for environmental data streaming", subscription.getDescription());
    }

    @Test
    void status_isDeserialized() {
        assertEquals("ACTIVE", subscription.getStatus());
    }

    @Test
    void channel_isDeserialized() {
        assertNotNull(subscription.getChannel());
        assertEquals(ChannelType.WEBHOOK, subscription.getChannel().getChannelType());
    }

    @Test
    void channel_details_isDeserialized() {
        assertNotNull(subscription.getChannel().getChannelDetails());
        assertEquals("https://webhook.example.com/events",
                subscription.getChannel().getChannelDetails().get("endpoint"));
    }

    @Test
    void messages_isDeserialized() {
        assertNotNull(subscription.getMessages());
        assertEquals(2, subscription.getMessages().size());
    }

    @Test
    void messages_firstMessage_messageType() {
        assertEquals(StreamingMessageType.ENVIRONMENTAL,
                subscription.getMessages().get(0).getMessageType());
    }

    @Test
    void messages_firstMessage_accountNumbers() {
        assertEquals(2, subscription.getMessages().get(0).getAccountNumbers().size());
        assertTrue(subscription.getMessages().get(0).getAccountNumbers().contains("123456"));
        assertTrue(subscription.getMessages().get(0).getAccountNumbers().contains("789012"));
    }

    @Test
    void messages_firstMessage_ibxs() {
        assertEquals(2, subscription.getMessages().get(0).getIbxs().size());
        assertTrue(subscription.getMessages().get(0).getIbxs().contains("SV5"));
        assertTrue(subscription.getMessages().get(0).getIbxs().contains("DC6"));
    }

    @Test
    void messages_secondMessage_messageType() {
        assertEquals(StreamingMessageType.POWER,
                subscription.getMessages().get(1).getMessageType());
    }

    @Test
    void messages_secondMessage_accountNumbers() {
        assertEquals(1, subscription.getMessages().get(1).getAccountNumbers().size());
        assertEquals("123456", subscription.getMessages().get(1).getAccountNumbers().get(0));
    }

    @Test
    void messages_secondMessage_ibxs() {
        assertEquals(1, subscription.getMessages().get(1).getIbxs().size());
        assertEquals("SV5", subscription.getMessages().get(1).getIbxs().get(0));
    }

    @Test
    void createdDate_isDeserialized() {
        assertEquals("2024-01-10T08:00:00Z", subscription.getCreatedDate());
    }

    @Test
    void updatedDate_isDeserialized() {
        assertEquals("2024-01-15T12:00:00Z", subscription.getUpdatedDate());
    }
}
