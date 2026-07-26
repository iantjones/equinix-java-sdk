/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.internal.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies {@code MetroCodeDeserializer}'s forward-compatibility behaviour: a metro code the
 * enum does not list must not fail the read.
 */
class MetroCodeDeserializerTest {

    @Test
    @DisplayName("known metro codes deserialize to their constant (case-insensitive)")
    void knownCodes() throws Exception {
        assertEquals(MetroCode.SV, Constants.mapper().readValue("\"SV\"", MetroCode.class));
        assertEquals(MetroCode.DC, Constants.mapper().readValue("\"dc\"", MetroCode.class));
    }

    @Test
    @DisplayName("a new/unknown metro code maps to UNKNOWN instead of crashing the response")
    void unknownCodeMapsToUnknown() throws Exception {
        assertEquals(MetroCode.UNKNOWN, Constants.mapper().readValue("\"ZZ\"", MetroCode.class));
        assertEquals(MetroCode.UNKNOWN,
                Constants.mapper().readValue("\"NEW_METRO_2027\"", MetroCode.class));
        assertDoesNotThrow(() -> Constants.mapper().readValue("\"QQ\"", MetroCode.class));
    }

    @Test
    @DisplayName("an empty-string metro code maps to the UNKNOWN sentinel, not null")
    void emptyCodeMapsToUnknown() throws Exception {
        assertEquals(MetroCode.UNKNOWN, Constants.mapper().readValue("\"\"", MetroCode.class));
    }

    @Test
    @DisplayName("a whitespace-only metro code maps to the UNKNOWN sentinel, not null")
    void whitespaceCodeMapsToUnknown() throws Exception {
        assertEquals(MetroCode.UNKNOWN, Constants.mapper().readValue("\"   \"", MetroCode.class));
        assertEquals(MetroCode.UNKNOWN, Constants.mapper().readValue("\"\\t\"", MetroCode.class));
    }

    @Test
    @DisplayName("an explicit JSON null stays null (absence, not an unknown metro)")
    void jsonNullStaysNull() throws Exception {
        assertNull(Constants.mapper().readValue("null", MetroCode.class));
    }
}
