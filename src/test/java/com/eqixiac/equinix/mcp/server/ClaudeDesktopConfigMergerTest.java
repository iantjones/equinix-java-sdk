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
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ClaudeDesktopConfigMerger - merges the equinix entry, backs up, writes no secret by default")
class ClaudeDesktopConfigMergerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path dir;

    private Path repo() throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Files.writeString(repo.resolve(".env.local"),
                "export EQUINIX_ACCESS_KEY=\"ak-value\"\nEQUINIX_SECRET_KEY='sk-value' # secret\n");
        return repo;
    }

    private static final String EXISTING = """
            {"globalShortcut": "Ctrl+Space",
             "mcpServers": {
               "other": {"command": "node", "args": ["x.js"]},
               "equinix": {"command": "/opt/jdk/bin/java",
                           "args": ["-Xmx512m", "-jar", "/old/server.jar", "--toolsets", "design,mutate",
                                    "--env-file", "/old/.env.local"],
                           "env": {"EQUINIX_SANDBOX": "true", "EQUINIX_MCP_TOOLSETS": "design",
                                   "EQUINIX_ACCESS_KEY": "old-ak", "EQUINIX_SECRET_KEY": "old-sk"},
                           "custom": 1}}}""";

    @Test
    @DisplayName("an existing entry is merged: env, custom args, command and other servers survive; secrets are removed")
    void mergesWithoutSecrets() throws Exception {
        Path repo = repo();
        Path config = dir.resolve("claude_desktop_config.json");
        Files.writeString(config, EXISTING);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Path backup = ClaudeDesktopConfigMerger.run(config, Path.of("/new/server.jar"), repo, false,
                new PrintStream(out, true, StandardCharsets.UTF_8));

        assertNotNull(backup);
        assertEquals(EXISTING, Files.readString(backup), "the original is backed up verbatim");
        String written = Files.readString(config);
        assertFalse(written.startsWith("﻿"), "no byte-order mark");
        JsonNode root = MAPPER.readTree(written);
        assertEquals("Ctrl+Space", root.get("globalShortcut").asText());
        assertEquals("node", root.get("mcpServers").get("other").get("command").asText());
        JsonNode entry = root.get("mcpServers").get("equinix");
        assertEquals("/opt/jdk/bin/java", entry.get("command").asText(), "a custom java path is kept");
        assertEquals(List.of("-Xmx512m", "-jar", "/new/server.jar", "--toolsets", "design,mutate",
                        "--env-file", repo.resolve(".env.local").toString()),
                MAPPER.convertValue(entry.get("args"), List.class));
        assertEquals(repo.toString(), entry.get("cwd").asText());
        assertEquals("true", entry.get("env").get("EQUINIX_SANDBOX").asText(), "sandbox users stay on sandbox");
        assertEquals("design", entry.get("env").get("EQUINIX_MCP_TOOLSETS").asText());
        assertFalse(entry.get("env").has("EQUINIX_ACCESS_KEY"));
        assertFalse(entry.get("env").has("EQUINIX_SECRET_KEY"));
        assertEquals(1, entry.get("custom").asInt());
        assertFalse(written.contains("sk-value") || written.contains("ak-value"), "no secret written");
        String log = out.toString(StandardCharsets.UTF_8);
        assertFalse(log.contains("sk-value") || log.contains("old-sk"), "no secret printed: " + log);
        assertTrue(log.contains("Kept env: EQUINIX_SANDBOX, EQUINIX_MCP_TOOLSETS"), log);
    }

    @Test
    @DisplayName("--embed-credentials writes the keys into env and drops --env-file")
    void embedsOnRequest() throws Exception {
        Path repo = repo();
        Path config = dir.resolve("claude_desktop_config.json");
        Files.writeString(config, EXISTING);
        ClaudeDesktopConfigMerger.run(config, Path.of("/new/server.jar"), repo, true,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        JsonNode entry = MAPPER.readTree(Files.readString(config)).get("mcpServers").get("equinix");
        assertEquals("ak-value", entry.get("env").get("EQUINIX_ACCESS_KEY").asText());
        assertEquals("sk-value", entry.get("env").get("EQUINIX_SECRET_KEY").asText());
        assertEquals("true", entry.get("env").get("EQUINIX_SANDBOX").asText());
        assertFalse(entry.get("args").toString().contains("--env-file"));
    }

    @Test
    @DisplayName("a missing config is created with a fresh entry; no backup is made")
    void createsConfig() throws Exception {
        Path repo = repo();
        Path config = dir.resolve("Claude/claude_desktop_config.json");
        Path backup = ClaudeDesktopConfigMerger.run(config, Path.of("/new/server.jar"), repo, false,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(null, backup);
        JsonNode entry = MAPPER.readTree(Files.readString(config)).get("mcpServers").get("equinix");
        assertEquals("java", entry.get("command").asText());
        assertEquals("-jar", entry.get("args").get(0).asText());
        assertFalse(entry.has("env"));
    }

    @Test
    @DisplayName("an invalid config or a malformed .env.local leaves everything untouched")
    void refusesBadInput() throws Exception {
        Path repo = repo();
        Path config = dir.resolve("claude_desktop_config.json");
        Files.writeString(config, "{not json");
        assertThrows(IllegalArgumentException.class, () -> ClaudeDesktopConfigMerger.run(config,
                Path.of("/j.jar"), repo, false, new PrintStream(new ByteArrayOutputStream())));
        assertEquals("{not json", Files.readString(config));

        Files.writeString(config, EXISTING);
        Files.writeString(repo.resolve(".env.local"), "EQUINIX_ACCESS_KEY=\"unterminated\nEQUINIX_SECRET_KEY=x\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ClaudeDesktopConfigMerger
                .run(config, Path.of("/j.jar"), repo, false, new PrintStream(new ByteArrayOutputStream())));
        assertTrue(e.getMessage().contains("EQUINIX_ACCESS_KEY"), e.getMessage());
        assertEquals(EXISTING, Files.readString(config));
        try (var files = Files.list(dir)) {
            assertEquals(2, files.count(), "no backup was written for a refused run (repo + config only)");
        }
    }
}
