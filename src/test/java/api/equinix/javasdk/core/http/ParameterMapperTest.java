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

package api.equinix.javasdk.core.http;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Query-parameter accumulation and wire formatting in {@link ParameterMapper}.
 */
class ParameterMapperTest {

    @Test
    void addAdditionalValue_accumulatesDistinctValuesForOneKey() {
        // The previous implementation seeded new keys with an immutable List.of, so the second
        // distinct value for the same key threw UnsupportedOperationException.
        Map<String, List<String>> params = new HashMap<>();

        ParameterMapper.addAdditionalValue(params, "name", "a");
        ParameterMapper.addAdditionalValue(params, "name", "b");
        ParameterMapper.addAdditionalValue(params, "name", "a"); // duplicate ignored

        assertEquals(List.of("a", "b"), params.get("name"));
    }

    @Test
    void addAdditionalValue_nullValueIsANoOp() {
        Map<String, List<String>> params = new HashMap<>();

        ParameterMapper.addAdditionalValue(params, "name", (String) null);

        assertFalse(params.containsKey("name"));
    }

    @Test
    void addAdditionalValue_copiesOnWriteWhenSeededWithImmutableList() {
        // A key seeded elsewhere (e.g. via singleParamMap -> List.of) must still accumulate.
        Map<String, List<String>> params = new HashMap<>();
        params.put("name", List.of("a"));

        ParameterMapper.addAdditionalValue(params, "name", "b");

        assertEquals(List.of("a", "b"), params.get("name"));
    }

    @Test
    void dateTimeForQuery_convertsLocalWallClockToUtc() {
        // The wire format stamps a 'Z' (UTC designator); the input LocalDateTime is interpreted
        // in the system default zone and must be CONVERTED to UTC, not just labeled as UTC.
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT-5")); // UTC+5, no DST
            String wire = ParameterMapper.dateTimeForQuery(LocalDateTime.of(2026, 7, 7, 12, 0, 0));
            assertEquals("2026-07-07T07:00:00Z", wire);

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String utcWire = ParameterMapper.dateTimeForQuery(LocalDateTime.of(2026, 7, 7, 12, 0, 0));
            assertEquals("2026-07-07T12:00:00Z", utcWire);
        }
        finally {
            TimeZone.setDefault(original);
        }
    }
}
