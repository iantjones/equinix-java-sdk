package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.enums.BandwidthUnit;
import com.eqixiac.equinix.core.internal.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression tests for the {@code BandwidthDeserializer} normalization: case-folding must happen
 * before short-form mapping (the old replace-then-uppercase chain corrupted already-canonical
 * values like {@code "MBPS"} into {@code "MBPSPS"} and missed lowercase short forms entirely),
 * and an unknown unit must not crash the whole response.
 */
class BandwidthDeserializerTest {

    private BandwidthUnit read(String json) throws Exception {
        return Constants.mapper().readValue(json, BandwidthUnit.class);
    }

    @Test
    void readsCanonicalValuesInAnyCase() throws Exception {
        assertEquals(BandwidthUnit.MBPS, read("\"MBPS\""));
        assertEquals(BandwidthUnit.MBPS, read("\"Mbps\""));
        assertEquals(BandwidthUnit.MBPS, read("\"mbps\""));
        assertEquals(BandwidthUnit.GBPS, read("\"GBPS\""));
        assertEquals(BandwidthUnit.GBPS, read("\"Gbps\""));
        assertEquals(BandwidthUnit.TBPS, read("\"Tbps\""));
        assertEquals(BandwidthUnit.PBPS, read("\"PBPS\""));
    }

    @Test
    void readsShortFormsInAnyCase() throws Exception {
        assertEquals(BandwidthUnit.MBPS, read("\"MB\""));
        assertEquals(BandwidthUnit.MBPS, read("\"mb\""));
        assertEquals(BandwidthUnit.GBPS, read("\"GB\""));
        assertEquals(BandwidthUnit.GBPS, read("\"gb\""));
        assertEquals(BandwidthUnit.TBPS, read("\"tb\""));
        assertEquals(BandwidthUnit.PBPS, read("\"Pb\""));
    }

    @Test
    void trimsWhitespace() throws Exception {
        assertEquals(BandwidthUnit.GBPS, read("\" Gbps \""));
    }

    @Test
    void unknownUnitMapsToNullInsteadOfCrashing() throws Exception {
        assertNull(read("\"EXABYTES\""));
        assertNull(read("\"\""));
        assertNull(read("\"   \""));
    }
}
