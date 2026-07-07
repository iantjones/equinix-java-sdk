package api.equinix.javasdk.fabric;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the Fabric {@code SideDeserializer} is wired into the shared {@link Constants#mapper()}
 * via the {@code JacksonModuleProvider} SPI. A default enum deserializer would reject the
 * {@code "aSide"}/{@code "zSide"} wire forms, so success here proves the SPI registration ran.
 */
class SideDeserializationTest {

    @Test
    void deserializesViewPointForms() throws Exception {
        assertEquals(Side.A_Side, Constants.mapper().readValue("\"aSide\"", Side.class));
        assertEquals(Side.Z_Side, Constants.mapper().readValue("\"zSide\"", Side.class));
    }

    @Test
    void deserializesUnderscoreForms() throws Exception {
        assertEquals(Side.A_Side, Constants.mapper().readValue("\"A_Side\"", Side.class));
        assertEquals(Side.Z_Side, Constants.mapper().readValue("\"Z_Side\"", Side.class));
    }

    @Test
    void toViewPointMapsToWireForm() {
        assertEquals("aSide", Side.A_Side.toViewPoint());
        assertEquals("zSide", Side.Z_Side.toViewPoint());
    }
}
