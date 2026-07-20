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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
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
 * streams, driven with raw newline-delimited JSON-RPC — initialize, initialized, tools/list,
 * and a tools/call — while {@code System.out} is captured to prove the process's stdout
 * carries nothing (the wire is the injected pipe; stdout stays sacred).
 */
@DisplayName("stdio round trip — raw JSON-RPC over piped streams + stdout purity")
class StdioRoundTripTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @Timeout(30)
    @DisplayName("initialize → tools/list (12 tools) → tools/call, all valid JSON-RPC, stdout untouched")
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
                    .transportProvider(new StdioServerTransportProvider(
                            new JacksonMcpJsonMapper(context.objectMapper()), serverIn, serverOutPipe))
                    .build();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientReads, StandardCharsets.UTF_8));
            List<String> wireLines = new ArrayList<>();

            // ── initialize ──────────────────────────────────────────────────
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

            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");

            // ── tools/list ──────────────────────────────────────────────────
            send(clientToServer, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
            JsonNode listResponse = readMessage(reader, wireLines);
            assertEquals(2, listResponse.get("id").asInt());
            JsonNode tools = listResponse.get("result").get("tools");
            assertEquals(12, tools.size(), "the full launch catalog is served: " + tools);
            Set<String> names = new HashSet<>();
            tools.forEach(t -> names.add(t.get("name").asText()));
            assertTrue(names.contains("design_optimize_placement"), names.toString());
            assertTrue(names.contains("ibx_list_power_events"), names.toString());
            for (JsonNode tool : tools) {
                assertNotNull(tool.get("inputSchema"), tool.get("name") + " ships an input schema");
                assertEquals(true, tool.get("annotations").get("readOnlyHint").asBoolean(),
                        tool.get("name") + " is advertised read-only");
            }

            // ── tools/call ──────────────────────────────────────────────────
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
            assertTrue(structured.get("estimated_latency_ms").asDouble() > 0, structured.toString());

            // ── wire and stdout purity ──────────────────────────────────────
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
}
