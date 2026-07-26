package com.eqixiac.equinix.ibxsmartview;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.ibxsmartview.enums.ChannelType;
import com.eqixiac.equinix.ibxsmartview.enums.SubscriptionStatus;
import com.eqixiac.equinix.ibxsmartview.model.json.StreamingSubscriptionJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link StreamingSubscriptionJson} (SubscriptionResponse).
 * Loads the streaming_subscription_response.json fixture and verifies all fields including the
 * typed nested messageType and channel.
 */
class StreamingSubscriptionDeserializationTest {

    private static ObjectMapper objectMapper;
    private static StreamingSubscriptionJson subscription;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();

        InputStream is = StreamingSubscriptionDeserializationTest.class.getResourceAsStream("/json/ibxsmartview/streaming_subscription_response.json");
        assertNotNull(is, "streaming_subscription_response.json fixture not found on classpath");

        subscription = objectMapper.readValue(is, StreamingSubscriptionJson.class);
    }

    @Test
    void id_isDeserialized() {
        assertEquals("sub-12345-abcde", subscription.getId());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void orgId_isDeserialized() {
        assertEquals("org-987", subscription.getOrgId());
    }

    @Test
    void createdBy_isDeserialized() {
        assertEquals("user-a", subscription.getCreatedBy());
    }

    @Test
    void updatedBy_isDeserialized() {
        assertEquals("user-b", subscription.getUpdatedBy());
    }

    @Test
    void createdDateTime_isDeserialized() {
        assertEquals("2024-01-10T08:00:00Z", subscription.getCreatedDateTime());
    }

    @Test
    void updatedDateTime_isDeserialized() {
        assertEquals("2024-01-15T12:00:00Z", subscription.getUpdatedDateTime());
    }

    @Test
    void channel_type_isDeserialized() {
        assertNotNull(subscription.getChannel());
        assertEquals(ChannelType.WEBHOOK, subscription.getChannel().getChannelType());
    }

    @Test
    void channel_webhookConfiguration_isDeserialized() {
        assertNotNull(subscription.getChannel().getWebhookChannelConfiguration());
        assertEquals("https://webhook.example.com/events",
                subscription.getChannel().getWebhookChannelConfiguration().getUrl());
        assertEquals(3, subscription.getChannel().getWebhookChannelConfiguration().getNumberOfRetries());
    }

    @Test
    void messageType_environmental_isDeserialized() {
        assertNotNull(subscription.getMessageType());
        assertNotNull(subscription.getMessageType().getEnvironmental());
        assertEquals(1, subscription.getMessageType().getEnvironmental().size());
        assertEquals("123456", subscription.getMessageType().getEnvironmental().get(0).getAccountNumber());
        assertTrue(subscription.getMessageType().getEnvironmental().get(0).getIbx().contains("SV5"));
        assertTrue(subscription.getMessageType().getEnvironmental().get(0).getIbx().contains("DC6"));
    }

    @Test
    void messageType_power_isDeserialized() {
        assertNotNull(subscription.getMessageType().getPower());
        assertEquals(1, subscription.getMessageType().getPower().size());
        assertEquals("123456", subscription.getMessageType().getPower().get(0).getAccountNumber());
        assertEquals("SV5", subscription.getMessageType().getPower().get(0).getIbx().get(0));
    }
}
