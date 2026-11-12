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

import com.eqixiac.equinix.design.estate.EstateAnalyzer;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.mcp.server.DesignFixtures.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code design_analyze_estate}: scope enforcement and the payload over an offline inventory snapshot. */
@DisplayName("design_analyze_estate (scoped, read-only)")
class EstateToolsTest {

    private final List<EstateScope> scopes = new ArrayList<>();

    private ServerContext snapshotContext() {
        return ServerContext.builder()
                .environment(Map.of())
                .estateAnalyzerFactory(scope -> {
                    scopes.add(scope);
                    return EstateAnalyzer.fromSnapshot(snapshot());
                })
                .build();
    }

    private static EstateInventory snapshot() {
        try (InputStream in = EstateToolsTest.class.getResourceAsStream("/json/design/estate/inventory_snapshot.json")) {
            return EstateInventory.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ObjectNode call(ServerContext ctx, String args) throws Exception {
        return DesignFixtures.call("design_analyze_estate", ctx, args);
    }

    @Test
    @DisplayName("an unscoped call is refused before anything is read, and says why")
    void refusesUnscoped() {
        ServerContext ctx = snapshotContext();
        for (String args : List.of("{}", "{\"project_ids\": []}", "{\"project_ids\": [\"   \"]}")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> call(ctx, args));
            assertTrue(e.getMessage().contains("project_ids"), e.getMessage());
            assertTrue(e.getMessage().contains("never scans everything"), e.getMessage());
        }
        assertTrue(scopes.isEmpty(), "no analysis was started");
    }

    @Test
    @DisplayName("malformed or too many project ids are refused")
    void refusesBadScopes() {
        ServerContext ctx = snapshotContext();
        assertThrows(IllegalArgumentException.class, () -> call(ctx, "{\"project_ids\": [\"*\"]}"));
        StringBuilder many = new StringBuilder("{\"project_ids\": [");
        for (int i = 0; i < 11; i++) {
            many.append(i == 0 ? "" : ",").append("\"11111111-1111-1111-1111-0000000000").append(10 + i).append('"');
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(ctx, many.append("]}").toString()));
        assertTrue(e.getMessage().contains("at most 10"), e.getMessage());
        assertTrue(scopes.isEmpty());
    }

    @Test
    @DisplayName("the server context itself refuses an unscoped or whole-account analysis")
    void contextRefusesUnscoped() {
        ServerContext ctx = snapshotContext();
        assertThrows(IllegalArgumentException.class,
                () -> ctx.estateAnalyzer(EstateScope.entireAccount(100)));
        assertThrows(IllegalArgumentException.class, () -> ctx.estateAnalyzer(null));
    }

    @Test
    @DisplayName("the schema requires project_ids (1 to 10) and the description says why")
    void schema() {
        ToolRegistration tool = DesignFixtures.tool("design_analyze_estate");
        assertEquals(List.of("project_ids"), tool.getInputSchema().get("required"));
        assertTrue(tool.getDescription().contains("SCOPE IS REQUIRED"), tool.getDescription());
        assertTrue(tool.getDescription().contains("other owners"), tool.getDescription());
        assertEquals(Toolset.ESTATE, tool.getToolset());
        assertTrue(tool.isReadOnly());
    }

    @Test
    @DisplayName("a scoped run returns ranked actions, findings, savings per currency and provenance")
    void scopedRun() throws Exception {
        ServerContext ctx = snapshotContext();
        ObjectNode payload = call(ctx, "{\"project_ids\": [\"11111111-1111-1111-1111-111111111111\"], "
                + "\"max_resources_per_type\": 100}");
        EstateScope scope = scopes.get(0);
        assertEquals(List.of("11111111-1111-1111-1111-111111111111"), List.copyOf(scope.projectIds()));
        assertFalse(scope.entireAccount());
        assertEquals(100, scope.maxResourcesPerType());
        assertTrue(payload.get("findings").size() > 0, payload.toPrettyString());
        assertEquals(1, payload.get("actions").get(0).get("rank").asInt());
        assertTrue(payload.has("addressable_monthly_saving"));
        assertTrue(payload.get("not_analysed").size() > 0);
        assertEquals("live", payload.get("provenance").get("kind").asText());
        JsonNode first = payload.get("findings").get(0);
        assertFalse(first.has("evidence"), "summary leaves evidence for detail=standard or finding_ids");

        String id = first.get("id").asText();
        ObjectNode detail = call(ctx, "{\"project_ids\": [\"11111111-1111-1111-1111-111111111111\"], "
                + "\"finding_ids\": [" + MAPPER.writeValueAsString(id) + "]}");
        assertTrue(detail.get("finding_details").get(0).has("evidence"), detail.toPrettyString());
    }
}
