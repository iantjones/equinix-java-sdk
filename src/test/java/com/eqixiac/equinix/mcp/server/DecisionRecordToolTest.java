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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.mcp.server.DesignFixtures.SPEC_TWO_METROS;
import static com.eqixiac.equinix.mcp.server.DesignFixtures.withSpec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("design_decision_record")
class DecisionRecordToolTest {

    private ServerContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = DesignFixtures.context();
    }

    private ObjectNode call(String args) throws Exception {
        return DesignFixtures.call("design_decision_record", context, args);
    }

    @Test
    @DisplayName("returns the ADR Markdown, the status of every section, the fingerprint and provenance")
    void record() throws Exception {
        ObjectNode payload = call(withSpec(SPEC_TWO_METROS, "\"status\": \"Accepted\""));

        assertEquals(64, payload.get("spec_fingerprint").asText().length());
        assertEquals("Accepted", payload.get("status").asText());
        assertFalse(payload.get("recommended").isNull(), payload.toString());
        assertEquals(9, payload.get("sections").size(), "every section reports a status");
        payload.get("sections").forEach(s -> assertTrue(
                s.get("status").asText().matches("included|skipped|failed"), s.toString()));
        payload.get("sections").forEach(s -> assertTrue(
                "included".equals(s.get("status").asText()) || s.hasNonNull("reason"),
                "a skipped or failed section says why: " + s));
        String markdown = payload.get("markdown").asText();
        assertTrue(markdown.startsWith("# "), markdown.substring(0, Math.min(80, markdown.length())));
        assertTrue(markdown.contains("## Decision"));
        assertEquals(0, payload.get("markdown_offset").asInt());
        assertTrue(payload.get("markdown_total_chars").asInt() >= markdown.length());
        assertTrue(payload.has("provenance"));
    }

    @Test
    @DisplayName("the Markdown pages on line ends and the pages join back into the whole document")
    void paging() throws Exception {
        ObjectNode first = call(withSpec(SPEC_TWO_METROS, null));
        String fingerprint = first.get("spec_fingerprint").asText();
        StringBuilder whole = new StringBuilder(first.get("markdown").asText());
        ObjectNode page = first;
        int guard = 0;
        while (!page.get("markdown_next_offset").isNull() && guard++ < 20) {
            page = call("{\"spec_fingerprint\": \"" + fingerprint + "\", \"offset\": "
                    + page.get("markdown_next_offset").asInt() + "}");
            whole.append(page.get("markdown").asText());
        }
        assertEquals(first.get("markdown_total_chars").asInt(), whole.length());

        String text = "line one\n" + "x".repeat(DecisionRecordTools.PAGE_CHARS) + "\nlast";
        assertEquals(9, DecisionRecordTools.pageEnd(text, 0), "a page ends after the last full line that fits");
        assertEquals(text.length(), DecisionRecordTools.pageEnd(text, text.length() - 4));
    }

    @Test
    @DisplayName("an offset past the end and a missing spec are refused with what to do")
    void refusals() {
        assertThrows(IllegalArgumentException.class,
                () -> call(withSpec(SPEC_TWO_METROS, "\"offset\": 99999999")));
        assertThrows(IllegalArgumentException.class, () -> call("{}"));
    }
}
