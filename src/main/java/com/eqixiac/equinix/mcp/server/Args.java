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

package com.eqixiac.equinix.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Argument-extraction helpers for {@link ToolHandler}s. Failures throw
 * {@link IllegalArgumentException} with a message written for the calling LLM — the adapter
 * turns it into a tool error result the model can correct from.
 */
final class Args {

    private Args() {
    }

    static String requireString(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("'" + field + "' is required and must be a non-empty string.");
        }
        return node.asText().trim();
    }

    static Optional<String> optString(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || node.asText().trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(node.asText().trim());
    }

    static double requireNumber(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || !node.isNumber()) {
            throw new IllegalArgumentException("'" + field + "' is required and must be a number.");
        }
        return node.asDouble();
    }

    static Optional<Double> optNumber(JsonNode args, String field) {
        JsonNode node = args.get(field);
        return node != null && node.isNumber() ? Optional.of(node.asDouble()) : Optional.empty();
    }

    static Optional<Integer> optInt(JsonNode args, String field) {
        JsonNode node = args.get(field);
        return node != null && node.isNumber() ? Optional.of(node.asInt()) : Optional.empty();
    }

    static Optional<Long> optLong(JsonNode args, String field) {
        JsonNode node = args.get(field);
        return node != null && node.isNumber() ? Optional.of(node.asLong()) : Optional.empty();
    }

    static boolean optBool(JsonNode args, String field, boolean fallback) {
        JsonNode node = args.get(field);
        return node != null && node.isBoolean() ? node.asBoolean() : fallback;
    }

    static int limit(JsonNode args, String field, int fallback, int max) {
        int requested = optInt(args, field).orElse(fallback);
        return Math.max(1, Math.min(requested, max));
    }

    static List<String> stringList(JsonNode args, String field) {
        List<String> values = new ArrayList<>();
        JsonNode node = args.get(field);
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual() && !item.asText().trim().isEmpty()) {
                    values.add(item.asText().trim());
                }
            });
        }
        return values;
    }

    /**
     * Parses a case-insensitive enum value, listing the valid options in the failure message.
     */
    static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String field) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + field + "' value '" + raw + "' is not valid. Valid values: "
                    + String.join(", ", Schemas.lowerNames(type)) + ".");
        }
    }

    static <E extends Enum<E>> Optional<E> optEnum(JsonNode args, String field, Class<E> type) {
        return optString(args, field).map(raw -> enumValue(type, raw, field));
    }
}
