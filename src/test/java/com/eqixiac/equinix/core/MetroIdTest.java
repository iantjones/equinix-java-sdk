package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetroId} and the {@link MetroCode} lookup helpers it builds on.
 */
class MetroIdTest {

    @Test
    @DisplayName("of(String) normalizes case and whitespace")
    void normalizes() {
        assertEquals("SV", MetroId.of(" sv ").code());
        assertEquals(MetroId.of("SV"), MetroId.of("sv"));
        assertEquals(MetroId.of("SV").hashCode(), MetroId.of("sv").hashCode());
    }

    @Test
    @DisplayName("of(String) rejects null and blank")
    void rejectsInvalid() {
        assertThrows(NullPointerException.class, () -> MetroId.of((String) null));
        assertThrows(IllegalArgumentException.class, () -> MetroId.of("   "));
    }

    @Test
    @DisplayName("a known code bridges to its MetroCode")
    void knownCode() {
        MetroId sv = MetroId.of("SV");
        assertTrue(sv.isKnown());
        assertEquals(Optional.of(MetroCode.SV), sv.asMetroCode());
        assertEquals(MetroId.of(MetroCode.SV), sv);
    }

    @Test
    @DisplayName("an unlisted code is still a valid id but has no MetroCode")
    void unknownCode() {
        MetroId zz = MetroId.of("ZZ");
        assertFalse(zz.isKnown());
        assertEquals(Optional.empty(), zz.asMetroCode());
        assertEquals("ZZ", zz.code());
        assertEquals("ZZ", zz.toString());
    }

    @Test
    @DisplayName("MetroCode.lookup is case-insensitive and never returns UNKNOWN as a match")
    void metroCodeLookup() {
        assertEquals(Optional.of(MetroCode.DC), MetroCode.lookup("dc"));
        assertEquals(Optional.empty(), MetroCode.lookup("ZZ"));
        assertEquals(Optional.empty(), MetroCode.lookup(null));
        assertEquals(Optional.empty(), MetroCode.lookup("  "));
        // "UNKNOWN" names the sentinel constant but must not count as a resolved metro.
        assertEquals(Optional.empty(), MetroCode.lookup("UNKNOWN"));
    }

    @Test
    @DisplayName("MetroCode.fromCode falls back to UNKNOWN")
    void metroCodeFromCode() {
        assertEquals(MetroCode.SV, MetroCode.fromCode("SV"));
        assertEquals(MetroCode.UNKNOWN, MetroCode.fromCode("ZZ"));
        assertEquals(MetroCode.UNKNOWN, MetroCode.fromCode(null));
    }
}
