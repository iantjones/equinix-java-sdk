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

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wire timestamp parsing: every fractional-second width from 0 to 9 digits, every offset
 * spelling, and normalisation of offset-bearing values to UTC.
 */
class LocalDateTimeDeserializerTest {

    static class Holder {
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        public LocalDateTime value;
    }

    private static LocalDateTime parse(String wire) throws Exception {
        return Constants.mapper().readValue("{\"value\":\"" + wire + "\"}", Holder.class).value;
    }

    @ParameterizedTest
    @CsvSource({
            // Fabric v4 4.34.0 example (POST /streams/{id}/alertRules): 9 fractional digits.
            "2025-02-04T00:20:54.511118575Z, 2025-02-04T00:20:54.511118575",
            "2025-02-04T00:20:54Z,           2025-02-04T00:20:54",
            "2025-02-04T00:20:54.5Z,         2025-02-04T00:20:54.5",
            "2025-02-04T00:20:54.51Z,        2025-02-04T00:20:54.51",
            "2025-02-04T00:20:54.511Z,       2025-02-04T00:20:54.511",
            "2025-02-04T00:20:54.5111Z,      2025-02-04T00:20:54.5111",
            "2025-02-04T00:20:54.51111Z,     2025-02-04T00:20:54.51111",
            "2025-02-04T00:20:54.511111Z,    2025-02-04T00:20:54.511111",
            "2025-02-04T00:20:54.5111111Z,   2025-02-04T00:20:54.5111111",
            "2025-02-04T00:20:54.51111111Z,  2025-02-04T00:20:54.51111111",
            "2025-02-04T00:20:54.511,        2025-02-04T00:20:54.511",
            "2025-02-04 00:20:54,            2025-02-04T00:20:54",
            "2025-02-04T00:20:54+00:00,      2025-02-04T00:20:54",
            "2025-02-04T00:20:54.123+0000,   2025-02-04T00:20:54.123",
            "2025-02-04T02:20:54+02:00,      2025-02-04T00:20:54",
            "2025-02-04T02:20:54+02,         2025-02-04T00:20:54",
            "2025-02-03T19:20:54.25-05:00,   2025-02-04T00:20:54.25",
            "03/04/2021 14:00:00,            2021-03-04T14:00:00",
            "Wed Mar 03 2021 14:00:00 GMT,   2021-03-03T14:00:00",
    })
    void parsesAndNormalisesToUtc(String wire, String expected) throws Exception {
        assertEquals(LocalDateTime.parse(expected.trim()), parse(wire.trim()));
    }
}
