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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Launches the embedded Equinix Intelligence MCP Server over stdio — the entry point an MCP
 * host (Claude Desktop, Cursor, VS Code, …) configures as a command. A community server, not
 * affiliated with Equinix.
 *
 * <p>Configuration, all via environment (or {@code .env.local} in the working directory as a
 * fallback for the credentials):</p>
 * <ul>
 *   <li>{@code EQUINIX_ACCESS_KEY} / {@code EQUINIX_SECRET_KEY} — required client
 *       credentials; when absent from the environment, simple {@code KEY=VALUE} lines in
 *       {@code .env.local} are consulted. Missing credentials exit with status 2.</li>
 *   <li>{@code EQUINIX_MCP_TOOLSETS} or {@code --toolsets design,ibx} — optional toolset
 *       filter (ids: design, fabric, portal, ne, ibx, mutate); default: every read-only
 *       toolset. The {@code mutate} toolset (the Safe Mutation Broker) is never on by
 *       default and must be named explicitly.</li>
 *   <li>{@code EQUINIX_SANDBOX=true} — optional; targets the sandbox environment.</li>
 *   <li>{@code EQUINIX_MCP_LOG_LEVEL} — optional slf4j-simple level (default {@code info}).</li>
 *   <li>{@code EQUINIX_PEERINGDB_KEY}, {@code GCP_BILLING_API_KEY},
 *       {@code EQUINIX_MCP_PRICING_TIMEOUT_MS} — optional tool enrichments.</li>
 * </ul>
 *
 * <p><strong>stdout is sacred</strong>: it carries only MCP JSON-RPC. Every diagnostic —
 * including these startup messages — goes to stderr, and slf4j-simple is pinned to stderr
 * before the first logger initializes.</p>
 */
public final class EquinixMcpServerMain {

    static final String ENV_ACCESS_KEY = "EQUINIX_ACCESS_KEY";
    static final String ENV_SECRET_KEY = "EQUINIX_SECRET_KEY";
    static final String ENV_TOOLSETS = "EQUINIX_MCP_TOOLSETS";
    static final String ENV_SANDBOX = "EQUINIX_SANDBOX";
    static final String ENV_LOG_LEVEL = "EQUINIX_MCP_LOG_LEVEL";
    static final String DOT_ENV_FILE = ".env.local";

    /** Exit status when credentials cannot be resolved. */
    static final int EXIT_NO_CREDENTIALS = 2;

    private EquinixMcpServerMain() {
    }

    /**
     * Starts the server. Returns once the transport is serving; the process stays alive on
     * the transport's reader thread and exits naturally when the host closes stdin.
     *
     * @param args optionally {@code --toolsets <csv>}
     */
    public static void main(String[] args) {
        configureLoggingToStderr(System.getenv());

        Map<String, String> env = System.getenv();
        Optional<BasicEquinixCredentials> credentials =
                resolveCredentials(env, Path.of(System.getProperty("user.dir", ".")));
        if (credentials.isEmpty()) {
            System.err.println("equinix-sdk-java-intelligence: no Equinix credentials found.");
            System.err.println("  Set " + ENV_ACCESS_KEY + " and " + ENV_SECRET_KEY + " in the environment,");
            System.err.println("  or put " + ENV_ACCESS_KEY + "=... and " + ENV_SECRET_KEY + "=... lines in "
                    + DOT_ENV_FILE + " in the working directory.");
            System.err.println("  (Client credentials from the Equinix developer portal; no browser login is involved.)");
            System.exit(EXIT_NO_CREDENTIALS);
            return;
        }

        String toolsets = resolveToolsets(args, env);
        boolean sandbox = "true".equalsIgnoreCase(env.get(ENV_SANDBOX));

        EquinixMcpServer server = EquinixMcpServer.builder()
                .credentials(credentials.get())
                .sandbox(sandbox)
                .toolsets(toolsets)
                .build();

        System.err.println("equinix-sdk-java-intelligence v" + EquinixMcpServer.serverVersion()
                + " ready on stdio with " + server.getRegistrations().size() + " tools"
                + (sandbox ? " (sandbox)" : "") + ".");
        // The stdio transport's non-daemon reader thread keeps the JVM alive; when the MCP
        // host closes stdin the transport winds down and the process exits on its own.
    }

    /**
     * Pins slf4j-simple to stderr (stdout is the MCP wire) and applies the configured level.
     * Must run before the first {@code LoggerFactory} call; harmless if another binding is
     * in use.
     */
    static void configureLoggingToStderr(Map<String, String> env) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
        String level = env.getOrDefault(ENV_LOG_LEVEL, "info");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
    }

    /**
     * Resolves credentials: real environment first, then {@code .env.local} in the working
     * directory (simple {@code KEY=VALUE} lines; {@code #} comments and blanks ignored).
     *
     * @param env the environment view
     * @param workingDir the directory whose {@code .env.local} is the fallback
     * @return the credentials, or empty when neither source has both keys
     */
    static Optional<BasicEquinixCredentials> resolveCredentials(Map<String, String> env, Path workingDir) {
        String accessKey = trimmed(env.get(ENV_ACCESS_KEY));
        String secretKey = trimmed(env.get(ENV_SECRET_KEY));
        if (accessKey == null || secretKey == null) {
            Map<String, String> dotEnv = parseDotEnv(workingDir.resolve(DOT_ENV_FILE));
            if (accessKey == null) {
                accessKey = trimmed(dotEnv.get(ENV_ACCESS_KEY));
            }
            if (secretKey == null) {
                secretKey = trimmed(dotEnv.get(ENV_SECRET_KEY));
            }
        }
        if (accessKey == null || secretKey == null) {
            return Optional.empty();
        }
        return Optional.of(new BasicEquinixCredentials(accessKey, secretKey));
    }

    /**
     * Parses a {@code .env.local}-style file: one {@code KEY=VALUE} per line, first {@code =}
     * splits, whitespace trimmed, {@code #} comment lines and blank lines ignored, missing
     * file yields an empty map. (House pattern shared with the retired MCP-client login.)
     *
     * @param envFile the file to parse
     * @return the parsed key/value pairs
     */
    static Map<String, String> parseDotEnv(Path envFile) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(envFile)) {
            return values;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            System.err.println("equinix-sdk-java-intelligence: could not read " + envFile + ": " + e);
            return values;
        }
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    /**
     * Resolves the toolset filter: a {@code --toolsets <csv>} (or {@code --toolsets=<csv>})
     * argument wins over {@code EQUINIX_MCP_TOOLSETS}; blank means every toolset.
     *
     * @param args the launch arguments
     * @param env the environment view
     * @return the csv to hand to {@code Toolset.parse} (possibly empty)
     */
    static String resolveToolsets(String[] args, Map<String, String> env) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--toolsets") && i + 1 < args.length) {
                    return args[i + 1];
                }
                if (arg.startsWith("--toolsets=")) {
                    return arg.substring("--toolsets=".length());
                }
            }
        }
        return env.getOrDefault(ENV_TOOLSETS, "");
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
