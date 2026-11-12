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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test fixtures under {@code src/test/resources/json/messaging}.
 * <ul>
 *   <li>{@code docs-*-sample.json}: the complete sample notifications from the Notification Types
 *       guide (docs.equinix.com/incidents-notifications/messaging-gateway/notification-types, fetched
 *       2026-11-11), verbatim except for the blank lines, trailing spaces and non-breaking spaces (U+00A0, used as indentation) the docs renderer inserts.</li>
 *   <li>{@code spec-*-body.json}: the {@code example} of each body schema in the AsyncAPI spec
 *       {@code emgv1.yaml}, verbatim. The spec only has body-level examples, so tests wrap them in an
 *       envelope with {@link #envelope}.</li>
 * </ul>
 */
final class Fixtures {

    static final ObjectMapper JSON = new ObjectMapper();

    private Fixtures() {
    }

    static String read(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/json/messaging/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("missing fixture " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Wraps a spec body example in a minimal envelope with the given resource and version. */
    static String envelope(String resource, String version, String bodyFixture) {
        try {
            JsonNode body = JSON.readTree(read(bodyFixture));
            ObjectNode root = JSON.createObjectNode();
            ObjectNode task = root.putObject("Task");
            task.put("Id", "test-" + bodyFixture);
            task.put("Source", "3e095d30-40ff-11e9-8959-5be078353003");
            task.put("Verb", "Update");
            task.put("Resource", resource);
            task.put("ContentType", "application/json");
            task.put("CreationTimeUTC", "2025-09-09T10:00:00Z");
            task.put("Version", version);
            task.set("Body", body);
            root.put("Signature", "c2ln");
            return JSON.writeValueAsString(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
