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

package com.eqixiac.equinix.messaging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Package-private wire helpers shared by the EMG models: tolerant enum lookup, null-safe
 * immutable list copies and lenient timestamp parsing.
 */
final class Wire {

    private Wire() {
    }

    /**
     * Resolves a wire value to an enum constant by comparing only letters and digits,
     * case-insensitively, so {@code IN-PROCESS}, {@code In Process} and {@code IN_PROCESS} are the
     * same value and {@code InProgress} matches {@code IN_PROGRESS}.
     *
     * @return {@code null} for a {@code null} value, {@code fallback} for an unrecognised one
     */
    static <E extends Enum<E>> E lookup(Class<E> type, String value, E fallback) {
        if (value == null) {
            return null;
        }
        String wanted = normalise(value);
        for (E constant : type.getEnumConstants()) {
            if (constant != fallback && normalise(constant.name()).equals(wanted)) {
                return constant;
            }
        }
        return fallback;
    }

    private static String normalise(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    /** Unmodifiable copy without {@code null} elements; {@code null} becomes an empty list. */
    static <T> List<T> copy(List<T> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<T> out = new ArrayList<>(list.size());
        for (T item : list) {
            if (item != null) {
                out.add(item);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Parses an ISO-8601 timestamp leniently. The AsyncAPI spec declares {@code date-time} (with
     * offset), but the documented sample notifications carry zone-less values such as
     * {@code 2021-03-29T17:33:00}; EMG documents its timestamps as UTC (release R2023.1), so a
     * zone-less value is read as UTC. Anything unparseable yields {@code null} (the raw JSON is kept
     * on every message, so nothing is lost).
     */
    static Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through: zone-less form
        }
        try {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** Lower-cased, trimmed resource name for routing ({@code null}-safe). */
    static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
