package api.equinix.javasdk.fabric;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.ConnectionState;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link ConnectionJson}.
 * Loads the connection_response.json fixture and verifies all top-level
 * and nested fields deserialize correctly.
 */
class ConnectionDeserializationTest {

    private static ObjectMapper objectMapper;
    private static ConnectionJson connection;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream is = ConnectionDeserializationTest.class.getResourceAsStream("/json/fabric/connection_response.json");
        assertNotNull(is, "connection_response.json fixture not found on classpath");

        connection = objectMapper.readValue(is, ConnectionJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connection.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("My-EVPL-Connection", connection.getName());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(ConnectionType.EVPL_VC, connection.getType());
    }

    @Test
    void bandwidth_isDeserialized() {
        assertEquals(1000, connection.getBandwidth());
    }

    @Test
    void state_isDeserialized() {
        assertEquals(ConnectionState.ACTIVE, connection.getState());
    }

    @Test
    void isRemote_isDeserialized() {
        assertNotNull(connection.getIsRemote());
        assertFalse(connection.getIsRemote());
    }

    @Test
    void aSide_accessPoint_isDeserialized() {
        assertNotNull(connection.getASide());
        assertNotNull(connection.getASide().getAccessPoint());
        assertNotNull(connection.getASide().getAccessPoint().getPort());
        assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                connection.getASide().getAccessPoint().getPort().getUuid());
    }

    @Test
    void aSide_linkProtocol_isDeserialized() {
        assertNotNull(connection.getASide().getAccessPoint().getLinkProtocol());
        assertEquals(1001, connection.getASide().getAccessPoint().getLinkProtocol().getVlanTag());
    }

    @Test
    void zSide_accessPoint_isDeserialized() {
        assertNotNull(connection.getZSide());
        assertNotNull(connection.getZSide().getAccessPoint());
        assertNotNull(connection.getZSide().getAccessPoint().getProfile());
        assertEquals("20d32a80-0d61-4333-bc03-4b2d446794a0",
                connection.getZSide().getAccessPoint().getProfile().getUuid());
    }

    @Test
    void zSide_linkProtocol_isDeserialized() {
        assertNotNull(connection.getZSide().getAccessPoint().getLinkProtocol());
        assertEquals(1002, connection.getZSide().getAccessPoint().getLinkProtocol().getVlanTag());
    }

    @Test
    void operation_isDeserialized() {
        assertNotNull(connection.getOperation());
    }

    @Test
    void changeLog_isDeserialized() {
        assertNotNull(connection.getChangeLog());
        assertEquals("testuser", connection.getChangeLog().getCreatedBy());
        assertEquals("Test User", connection.getChangeLog().getCreatedByFullName());
        assertNotNull(connection.getChangeLog().getCreatedDateTime());
    }

    @Test
    void redundancy_isDeserialized() {
        assertNotNull(connection.getRedundancy());
        assertEquals("m167f685-41b0-1b07-6de0-3a7c54b08b8f", connection.getRedundancy().getGroup());
    }

    @Test
    void notifications_isDeserialized() {
        assertNotNull(connection.getNotifications());
        assertFalse(connection.getNotifications().isEmpty());
        assertEquals(1, connection.getNotifications().size());
        assertEquals("testuser@example.com", connection.getNotifications().get(0).getEmails().get(0));
    }

    @Test
    void account_isDeserialized() {
        assertNotNull(connection.getAccount());
    }

    @Test
    void order_isDeserialized() {
        assertNotNull(connection.getOrder());
    }
}
