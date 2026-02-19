package api.equinix.javasdk.fabric;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.enums.PortType;
import api.equinix.javasdk.fabric.model.json.PortJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link PortJson}.
 * Loads the port_response.json fixture and verifies all fields.
 */
class PortDeserializationTest {

    private static ObjectMapper objectMapper;
    private static PortJson port;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.objectMapper;

        InputStream is = PortDeserializationTest.class.getResourceAsStream("/json/fabric/port_response.json");
        assertNotNull(is, "port_response.json fixture not found on classpath");

        port = objectMapper.readValue(is, PortJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee", port.getUuid());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("testBuyer-SV5-NL-Dot1q-BO-PRI-10G-JN-154", port.getName());
    }

    @Test
    void type_isDeserialized() {
        assertEquals(PortType.XF_PORT, port.getType());
    }

    @Test
    void state_isDeserialized() {
        assertEquals(PortState.ACTIVE, port.getState());
    }

    @Test
    void bandwidth_isDeserialized() {
        assertEquals(10000, port.getBandwidth());
    }

    @Test
    void usedBandwidth_isDeserialized() {
        assertEquals(2500, port.getUsedBandwidth());
    }

    @Test
    void availableBandwidth_isDeserialized() {
        assertEquals(7500, port.getAvailableBandwidth());
    }

    @Test
    void lagEnabled_isDeserialized() {
        assertNotNull(port.getLagEnabled());
        assertFalse(port.getLagEnabled());
    }

    @Test
    void encapsulation_isDeserialized() {
        assertNotNull(port.getEncapsulation());
    }

    @Test
    void settings_isDeserialized() {
        assertNotNull(port.getSettings());
    }

    @Test
    void location_isDeserialized() {
        assertNotNull(port.getLocation());
    }

    @Test
    void physicalPorts_isDeserialized() {
        assertNotNull(port.getPhysicalPorts());
        assertEquals(1, port.getPhysicalPorts().size());
    }

    @Test
    void redundancy_isDeserialized() {
        assertNotNull(port.getRedundancy());
        assertEquals("m167f685-41b0-1b07-6de0-3a7c54b08b8f", port.getRedundancy().getGroup());
    }

    @Test
    void account_isDeserialized() {
        assertNotNull(port.getAccount());
    }

    @Test
    void changeLog_isDeserialized() {
        assertNotNull(port.getChangeLog());
        assertEquals("testuser", port.getChangeLog().getCreatedBy());
        assertNotNull(port.getChangeLog().getCreatedDateTime());
    }

    @Test
    void projectId_isDeserialized() {
        assertEquals("proj-abc-123", port.getProjectId());
    }

    @Test
    void device_isDeserialized() {
        assertNotNull(port.getDevice());
    }
}
