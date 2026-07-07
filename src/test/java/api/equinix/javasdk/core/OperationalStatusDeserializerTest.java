package api.equinix.javasdk.core;

import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.core.internal.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression tests for the {@code OperationalStatusDeserializer} forward-compatibility guard:
 * a status value the enum does not list must map to {@link OperationalStatus#UNKNOWN} rather
 * than aborting deserialization of the whole payload.
 */
class OperationalStatusDeserializerTest {

    private OperationalStatus read(String json) throws Exception {
        return Constants.mapper().readValue(json, OperationalStatus.class);
    }

    @Test
    void readsKnownStatusesCaseInsensitively() throws Exception {
        assertEquals(OperationalStatus.UP, read("\"UP\""));
        assertEquals(OperationalStatus.UP, read("\"up\""));
        assertEquals(OperationalStatus.DOWN, read("\"Down\""));
        assertEquals(OperationalStatus.PARTIAL, read("\"partial\""));
    }

    @Test
    void unknownStatusMapsToUnknownSentinel() throws Exception {
        assertEquals(OperationalStatus.UNKNOWN, read("\"DEGRADED\""));
        assertEquals(OperationalStatus.UNKNOWN, read("\"PARTIAL_UP\""));
        assertDoesNotThrow(() -> read("\"some-future-status\""));
    }

    @Test
    void blankMapsToNull() throws Exception {
        assertNull(read("\"\""));
        assertNull(read("\"  \""));
    }
}
