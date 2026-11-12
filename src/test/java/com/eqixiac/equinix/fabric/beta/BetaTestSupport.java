/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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
package com.eqixiac.equinix.fabric.beta;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Shared helpers for the Fabric Beta tests. */
final class BetaTestSupport {

    private BetaTestSupport() {
    }

    static <T> T read(String json, Class<T> type) throws Exception {
        return Constants.mapper().readValue(json, type);
    }

    /** The JSON with every null-valued member removed (the SDK never sends nulls). */
    static String withoutNulls(String json) throws Exception {
        JsonNode n = Constants.mapper().readTree(json);
        strip(n);
        return Constants.mapper().writeValueAsString(n);
    }

    private static void strip(JsonNode n) {
        if (n.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getValue().isNull()) {
                    it.remove();
                } else {
                    strip(e.getValue());
                }
            }
        } else if (n.isArray()) {
            n.forEach(BetaTestSupport::strip);
        }
    }

    /** Patch operations read from a spec op/path/value example array. */
    static List<PatchOperation> patchOps(String json) throws Exception {
        List<PatchOperation> ops = new ArrayList<>();
        for (JsonNode op : Constants.mapper().readTree(json)) {
            Object value = op.has("value") ? Constants.mapper().treeToValue(op.get("value"), Object.class) : null;
            ops.add(new PatchOperation(op.path("op").asText(), op.path("path").asText(), value));
        }
        return ops;
    }

    static void assertCovers(String expectedJson, Object actual, String label) throws Exception {
        assertCovers(Constants.mapper().readTree(expectedJson), actual, label);
    }

    /**
     * Asserts that the model, written back to JSON, still carries every non-null value of the example: the
     * model reads every property the example uses, with an equal value.
     */
    static void assertCovers(JsonNode expected, Object actual, String label) {
        JsonNode written = Constants.mapper().valueToTree(actual);
        List<String> problems = new ArrayList<>();
        compare(expected, written, "$", problems);
        if (!problems.isEmpty()) {
            fail(label + ": model lost or changed example values:\n  " + String.join("\n  ", problems));
        }
    }

    /** Asserts a page of models covers the example envelope's {@code data} array element by element. */
    static void assertPageCovers(String envelopeJson, List<?> items, String label) throws Exception {
        JsonNode data = Constants.mapper().readTree(envelopeJson).path("data");
        assertEquals(data.size(), items.size(), label + ": item count");
        for (int i = 0; i < items.size(); i++) {
            assertCovers(data.get(i), items.get(i), label + "[" + i + "]");
        }
    }

    private static void compare(JsonNode exp, JsonNode act, String at, List<String> problems) {
        if (exp == null || exp.isNull()) {
            return;
        }
        if (act == null || act.isMissingNode() || act.isNull()) {
            problems.add(at + " missing (expected " + exp + ")");
            return;
        }
        if (exp.isObject()) {
            if (!act.isObject()) {
                problems.add(at + " expected object, got " + act);
                return;
            }
            exp.fields().forEachRemaining(e -> compare(e.getValue(), act.get(e.getKey()), at + "." + e.getKey(), problems));
        } else if (exp.isArray()) {
            if (!act.isArray() || act.size() != exp.size()) {
                problems.add(at + " expected array of " + exp.size() + ", got " + act);
                return;
            }
            for (int i = 0; i < exp.size(); i++) {
                compare(exp.get(i), act.get(i), at + "[" + i + "]", problems);
            }
        } else if (exp.isNumber()) {
            if (!act.isNumber() || exp.decimalValue().compareTo(act.decimalValue()) != 0) {
                problems.add(at + " expected " + exp + ", got " + act);
            }
        } else if (!exp.equals(act)) {
            problems.add(at + " expected " + exp + ", got " + act);
        }
    }

}
