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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers this server in a Claude Desktop {@code claude_desktop_config.json} by <em>merging</em>
 * into the {@code mcpServers.equinix} entry. {@code connect-claude-desktop.ps1} (Windows) and
 * {@code connect-claude-desktop.sh} (macOS, Linux) both run it from the built server jar, so the
 * merge behaves the same on every platform:
 *
 * <pre>
 * java -cp equinix-sdk-java-&lt;v&gt;-mcp-server.jar com.eqixiac.equinix.mcp.server.ClaudeDesktopConfigMerger \
 *      --config &lt;claude_desktop_config.json&gt; --jar &lt;server jar&gt; --repo &lt;repository dir&gt; [--embed-credentials]
 * </pre>
 *
 * <p>Merge rules for {@code mcpServers.equinix}:</p>
 * <ul>
 *   <li>every other server, top-level setting and unknown field is kept;</li>
 *   <li>{@code command} is kept when set (for example an absolute {@code java} path), else {@code java};</li>
 *   <li>{@code args}: the value after {@code -jar} is replaced with the new jar (or {@code -jar <jar>}
 *       is prepended); other arguments ({@code --toolsets}, JVM options) are kept; a previous
 *       {@code --env-file} pair is replaced;</li>
 *   <li>{@code env}: every variable is kept ({@code EQUINIX_SANDBOX}, {@code EQUINIX_MCP_TOOLSETS},
 *       ...) except the two credential keys, which are removed by default;</li>
 *   <li>by default no secret is written: {@code cwd} is set to the repository and
 *       {@code --env-file <repo>/.env.local} is added, so the server reads the credentials from
 *       {@code .env.local} (the flag also covers hosts that ignore {@code cwd}). With
 *       {@code --embed-credentials} the two keys are copied into {@code env} instead, in plain text.</li>
 * </ul>
 * <p>Before writing, the existing file is copied to {@code <config>.bak-<timestamp>}. A file that is
 * not a JSON object is left untouched and the run fails. No credential value is ever printed.</p>
 */
public final class ClaudeDesktopConfigMerger {

    /** The server entry name under {@code mcpServers}. */
    public static final String ENTRY = "equinix";

    private static final String ACCESS = EquinixMcpServerMain.ENV_ACCESS_KEY;
    private static final String SECRET = EquinixMcpServerMain.ENV_SECRET_KEY;

    private ClaudeDesktopConfigMerger() {
    }

