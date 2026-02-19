package api.equinix.javasdk.fabric;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link ServiceTokenJson}.
 * Loads the service_token_response.json fixture and verifies all fields.
 */
class ServiceTokenDeserializationTest {

    private static ObjectMapper objectMapper;
    private static ServiceTokenJson serviceToken;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream is = ServiceTokenDeserializationTest.class.getResourceAsStream("/json/fabric/service_token_response.json");
        assertNotNull(is, "service_token_response.json fixture not found on classpath");

        serviceToken = objectMapper.readValue(is, ServiceTokenJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertNotNull(serviceToken.getUuid());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(ServiceTokenType.VC_TOKEN, serviceToken.getType());
    }

    @Test
    void state_isDeserialized() {
        assertEquals(ServiceTokenState.ACTIVE, serviceToken.getState());
    }

    @Test
    void href_isDeserialized() {
        assertNotNull(serviceToken.getHref());
    }

    @Test
    void expiry_isDeserialized() {
        assertEquals(30, serviceToken.getExpiry());
    }

    @Test
    void expirationDateTime_isDeserialized() {
        assertNotNull(serviceToken.getExpirationDateTime());
        assertEquals(2024, serviceToken.getExpirationDateTime().getYear());
        assertEquals(3, serviceToken.getExpirationDateTime().getMonthValue());
        assertEquals(15, serviceToken.getExpirationDateTime().getDayOfMonth());
    }

    @Test
    void connection_isDeserialized() {
        assertNotNull(serviceToken.getConnection());
        assertTrue(serviceToken.getConnection().getAllowRemoteConnection());
        assertTrue(serviceToken.getConnection().getAllowCustomBandwidth());
        assertEquals(500, serviceToken.getConnection().getBandwidthLimit());
    }

    @Test
    void notifications_isDeserialized() {
        assertNotNull(serviceToken.getNotifications());
        assertEquals(1, serviceToken.getNotifications().size());
        assertEquals(2, serviceToken.getNotifications().get(0).getEmails().size());
        assertTrue(serviceToken.getNotifications().get(0).getEmails().contains("admin@example.com"));
        assertTrue(serviceToken.getNotifications().get(0).getEmails().contains("ops@example.com"));
    }

    @Test
    void account_isDeserialized() {
        assertNotNull(serviceToken.getAccount());
    }

    @Test
    void changeLog_isDeserialized() {
        assertNotNull(serviceToken.getChangeLog());
        assertEquals("tokenCreator", serviceToken.getChangeLog().getCreatedBy());
        assertNotNull(serviceToken.getChangeLog().getCreatedDateTime());
    }
}
