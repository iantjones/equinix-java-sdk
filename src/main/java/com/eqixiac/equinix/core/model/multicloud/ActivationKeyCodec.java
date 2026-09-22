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

package com.eqixiac.equinix.core.model.multicloud;

import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Base64 + JSON codec behind {@link ActivationKey}. Package-private: the public surface is
 * {@link ActivationKey#decode(String)}, {@link ActivationKey#encode()} and
 * {@link ActivationKey.V1#builder()}.
 *
 * <p>JSON member names are those of {@code environment.yaml#/ActivationKey},
 * {@code #/ActivationKeyV1} and {@code #/ActivationKeyV2} at specification commit
 * {@code bbfc763}.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ActivationKeyCodec {

    static final String VERSION = "version";
    static final String DESTINATION_ENVIRONMENT_URI = "destinationEnvironmentUri";
    static final String SHARED_CONNECTION_UUID = "sharedConnectionUuid";
    static final String CONNECTION_SIZE_MBPS = "connectionSizeMbps";
    static final String DESTINATION_ACCOUNT_ID = "destinationAccountId";
    static final String ENCRYPTED_CONTENTS = "encryptedContents";

    // Derived from the shared wire mapper (an ObjectReader is immutable; the shared mapper itself is
    // never reconfigured). Trailing content and duplicate member names make a payload ambiguous:
    // two parsers could read two different account ids from one key. Such a payload is not
    // interpreted; it becomes Opaque.
    private static final ObjectReader STRICT_TREE_READER = Constants.mapper().reader()
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    static ActivationKey decode(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        String raw = encoded.strip();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("activation key must not be blank");
        }

        String compact = removeAsciiWhitespace(raw);
        byte[] payload = base64Decode(compact);
        if (payload == null) {
            return new ActivationKey.Opaque(raw, null, null);
        }

        JsonNode root;
        try {
            root = STRICT_TREE_READER.readTree(payload);
        } catch (IOException | RuntimeException e) {
            return new ActivationKey.Opaque(raw, null, null);
        }
        if (root == null || !root.isObject()) {
            return new ActivationKey.Opaque(raw, null, null);
        }

        Integer version = intOrNull(root.get(VERSION));
        String uri = textOrNull(root.get(DESTINATION_ENVIRONMENT_URI));

        if (version != null && uri != null) {
            if (version == ActivationKey.V1.VERSION) {
                String uuid = textOrNull(root.get(SHARED_CONNECTION_UUID));
                Integer sizeMbps = intOrNull(root.get(CONNECTION_SIZE_MBPS));
                String account = textOrNull(root.get(DESTINATION_ACCOUNT_ID));
                if (uuid != null && account != null && sizeMbps != null && sizeMbps > 0) {
                    return new ActivationKey.V1(uri, uuid, sizeMbps, account, compact);
                }
            } else if (version == ActivationKey.V2Encrypted.VERSION) {
                if (textOrNull(root.get(ENCRYPTED_CONTENTS)) != null) {
                    return new ActivationKey.V2Encrypted(uri, compact);
                }
            }
        }
        return new ActivationKey.Opaque(raw, version, uri);
    }

    /**
     * Canonical version 1 encoding: compact UTF-8 JSON, members in schema order (envelope first),
     * RFC 4648 section 4 alphabet with padding.
     */
    static String canonicalV1(String destinationEnvironmentUri, String sharedConnectionUuid,
                              int connectionSizeMbps, String destinationAccountId) {
        // ObjectNode keeps insertion order, so the member order below is the serialized order.
        ObjectNode node = Constants.mapper().createObjectNode();
        node.put(VERSION, ActivationKey.V1.VERSION);
        node.put(DESTINATION_ENVIRONMENT_URI, destinationEnvironmentUri);
        node.put(SHARED_CONNECTION_UUID, sharedConnectionUuid);
        node.put(CONNECTION_SIZE_MBPS, connectionSizeMbps);
        node.put(DESTINATION_ACCOUNT_ID, destinationAccountId);
        try {
            byte[] json = Constants.mapper().writeValueAsString(node).getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(json);
        } catch (JsonProcessingException e) {
            // Unreachable for an ObjectNode of strings and ints. The message carries no member value.
            throw new IllegalStateException("could not serialize a version 1 activation key", e);
        }
    }

    private static byte[] base64Decode(String compact) {
        try {
            return Base64.getDecoder().decode(compact);
        } catch (IllegalArgumentException standardAlphabetRejected) {
            try {
                return Base64.getUrlDecoder().decode(compact);
            } catch (IllegalArgumentException urlAlphabetRejected) {
                return null;
            }
        }
    }

    private static String removeAsciiWhitespace(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static Integer intOrNull(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() && !node.textValue().isBlank() ? node.textValue() : null;
    }
}