    /**
     * Runs the merge; exits non-zero with a message on stderr on failure.
     *
     * @param args {@code --config <file> --jar <file> --repo <dir> [--embed-credentials]}
     */
    public static void main(String[] args) {
        Path config = null;
        Path jar = null;
        Path repo = null;
        boolean embed = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config" -> config = Path.of(args[++i]);
                case "--jar" -> jar = Path.of(args[++i]);
                case "--repo" -> repo = Path.of(args[++i]);
                case "--embed-credentials" -> embed = true;
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(64);
                }
            }
        }
        if (config == null || jar == null || repo == null) {
            System.err.println("Usage: --config <claude_desktop_config.json> --jar <server jar> --repo <dir> "
                    + "[--embed-credentials]");
            System.exit(64);
        }
        try {
            run(config, jar.toAbsolutePath(), repo.toAbsolutePath(), embed, System.out);
        }
        catch (IllegalArgumentException | IOException e) {
            System.err.println("Not changed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Validates {@code <repo>/.env.local}, backs the config up, merges and writes it.
     *
     * @param config the Claude Desktop config file (created when missing)
     * @param jar the server jar
     * @param repo the repository directory holding {@code .env.local}
     * @param embedCredentials copy the credentials into the entry's {@code env}
     * @param out where the summary goes (never any credential value)
     * @return the backup file, or {@code null} when there was no existing config
     * @throws IOException on a read or write failure
     * @throws IllegalArgumentException when {@code .env.local} or the config is unusable
     */
    static Path run(Path config, Path jar, Path repo, boolean embedCredentials, PrintStream out) throws IOException {
        Path envFile = repo.resolve(EquinixMcpServerMain.DOT_ENV_FILE);
        if (!Files.isRegularFile(envFile)) {
            throw new IllegalArgumentException(envFile + " is missing: create it with " + ACCESS + "=... and " + SECRET
                    + "=... lines.");
        }
        Map<String, String> credentials = EquinixMcpServerMain.parseDotEnv(envFile);
        for (String key : List.of(ACCESS, SECRET)) {
            if (credentials.get(key) == null || credentials.get(key).isBlank()) {
                throw new IllegalArgumentException(key + " is empty in " + envFile + ".");
            }
        }

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root;
        if (Files.isRegularFile(config) && Files.size(config) > 0) {
            JsonNode parsed;
            try {
                parsed = mapper.readTree(stripBom(Files.readString(config, StandardCharsets.UTF_8)));
            }
            catch (IOException e) {
                throw new IllegalArgumentException(config + " is not valid JSON; fix or remove it first.");
            }
            if (parsed == null || !parsed.isObject()) {
                throw new IllegalArgumentException(config + " is not a JSON object; fix or remove it first.");
            }
            root = (ObjectNode) parsed;
        }
        else {
            root = mapper.createObjectNode();
        }

        List<String> notes = merge(root, jar, repo, embedCredentials ? credentials : null);

        Path backup = null;
        if (Files.isRegularFile(config)) {
            backup = config.resolveSibling(config.getFileName() + ".bak-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            Files.copy(config, backup, StandardCopyOption.REPLACE_EXISTING);
            out.println("Backed up the existing config to " + backup);
        }
        else if (config.getParent() != null) {
            Files.createDirectories(config.getParent());
        }
        // UTF-8 without a byte-order mark: Claude Desktop's JSON parser rejects a BOM.
        Files.writeString(config, mapper.writeValueAsString(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        out.println("Registered mcpServers." + ENTRY + " in " + config);
        out.println("Jar: " + jar);
        notes.forEach(note -> out.println(note));
        return backup;
    }

    /**
     * Merges the server entry into {@code root} in place.
     *
     * @param root the parsed config object
     * @param jar the server jar
     * @param repo the repository directory
     * @param credentials the credentials to embed, or {@code null} to read them from {@code .env.local}
     * @return human-readable notes about what changed (no values)
     */
    static List<String> merge(ObjectNode root, Path jar, Path repo, Map<String, String> credentials) {
        List<String> notes = new ArrayList<>();
        JsonNode servers = root.get("mcpServers");
        ObjectNode mcpServers = servers != null && servers.isObject() ? (ObjectNode) servers : root.putObject("mcpServers");
        JsonNode existing = mcpServers.get(ENTRY);
        ObjectNode entry = existing != null && existing.isObject() ? (ObjectNode) existing : mcpServers.putObject(ENTRY);
        notes.add(existing != null && existing.isObject()
                ? "Merged into the existing entry; its other settings were kept."
                : "Created a new entry.");

        if (!entry.path("command").isTextual() || entry.path("command").asText().isBlank()) {
            entry.put("command", "java");
        }

        List<String> args = new ArrayList<>();
        if (entry.path("args").isArray()) {
            entry.get("args").forEach(arg -> args.add(arg.asText()));
        }
        int envFileAt = args.indexOf("--env-file");
        if (envFileAt >= 0) {
            args.remove(envFileAt);
            if (envFileAt < args.size()) {
                args.remove(envFileAt);
            }
        }
        args.removeIf(arg -> arg.startsWith("--env-file="));
        int jarAt = args.indexOf("-jar");
        if (jarAt < 0) {
            args.add(0, "-jar");
            args.add(1, jar.toString());
        }
        else if (jarAt + 1 < args.size()) {
            args.set(jarAt + 1, jar.toString());
        }
        else {
            args.add(jar.toString());
        }
        if (credentials == null) {
            args.add("--env-file");
            args.add(repo.resolve(EquinixMcpServerMain.DOT_ENV_FILE).toString());
        }
        ArrayNode argsNode = entry.putArray("args");
        args.forEach(argsNode::add);

        entry.put("cwd", repo.toString());

        JsonNode envNode = entry.get("env");
        ObjectNode env = envNode != null && envNode.isObject() ? (ObjectNode) envNode : null;
        if (credentials != null) {
            if (env == null) {
                env = entry.putObject("env");
            }
            env.put(ACCESS, credentials.get(ACCESS));
            env.put(SECRET, credentials.get(SECRET));
            notes.add("Embedded " + ACCESS + " and " + SECRET + " in the config in PLAIN TEXT "
                    + "(--embed-credentials).");
        }
        else {
            boolean removedAccess = env != null && env.remove(ACCESS) != null;
            boolean removedSecret = env != null && env.remove(SECRET) != null;
            if (removedAccess || removedSecret) {
                notes.add("Removed credentials previously embedded in the config; the server now reads them "
                        + "from " + repo.resolve(EquinixMcpServerMain.DOT_ENV_FILE) + ".");
            }
            else {
                notes.add("No secret was written: the server reads the credentials from "
                        + repo.resolve(EquinixMcpServerMain.DOT_ENV_FILE) + ".");
            }
            if (env != null && env.isEmpty()) {
                entry.remove("env");
            }
        }
        if (env != null && !env.isEmpty()) {
            List<String> kept = new ArrayList<>();
            env.fieldNames().forEachRemaining(name -> {
                if (!name.equals(ACCESS) && !name.equals(SECRET)) {
                    kept.add(name);
                }
            });
            if (!kept.isEmpty()) {
                notes.add("Kept env: " + String.join(", ", kept));
            }
        }
        return notes;
    }

    private static String stripBom(String text) {
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
