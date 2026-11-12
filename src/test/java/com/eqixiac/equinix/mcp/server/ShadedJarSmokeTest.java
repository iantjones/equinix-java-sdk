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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end smoke test of the SHADED server jar as a subprocess over stdio: {@code initialize},
 * {@code tools/list} and one {@code tools/call} that needs no network
 * ({@code design_list_multicloud_environments} reads the bundled catalog), asserting that stdout
 * carries nothing but JSON-RPC 2.0 messages and that the process exits when stdin closes.
 *
 * <p>The jar is built by {@code package}, after {@code test}, so this test skips (JUnit assumption)
 * when no {@code target/equinix-sdk-java-*-mcp-server.jar} exists. Point it at a specific jar with
 * {@code -Dmcp.server.jar=<path>}. CI runs it after packaging:</p>
 * <pre>
 * mvn -B package -DskipTests
 * mvn -B test -Dtest=ShadedJarSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dmcp.server.jar.required=true
 * </pre>
 * <p>With {@code -Dmcp.server.jar.required=true} a missing jar fails instead of skipping.</p>
 */
@DisplayName("Shaded MCP server jar - subprocess stdio smoke test")
class ShadedJarSmokeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DUMMY_SECRET = "smoke-test-secret-not-real";

    @TempDir
    Path workingDir;

    private static Optional<Path> serverJar() throws IOException {
        String explicit = System.getProperty("mcp.server.jar");
        if (explicit != null && !explicit.isBlank()) {
            return Optional.of(Path.of(explicit)).filter(Files::isRegularFile);
        }
        Path target = Path.of("target");
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(target)) {
            return files.filter(p -> p.getFileName().toString().matches("equinix-sdk-java-.*-mcp-server\\.jar"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
        }
    }

    @Test
    @Timeout(120)
    @DisplayName("initialize, tools/list and a no-network tools/call; stdout is JSON-RPC only")
    void shadedJarSpeaksOnlyJsonRpcOnStdout() throws Exception {
        Optional<Path> jar = serverJar();
        if (Boolean.getBoolean("mcp.server.jar.required")) {
            assertTrue(jar.isPresent(), "mcp.server.jar.required is set but no shaded server jar was found");
        }
        assumeTrue(jar.isPresent(), "no shaded server jar in target/ (run 'mvn package' first); skipping");

        String java = ProcessHandle.current().info().command().orElse("java");
        ProcessBuilder builder = new ProcessBuilder(java, "-jar", jar.get().toAbsolutePath().toString(),
                "--toolsets", "design")
                .directory(workingDir.toFile());
        Map<String, String> env = builder.environment();
        env.keySet().removeIf(key -> key.startsWith("EQUINIX_") || key.equals("GCP_BILLING_API_KEY"));
        // Dummy credentials: the server needs some to start, and this test makes no Equinix call.
        env.put("EQUINIX_ACCESS_KEY", "smoke-test-access");
        env.put("EQUINIX_SECRET_KEY", DUMMY_SECRET);
        env.put("EQUINIX_MCP_LOG_LEVEL", "debug");
        Process process = builder.start();

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        Thread stderrPump = new Thread(() -> {
            try {
                process.getErrorStream().transferTo(stderr);
            }
            catch (IOException ignored) {
                // The process ended.
            }
        });
        stderrPump.start();
        BlockingQueue<String> stdoutLines = new LinkedBlockingQueue<>();
        List<String> allStdout = new ArrayList<>();
        Thread stdoutPump = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (allStdout) {
                        allStdout.add(line);
                    }
                    stdoutLines.add(line);
                }
            }
            catch (IOException ignored) {
                // The process ended.
            }
        });
        stdoutPump.start();

        try (OutputStream stdin = process.getOutputStream()) {
            send(stdin, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":"
                    + "\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"smoke\",\"version\":\"1\"}}}");
            JsonNode init = next(stdoutLines, stderr);
            assertEquals(1, init.get("id").asInt());
            assertEquals(EquinixMcpServer.SERVER_NAME, init.get("result").get("serverInfo").get("name").asText());
            assertEquals("2025-11-25", init.get("result").get("protocolVersion").asText());

            send(stdin, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            send(stdin, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
            JsonNode list = next(stdoutLines, stderr);
            assertEquals(2, list.get("id").asInt());
            assertEquals(16, list.get("result").get("tools").size(), "the design toolset: " + list);

            send(stdin, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":"
                    + "\"design_list_multicloud_environments\",\"arguments\":{}}}");
            JsonNode call = next(stdoutLines, stderr);
            assertEquals(3, call.get("id").asInt());
            assertFalse(call.get("result").path("isError").asBoolean(false), call.toString());
            assertNotNull(call.get("result").get("structuredContent"), call.toString());
        }

        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the server exits when stdin closes");
        stdoutPump.join(5_000);
        stderrPump.join(5_000);
        synchronized (allStdout) {
            assertEquals(3, allStdout.size(), "stdout carries exactly the three responses: " + allStdout);
            for (String line : allStdout) {
                JsonNode message = MAPPER.readTree(line);
                assertEquals("2.0", message.path("jsonrpc").asText(), "not JSON-RPC on stdout: " + line);
            }
        }
        String errors = stderr.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains("ready on stdio"), "diagnostics go to stderr: " + errors);
        assertFalse(errors.contains(DUMMY_SECRET), "the secret never reaches the logs, even at debug");
    }

    private static void send(OutputStream stdin, String json) throws IOException {
        stdin.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    private static JsonNode next(BlockingQueue<String> lines, ByteArrayOutputStream stderr) throws Exception {
        String line = lines.poll(60, TimeUnit.SECONDS);
        assertNotNull(line, "no response on stdout; stderr: " + stderr.toString(StandardCharsets.UTF_8));
        return MAPPER.readTree(line);
    }
}
