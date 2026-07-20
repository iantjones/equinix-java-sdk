package api.equinix.javasdk.networkedge;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.model.implementation.DeviceManagement;
import api.equinix.javasdk.networkedge.model.implementation.DeviceManagementTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link DeviceManagementTypes#byValue(DeviceManagementType)}.
 *
 * <p>Guards against the switch fall-through regression where both enum values
 * resolved to the SELF-CONFIGURED entry.</p>
 */
class DeviceManagementTypesTest {

    private static final ObjectMapper objectMapper = Constants.mapper();

    private static DeviceManagementTypes managementTypes;

    @BeforeAll
    static void deserializeFixture() throws Exception {
        String json = """
                {
                  "EQUINIX-CONFIGURED": {"type": "EQUINIX-CONFIGURED"},
                  "SELF-CONFIGURED": {"type": "SELF-CONFIGURED"}
                }
                """;
        managementTypes = objectMapper.readValue(json, DeviceManagementTypes.class);
    }

    @Test
    void byValue_equinixConfigured_returnsEquinixConfiguredEntry() {
        DeviceManagement resolved = managementTypes.byValue(DeviceManagementType.EQUINIX_CONFIGURED);
        assertSame(managementTypes.getEquinixConfigured(), resolved);
        assertEquals(DeviceManagementType.EQUINIX_CONFIGURED, resolved.getType());
    }

    @Test
    void byValue_selfConfigured_returnsSelfConfiguredEntry() {
        DeviceManagement resolved = managementTypes.byValue(DeviceManagementType.SELF_CONFIGURED);
        assertSame(managementTypes.getSelfConfigured(), resolved);
        assertEquals(DeviceManagementType.SELF_CONFIGURED, resolved.getType());
    }

    @Test
    void byValue_distinguishesTheTwoEntries() {
        assertNotSame(managementTypes.byValue(DeviceManagementType.EQUINIX_CONFIGURED),
                managementTypes.byValue(DeviceManagementType.SELF_CONFIGURED));
    }
}
