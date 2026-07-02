package api.equinix.javasdk.fabric;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.ConnectionState;
import api.equinix.javasdk.fabric.enums.ConnectionStatus;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.LinkProtocolType;
import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.implementation.ServiceToken;
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
        assertEquals("PO-12345", connection.getOrder().getPurchaseOrderNumber());
        assertEquals("CR-98765", connection.getOrder().getCustomerReferenceNumber());
        assertEquals("1-323292", connection.getOrder().getOrderId());
        assertEquals("1-323333", connection.getOrder().getOrderNumber());
        assertEquals(24, connection.getOrder().getTermLength());
        assertEquals("Up to 1 Gbps", connection.getOrder().getBillingTier());
    }

    @Test
    void zSide_serviceToken_isDeserialized() {
        ServiceToken token = connection.getZSide().getServiceToken();
        assertNotNull(token);
        assertEquals("e05f4e14-cf3e-44b7-b0a6-085fcc93e2ea", token.getUuid());
        assertEquals("https://api.equinix.com/fabric/v4/serviceTokens/e05f4e14-cf3e-44b7-b0a6-085fcc93e2ea",
                token.getHref());
        assertEquals(ServiceTokenType.VC_TOKEN, token.getType());
        assertEquals(ServiceTokenState.ACTIVE, token.getState());
        assertEquals("Zside-Token-01", token.getName());
        assertEquals("Z-side token this connection was redeemed from", token.getDescription());
        assertEquals("z", token.getIssuerSide());
        assertEquals(30, token.getExpiry());
        assertNotNull(token.getExpirationDateTime());
        assertEquals(2024, token.getExpirationDateTime().getYear());
        assertEquals(6, token.getExpirationDateTime().getMonthValue());
        assertEquals(30, token.getExpirationDateTime().getDayOfMonth());
    }

    // ConnectionStatus is exercised through Jackson (the @JsonCreator wire path) directly:
    // ConnectionOperation deserializes these fields but currently exposes no getters.

    @Test
    void connectionStatus_draft_isReadAsDraft() throws Exception {
        assertEquals(ConnectionStatus.DRAFT,
                objectMapper.readValue("\"DRAFT\"", ConnectionStatus.class));
    }

    @Test
    void connectionStatus_cancelled_isReadAsCancelled() throws Exception {
        assertEquals(ConnectionStatus.CANCELLED,
                objectMapper.readValue("\"CANCELLED\"", ConnectionStatus.class));
    }

    @Test
    void connectionStatus_pendingAutoApproval_isReadAsPendingAutoApproval() throws Exception {
        assertEquals(ConnectionStatus.PENDING_AUTO_APPROVAL,
                objectMapper.readValue("\"PENDING_AUTO_APPROVAL\"", ConnectionStatus.class));
    }

    @Test
    void connectionStatus_unknownValue_fallsBackToUnknownNotNull() throws Exception {
        ConnectionStatus status = objectMapper.readValue("\"SOME_FUTURE_STATUS\"", ConnectionStatus.class);
        assertNotNull(status);
        assertEquals(ConnectionStatus.UNKNOWN, status);
    }

    @Test
    void connectionStatus_notApplicableWireValue_isReadAsNotApplicable() throws Exception {
        assertEquals(ConnectionStatus.NOT_APPLICABLE,
                objectMapper.readValue("\"N/A\"", ConnectionStatus.class));
    }

    @Test
    void linkProtocolType_vxlan_isReadAsVxlan() throws Exception {
        LinkProtocol linkProtocol = objectMapper.readValue(
                "{\"type\":\"VXLAN\",\"vni\":12345}", LinkProtocol.class);
        assertEquals(LinkProtocolType.VXLAN, linkProtocol.getType());
    }

    @Test
    void linkProtocolType_evpnVxlan_isReadAsEvpnVxlan() throws Exception {
        LinkProtocol linkProtocol = objectMapper.readValue(
                "{\"type\":\"EVPN_VXLAN\"}", LinkProtocol.class);
        assertEquals(LinkProtocolType.EVPN_VXLAN, linkProtocol.getType());
    }

    @Test
    void linkProtocolType_unknownValue_fallsBackToUnknownNotNull() throws Exception {
        LinkProtocol linkProtocol = objectMapper.readValue(
                "{\"type\":\"SOME_FUTURE_PROTOCOL\"}", LinkProtocol.class);
        assertNotNull(linkProtocol.getType());
        assertEquals(LinkProtocolType.UNKNOWN, linkProtocol.getType());
    }
}
