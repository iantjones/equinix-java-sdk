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

package api.equinix.javasdk.mcp.server.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Canonical-form JSON and its SHA-256, the binding between a proposal and its confirm token.
 *
 * <p>Canonicalization sorts object keys recursively and serializes compactly, so two specs
 * that differ only in key order or whitespace hash identically, while any change to a value —
 * a bandwidth, a UUID, an email — produces a different hash and therefore a different
 * proposal. Array order is significant (it is significant to the API too).</p>
 */
final class SpecHash {

    private SpecHash() {
    }

    /**
     * Renders the spec in canonical form: recursively key-sorted objects, compact output.
     */
    static String canonicalize(JsonNode spec, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(sortNode(spec, mapper));
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Spec canonicalization failed", e);
        }
    }

    /**
     * @return the lower-case hex SHA-256 of the canonical form
     */
    static String sha256Hex(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable in this JVM", e);
        }
    }

    private static JsonNode sortNode(JsonNode node, ObjectMapper mapper) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            ObjectNode sorted = mapper.createObjectNode();
            for (String name : names) {
                sorted.set(name, sortNode(node.get(name), mapper));
            }
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = mapper.createArrayNode();
            node.forEach(item -> sorted.add(sortNode(item, mapper)));
            return sorted;
        }
        return node;
    }
}
