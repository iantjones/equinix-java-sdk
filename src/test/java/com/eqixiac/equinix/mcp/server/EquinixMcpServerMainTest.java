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

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EquinixMcpServerMain - credential and toolset resolution")
class EquinixMcpServerMainTest {

    @TempDir
    Path workingDir;

    // credentials

    @Test
    @DisplayName("environment variables win outright when both keys are present")
    void environmentWins() throws Exception {
        Files.writeString(workingDir.resolve(".env.local"),
                "EQUINIX_ACCESS_KEY=file-access\nEQUINIX_SECRET_KEY=file-secret\n");
        Optional<BasicEquinixCredentials> credentials = EquinixMcpServerMain.resolveCredentials(
                Map.of("EQUINIX_ACCESS_KEY", "env-access", "EQUINIX_SECRET_KEY", "env-secret"),
                workingDir);
        assertTrue(credentials.isPresent());
        assertEquals("env-access", credentials.get().getAccessKey());
        assertEquals("env-secret", credentials.get().getSecretKey());
    }

    @Test
    @DisplayName(".env.local supplies whichever keys the environment lacks")
    void dotEnvFallback() throws Exception {
        Files.writeString(workingDir.resolve(".env.local"),
                "# comment line\n"
                        + "\n"
                        + "EQUINIX_ACCESS_KEY = file-access\n"
                        + "export EQUINIX_SECRET_KEY=\"file-secret\" # quoted, exported\n");
        Optional<BasicEquinixCredentials> credentials =
                EquinixMcpServerMain.resolveCredentials(Map.of(), workingDir);
        assertTrue(credentials.isPresent(), ".env.local alone should resolve credentials");
        assertEquals("file-access", credentials.get().getAccessKey());
        assertEquals("file-secret", credentials.get().getSecretKey());
    }

    @Test
    @DisplayName("mixed resolution: env access key + .env.local secret key")
    void mixedResolution() throws Exception {
        Files.writeString(workingDir.resolve(".env.local"), "EQUINIX_SECRET_KEY=file-secret\n");
        Optional<BasicEquinixCredentials> credentials = EquinixMcpServerMain.resolveCredentials(
                Map.of("EQUINIX_ACCESS_KEY", "env-access"), workingDir);
        assertTrue(credentials.isPresent());
        assertEquals("env-access", credentials.get().getAccessKey());
        assertEquals("file-secret", credentials.get().getSecretKey());
    }

    @Test
    @DisplayName("no environment keys and no .env.local resolves to empty (Main exits 2)")
    void absentEverywhere() {
        assertTrue(EquinixMcpServerMain.resolveCredentials(Map.of(), workingDir).isEmpty());
    }

    @Test
    @DisplayName("blank values are treated as absent")
    void blankValuesAreAbsent() throws Exception {
        Files.writeString(workingDir.resolve(".env.local"), "EQUINIX_ACCESS_KEY=\nEQUINIX_SECRET_KEY=  \n");
        assertTrue(EquinixMcpServerMain.resolveCredentials(
                Map.of("EQUINIX_ACCESS_KEY", "   "), workingDir).isEmpty());
    }

    @Test
    @DisplayName("parseDotEnv ignores comments and blanks; only the first '=' splits")
    void parseDotEnvShape() throws Exception {
        Path envFile = workingDir.resolve(".env.local");
        Files.writeString(envFile, "# header\nA=1\n  B = two \n\nE=x=y\nEMPTY=\n");
        Map<String, String> parsed = EquinixMcpServerMain.parseDotEnv(envFile);
        assertEquals("1", parsed.get("A"));
        assertEquals("two", parsed.get("B"));
        assertEquals("x=y", parsed.get("E"), "only the first '=' splits");
        assertEquals(3, parsed.size(), "empty values are omitted: " + parsed);
    }

