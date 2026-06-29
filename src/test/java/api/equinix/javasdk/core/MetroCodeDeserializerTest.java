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

package api.equinix.javasdk.core;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.internal.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@code MetroCodeDeserializer}'s forward-compatibility behaviour: a metro code the
 * enum does not list must not fail the read.
 */
class MetroCodeDeserializerTest {

    @Test
    @DisplayName("known metro codes deserialize to their constant (case-insensitive)")
    void knownCodes() throws Exception {
        assertEquals(MetroCode.SV, Constants.objectMapper.readValue("\"SV\"", MetroCode.class));
        assertEquals(MetroCode.DC, Constants.objectMapper.readValue("\"dc\"", MetroCode.class));
    }

    @Test
    @DisplayName("a new/unknown metro code maps to UNKNOWN instead of crashing the response")
    void unknownCodeMapsToUnknown() throws Exception {
        assertEquals(MetroCode.UNKNOWN, Constants.objectMapper.readValue("\"ZZ\"", MetroCode.class));
        assertEquals(MetroCode.UNKNOWN,
                Constants.objectMapper.readValue("\"NEW_METRO_2027\"", MetroCode.class));
        assertDoesNotThrow(() -> Constants.objectMapper.readValue("\"QQ\"", MetroCode.class));
    }
}
