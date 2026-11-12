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

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An in-process stdio smoke test: the real {@link StdioServerTransportProvider} over piped
 * streams, driven with raw newline-delimited JSON-RPC - initialize, initialized, tools/list,
 * and a tools/call - while {@code System.out} is captured to prove the process's stdout
 * carries nothing (the wire is the injected pipe; stdout stays sacred).
 */
@DisplayName("stdio round trip - raw JSON-RPC over piped streams + stdout purity")
class StdioRoundTripTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @Timeout(30)
    @DisplayName("initialize -> tools/list (22 tools) -> tools/call, all valid JSON-RPC, stdout untouched")
    void roundTrip() throws Exception {
        // Metro fixture for design_estimate_latency.
        Metro dc = metro("DC", "Ashburn", 39.0438, -77.4874);
        Metro sv = metro("SV", "Silicon Valley", 37.3382, -121.8863);
        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, sv), null, null, null, null));

        ServerContext context = ServerContext.builder()
                .metroRegistry(MetroRegistry.load(metros))
                .environment(Map.of())
                .build();

        PipedOutputStream clientToServer = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientToServer, 1 << 16);
        PipedOutputStream serverOutPipe = new PipedOutputStream();
        PipedInputStream clientReads = new PipedInputStream(serverOutPipe, 1 << 20);

        PrintStream realOut = System.out;
        ByteArrayOutputStream stdoutCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdoutCapture, true, StandardCharsets.UTF_8));

        EquinixMcpServer server = null;
        try {
            server = EquinixMcpServer.builder()
                    .context(context)
                    .stdio(serverIn, serverOutPipe)
                    .build();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientReads, StandardCharsets.UTF_8));
            List<String> wireLines = new ArrayList<>();

            // initialize
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",\
                    "capabilities":{},"clientInfo":{"name":"round-trip-test","version":"1.0"}}}""");
            JsonNode initResponse = readMessage(reader, wireLines);
            assertEquals(1, initResponse.get("id").asInt());
            assertEquals("equinix-sdk-java-intelligence",
                    initResponse.get("result").get("serverInfo").get("name").asText());
            assertNotNull(initResponse.get("result").get("capabilities").get("tools"),
                    "tools capability is advertised");
            assertTrue(initResponse.get("result").get("instructions").asText().contains("not affiliated"),
                    "the unofficial positioning ships in the instructions");
            assertEquals("2025-06-18", initResponse.get("result").get("protocolVersion").asText(),
                    "a supported requested revision is echoed");

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

            // tools/list
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
            JsonNode listResponse = readMessage(reader, wireLines);
            assertEquals(2, listResponse.get("id").asInt());
            JsonNode tools = listResponse.get("result").get("tools");
            assertEquals(22, tools.size(), "the full launch catalog is served: " + tools);
            Set<String> names = new HashSet<>();
            tools.forEach(t -> names.add(t.get("name").asText()));
            assertTrue(names.contains("design_optimize"), names.toString());
            assertTrue(names.contains("design_analyze_estate"), names.toString());
            assertTrue(names.contains("ibx_list_power_events"), names.toString());
            for (JsonNode tool : tools) {
                assertNotNull(tool.get("inputSchema"), tool.get("name") + " ships an input schema");
                assertEquals(true, tool.get("annotations").get("readOnlyHint").asBoolean(),
                        tool.get("name") + " is advertised read-only");
            }

            // tools/call
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"design_estimate_latency",\
                    "arguments":{"from":"DC","to":"SV"}}}""");
            JsonNode callResponse = readMessage(reader, wireLines);
            assertEquals(3, callResponse.get("id").asInt());
            JsonNode result = callResponse.get("result");
            assertFalse(result.path("isError").asBoolean(false), "the call succeeds: " + result);
            JsonNode structured = result.get("structuredContent");
            assertNotNull(structured, "structured content is returned");
            assertTrue(structured.get("distance_km").asDouble() > 3000, structured.toString());
            assertTrue(structured.get("estimated_round_trip_ms").asDouble() > 0, structured.toString());
            assertEquals("estimated", structured.get("basis").asText(), "the fixture has no measured pair");

            // design_validate_spec over the wire (structured issues)
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"design_validate_spec",                    "arguments":{"spec":"name: x\\nworkloads:\\n  - label: w\\n    bandwidthMbps: 100\\n"}}}""");
            JsonNode validate = readMessage(reader, wireLines).get("result");
            assertFalse(validate.path("isError").asBoolean(false), validate.toString());
            assertTrue(validate.get("structuredContent").get("valid").asBoolean(), validate.toString());
            assertEquals(64, validate.get("structuredContent").get("spec_fingerprint").asText().length());

            // resources and prompts
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"resources/list\",\"params\":{}}");
            JsonNode resources = readMessage(reader, wireLines).get("result").get("resources");
            Set<String> uris = new HashSet<>();
            resources.forEach(r -> uris.add(r.get("uri").asText()));
            assertEquals(Set.of(DesignResources.SPEC_SCHEMA_URI, DesignResources.TEMPLATE_URI,
                    DesignResources.PLANS_URI), uris);
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"resources/read\",\"params\":{\"uri\":\""
                    + DesignResources.SPEC_SCHEMA_URI + "\"}}");
            JsonNode schema = MAPPER.readTree(readMessage(reader, wireLines).get("result").get("contents").get(0)
                    .get("text").asText());
            assertEquals("https://json-schema.org/draft/2020-12/schema", schema.get("$schema").asText());
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"resources/templates/list\",\"params\":{}}");
            assertEquals(DesignResources.PLAN_TEMPLATE, readMessage(reader, wireLines).get("result")
                    .get("resourceTemplates").get(0).get("uriTemplate").asText());
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"prompts/list\",\"params\":{}}");
            Set<String> prompts = new HashSet<>();
            readMessage(reader, wireLines).get("result").get("prompts").forEach(p -> prompts.add(p.get("name").asText()));
            assertEquals(Set.of("design_from_requirements", "review_existing_estate", "what_breaks_if"), prompts);
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":9,"method":"prompts/get","params":{"name":"what_breaks_if",                    "arguments":{"failure":"LD"}}}""");
            String promptText = readMessage(reader, wireLines).get("result").get("messages").get(0).get("content")
                    .get("text").asText();
            assertTrue(promptText.contains("design_analyze_resilience") && promptText.contains("LD"), promptText);

            // wire and stdout purity
            for (String line : wireLines) {
                JsonNode parsed = MAPPER.readTree(line);
                assertEquals("2.0", parsed.get("jsonrpc").asText(),
                        "every wire line is a JSON-RPC message: " + line);
            }
            assertEquals(0, stdoutCapture.size(),
                    "System.out must stay untouched; got: " + stdoutCapture.toString(StandardCharsets.UTF_8));
        }
        finally {
            System.setOut(realOut);
            clientToServer.close();
            if (server != null) {
                server.close();
            }
        }
    }

    private static void send(OutputStream out, String json) throws Exception {
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** Reads the next non-blank wire line, records it, and parses it. */
    private static JsonNode readMessage(BufferedReader reader, List<String> wireLines) throws Exception {
        String line = reader.readLine();
        assertNotNull(line, "the server closed the stream unexpectedly");
        wireLines.add(line);
        return MAPPER.readTree(line);
    }

    private static Metro metro(String code, String name, double lat, double lon) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(Region.AMER);
        when(m.geoCoordinates()).thenReturn(MAPPER.readValue(
                "{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class));
        when(m.getConnectedMetros()).thenReturn(List.of());
        return m;
    }

    @Test
    @Timeout(60)
    @DisplayName("design tools chain over the wire: optimize -> plan -> the plan resource; estate refuses no scope")
    void designLoopOverStdio() throws Exception {
        ServerContext context = DesignFixtures.context();
        PipedOutputStream clientToServer = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientToServer, 1 << 16);
        PipedOutputStream serverOutPipe = new PipedOutputStream();
        PipedInputStream clientReads = new PipedInputStream(serverOutPipe, 1 << 22);
        EquinixMcpServer server = EquinixMcpServer.builder()
                .context(context)
                .toolsets("design,estate")
                .stdio(serverIn, serverOutPipe)
                .build();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientReads, StandardCharsets.UTF_8));
            List<String> wire = new ArrayList<>();
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25",\
                    "capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""");
            readMessage(reader, wire);
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

            ObjectNode optimizeArgs = (ObjectNode) MAPPER.readTree(DesignFixtures.withSpec(DesignFixtures.SPEC_DC_AWS,
                    null));
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":"
                    + "\"design_optimize\",\"arguments\":" + optimizeArgs + "}}");
            JsonNode optimized = readMessage(reader, wire).get("result");
            assertFalse(optimized.path("isError").asBoolean(false), optimized.toString());
            String fingerprint = optimized.get("structuredContent").get("spec_fingerprint").asText();

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":"
                    + "\"design_plan_deployment\",\"arguments\":{\"spec_fingerprint\":\"" + fingerprint
                    + "\",\"deployment\":{\"notifications\":[\"noc@example.com\"]}}}}");
            JsonNode planned = readMessage(reader, wire).get("result");
            assertFalse(planned.path("isError").asBoolean(false), planned.toString());
            String planId = planned.get("structuredContent").get("plan_id").asText();

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"resources/read\",\"params\":{\"uri\":\""
                    + DesignResources.planUri(planId) + "\"}}");
            JsonNode doc = MAPPER.readTree(readMessage(reader, wire).get("result").get("contents").get(0)
                    .get("text").asText());
            assertEquals(planId, doc.get("planId").asText());
            assertEquals(fingerprint, doc.get("specFingerprint").asText());

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":"
                    + "\"design_analyze_estate\",\"arguments\":{}}}");
            JsonNode refused = readMessage(reader, wire).get("result");
            assertTrue(refused.get("isError").asBoolean(), refused.toString());
            assertTrue(refused.get("content").get(0).get("text").asText().contains("project_ids"), refused.toString());
        }
        finally {
            clientToServer.close();
            server.close();
        }
    }

    @Test
    @Timeout(30)
    @DisplayName("SDK 2.1.0 negotiates up to 2025-11-25; notifications/cancelled stops an in-flight tools/call")
    void negotiationAndCancellation() throws Exception {
        java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        ToolRegistration slow = ToolRegistration.builder()
                .name("design_test_wait")
                .title("Waits until cancelled")
                .description("Test tool.")
                .inputSchema(Schemas.object(Schemas.props()))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) (args, ctx, cancel) -> {
                    started.countDown();
                    long deadline = System.currentTimeMillis() + 20_000;
                    while (!cancel.isCancelled() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(10);
                    }
                    cancel.throwIfCancelled("while waiting");
                    return ctx.objectMapper().createObjectNode().put("finished", true);
                })
                .build();
        ServerContext context = ServerContext.builder().environment(Map.of()).build();

        PipedOutputStream clientToServer = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientToServer, 1 << 16);
        PipedOutputStream serverOutPipe = new PipedOutputStream();
        PipedInputStream clientReads = new PipedInputStream(serverOutPipe, 1 << 20);
        EquinixMcpServer server = EquinixMcpServer.builder()
                .context(context)
                .toolsets(Toolset.IBX)
                .additionalTools(slow)
                .stdio(serverIn, serverOutPipe)
                .build();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientReads, StandardCharsets.UTF_8));
            List<String> wire = new ArrayList<>();
            send(clientToServer, """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2026-07-28",\
                    "capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""");
            assertEquals("2025-11-25", readMessage(reader, wire).get("result").get("protocolVersion").asText(),
                    "an unsupported newer revision is answered with the latest the SDK supports");
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":\"call-1\",\"method\":\"tools/call\","
                    + "\"params\":{\"name\":\"design_test_wait\",\"arguments\":{}}}");
            assertTrue(started.await(10, java.util.concurrent.TimeUnit.SECONDS), "the tool started");
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\","
                    + "\"params\":{\"requestId\":\"call-1\",\"reason\":\"user stop\"}}");

            JsonNode response = readMessage(reader, wire);
            assertEquals("call-1", response.get("id").asText());
            assertTrue(response.get("result").get("isError").asBoolean(), response.toString());
            assertTrue(response.get("result").get("content").get(0).get("text").asText()
                    .contains("cancelled this tool call"), response.toString());
        }
        finally {
            clientToServer.close();
            server.close();
        }
    }
}