    @Test
    @DisplayName("parseDotEnv accepts export prefixes, quotes, inline comments and a BOM")
    void parseDotEnvShellSyntax() throws Exception {
        Path envFile = workingDir.resolve(".env.local");
        Files.writeString(envFile, "\uFEFFexport EQUINIX_ACCESS_KEY=\"abc def\"   # quoted\n"
                + "export\tEQUINIX_SECRET_KEY='s3cr#t\\n'\n"
                + "PLAIN=value # trailing comment\n"
                + "HASH=abc#def\n"
                + "ESCAPED=\"a\\\"b\\\\c\"\n"
                + "exported=1\n");
        Map<String, String> parsed = EquinixMcpServerMain.parseDotEnv(envFile);
        assertEquals("abc def", parsed.get("EQUINIX_ACCESS_KEY"));
        assertEquals("s3cr#t\\n", parsed.get("EQUINIX_SECRET_KEY"), "single quotes are literal");
        assertEquals("value", parsed.get("PLAIN"));
        assertEquals("abc#def", parsed.get("HASH"), "a # not preceded by whitespace is part of the value");
        assertEquals("a\"b\\c", parsed.get("ESCAPED"));
        assertEquals("1", parsed.get("exported"), "a key that merely starts with 'export' is a key");
    }

    @Test
    @DisplayName("a malformed value fails with the line and key, never the value")
    void parseDotEnvMalformed() throws Exception {
        Path envFile = workingDir.resolve(".env.local");
        for (String[] bad : new String[][] {
                {"EQUINIX_SECRET_KEY=\"topsecret\n", "line 1: the value of EQUINIX_SECRET_KEY has an unterminated double quote"},
                {"A=1\nEQUINIX_SECRET_KEY='topsecret\n", "line 2: the value of EQUINIX_SECRET_KEY has an unterminated single quote"},
                {"EQUINIX_SECRET_KEY=\"topsecret\" extra\n", "EQUINIX_SECRET_KEY has text after its closing quote"},
                {"topsecret\n", "line 1 is not a KEY=VALUE line"},
                {"1BAD=topsecret\n", "not a valid variable name"}}) {
            Files.writeString(envFile, bad[0]);
            IllegalArgumentException e = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> EquinixMcpServerMain.parseDotEnv(envFile));
            assertTrue(e.getMessage().contains(bad[1]), e.getMessage());
            assertTrue(!e.getMessage().contains("topsecret"), "values never appear in messages: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("parseDotEnv of a missing file is an empty map")
    void parseDotEnvMissing() {
        assertTrue(EquinixMcpServerMain.parseDotEnv(workingDir.resolve("nope.env")).isEmpty());
    }

    // toolsets

    @Test
    @DisplayName("--toolsets argument wins over EQUINIX_MCP_TOOLSETS")
    void toolsetsArgWins() {
        assertEquals("design,ibx", EquinixMcpServerMain.resolveToolsets(
                new String[] {"--toolsets", "design,ibx"}, Map.of("EQUINIX_MCP_TOOLSETS", "ops")));
        assertEquals("ne", EquinixMcpServerMain.resolveToolsets(
                new String[] {"--toolsets=ne"}, Map.of("EQUINIX_MCP_TOOLSETS", "ops")));
    }

    @Test
    @DisplayName("EQUINIX_MCP_TOOLSETS applies when no argument is given; default is blank")
    void toolsetsEnvAndDefault() {
        assertEquals("ops", EquinixMcpServerMain.resolveToolsets(
                new String[0], Map.of("EQUINIX_MCP_TOOLSETS", "ops")));
        assertEquals("", EquinixMcpServerMain.resolveToolsets(new String[0], Map.of()));
    }

    @Test
    @DisplayName("Toolset.parse: blank selects the read-only defaults, ids select subsets, unknown ids fail loudly")
    void toolsetParsing() {
        assertEquals(6, Toolset.parse("").size());
        assertEquals(6, Toolset.parse(null).size());
        assertEquals(java.util.EnumSet.of(Toolset.DESIGN, Toolset.IBX), Toolset.parse(" design , ibx "));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Toolset.parse("design,warp"));
    }

    // logging

    @Test
    @DisplayName("configureLoggingToStderr pins slf4j-simple to stderr - stdout stays protocol-only")
    void loggingPinnedToStderr() {
        String previousLogFile = System.getProperty("org.slf4j.simpleLogger.logFile");
        String previousLevel = System.getProperty("org.slf4j.simpleLogger.defaultLogLevel");
        try {
            EquinixMcpServerMain.configureLoggingToStderr(Map.of("EQUINIX_MCP_LOG_LEVEL", "warn"));
            assertEquals("System.err", System.getProperty("org.slf4j.simpleLogger.logFile"));
            assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        }
        finally {
            restore("org.slf4j.simpleLogger.logFile", previousLogFile);
            restore("org.slf4j.simpleLogger.defaultLogLevel", previousLevel);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        }
        else {
            System.setProperty(key, value);
        }
    }
}
