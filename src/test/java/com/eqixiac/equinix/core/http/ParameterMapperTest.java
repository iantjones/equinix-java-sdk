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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
    void dateTimeForQuery_formatsVerbatimRegardlessOfJvmZone() {
        // SDK-wide UTC policy: LocalDateTime inputs are UTC wall clock (matching the
        // deserialization convention), so the digits are stamped with a literal 'Z' with NO zone
        // conversion. A briefly-shipped variant converted systemDefault -> UTC, which
        // double-shifted every API-sourced timestamp by the host's UTC offset.
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT-5")); // UTC+5, no DST
            assertEquals("2026-07-07T12:00:00Z",
                    ParameterMapper.dateTimeForQuery(LocalDateTime.of(2026, 7, 7, 12, 0, 0)));

            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+8")); // UTC-8, no DST
            assertEquals("2026-07-07T12:00:00Z",
                    ParameterMapper.dateTimeForQuery(LocalDateTime.of(2026, 7, 7, 12, 0, 0)));

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals("2026-07-07T12:00:00Z",
                    ParameterMapper.dateTimeForQuery(LocalDateTime.of(2026, 7, 7, 12, 0, 0)));
        }
        finally {
            TimeZone.setDefault(original);
        }
    }

    /** Test holder mirroring how every SDK model wires the core timestamp deserializer. */
    static class TimestampHolder {
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        public LocalDateTime createdDateTime;
    }

    @Test
    void dateTimeForQuery_roundTripsDeserializedTimestampsUnchanged() throws Exception {
        // Regression: an API-sourced timestamp (e.g. changeLog.getCreatedDateTime()) passed back
        // into a query (e.g. getStatistics) must hit the wire with the SAME instant. The core
        // deserializer parses "...T12:00:00Z" into bare LocalDateTime 12:00 (UTC wall clock), so
        // re-serialization must be verbatim — independent of the JVM's default zone.
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT-5")); // fixed non-UTC zone, no DST

            TimestampHolder holder = Constants.mapper()
                    .readValue("{\"createdDateTime\":\"2026-07-07T12:00:00Z\"}", TimestampHolder.class);
            assertEquals(LocalDateTime.of(2026, 7, 7, 12, 0, 0), holder.createdDateTime);

            assertEquals("2026-07-07T12:00:00Z", ParameterMapper.dateTimeForQuery(holder.createdDateTime));
        }
        finally {
            TimeZone.setDefault(original);
        }
    }
}
