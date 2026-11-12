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
 * Launches the embedded Equinix Intelligence MCP Server over stdio - the entry point an MCP
 * host (Claude Desktop, Cursor, VS Code, ...) configures as a command. A community server, not
 * affiliated with Equinix.
 *
 * <p>Configuration, all via environment (or {@code .env.local} in the working directory as a
 * fallback for the credentials; see {@link #parseDotEnv(Path)} for the accepted syntax - a malformed
 * line exits with status 3 and a message naming the line and key, never the value):</p>
 * <ul>
 *   <li>{@code EQUINIX_ACCESS_KEY} / {@code EQUINIX_SECRET_KEY} - required client
 *       credentials; when absent from the environment, simple {@code KEY=VALUE} lines in
 *       {@code .env.local} are consulted. Missing credentials exit with status 2.</li>
 *   <li>{@code EQUINIX_MCP_TOOLSETS} or {@code --toolsets design,ibx} - optional toolset
 *       filter (ids: design, estate, fabric, ops, ne, ibx, mutate); default: every read-only
 *       toolset. The {@code mutate} toolset (the Safe Mutation Broker) is never on by
 *       default and must be named explicitly.</li>
 *   <li>{@code EQUINIX_SANDBOX=true} - optional; targets the sandbox environment.</li>
 *   <li>{@code EQUINIX_MCP_LOG_LEVEL} - optional slf4j-simple level (default {@code info}).</li>
 *   <li>{@code EQUINIX_PEERINGDB_KEY}, {@code GCP_BILLING_API_KEY} - optional tool enrichments.</li>
 *   <li>{@code EQUINIX_MCP_PRICING_TIMEOUT_MS} - optional; the live-pricing budget of one tool call
 *       in milliseconds, shared by all its lookups (default 12000, accepted 100..120000; other values
 *       are clipped or replaced by the default with a warning).</li>
 *   <li>{@code EQUINIX_MCP_ELICIT_TIMEOUT_MS} - optional; the hard timeout in milliseconds for one
 *       MCP elicitation round trip (default 300000). It bounds the account and service-profile
 *       prompts of {@code design_plan_deployment} and the confirmation prompt of
 *       {@code fabric_confirm_change}; an unanswered confirmation prompt executes nothing. Accepted
 *       range 100..3600000.</li>
 *   <li>{@code --env-file <path>} - optional; where to read the credentials fallback instead of
 *       {@code .env.local} in the working directory.</li>
 * </ul>
 *
 * <p><strong>stdout is sacred</strong>: it carries only MCP JSON-RPC. Every diagnostic - 
 * including these startup messages - goes to stderr, and slf4j-simple is pinned to stderr
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

    /** Exit status when {@code .env.local} has a malformed line. */
    static final int EXIT_BAD_ENV_FILE = 3;

    private EquinixMcpServerMain() {
    }

    /**
     * Starts the server. Returns once the transport is serving; the process stays alive on
     * the transport's reader thread and exits naturally when the host closes stdin.
     *
     * @param args optionally {@code --toolsets <csv>} and {@code --env-file <path>}
     */
    public static void main(String[] args) {
        configureLoggingToStderr(System.getenv());

        Map<String, String> env = System.getenv();
        Optional<BasicEquinixCredentials> credentials;
        try {
            credentials = resolveCredentialsFrom(env, resolveEnvFile(args,
                    Path.of(System.getProperty("user.dir", "."))));
        }
        catch (IllegalArgumentException e) {
            // The message names the file, line and key, never a value.
            System.err.println("equinix-sdk-java-intelligence: " + e.getMessage());
            System.exit(EXIT_BAD_ENV_FILE);
            return;
        }
        if (credentials.isEmpty()) {
            System.err.println("equinix-sdk-java-intelligence: no Equinix credentials found.");
            System.err.println("  Set " + ENV_ACCESS_KEY + " and " + ENV_SECRET_KEY + " in the environment,");
            System.err.println("  or put " + ENV_ACCESS_KEY + "=... and " + ENV_SECRET_KEY + "=... lines in "
                    + DOT_ENV_FILE + " in the working directory (or the file named by --env-file).");
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
     * directory ({@link #parseDotEnv(Path)}).
     *
     * @param env the environment view
     * @param workingDir the directory whose {@code .env.local} is the fallback
     * @return the credentials, or empty when neither source has both keys
     * @throws IllegalArgumentException when {@code .env.local} has to be read and is malformed
     */
    static Optional<BasicEquinixCredentials> resolveCredentials(Map<String, String> env, Path workingDir) {
        return resolveCredentialsFrom(env, workingDir.resolve(DOT_ENV_FILE));
    }

    /**
     * Resolves credentials: real environment first, then the given {@code .env}-style file.
     *
     * @param env the environment view
     * @param envFile the fallback file (need not exist)
     * @return the credentials, or empty when neither source has both keys
     * @throws IllegalArgumentException when {@code envFile} has to be read and is malformed
     */
    static Optional<BasicEquinixCredentials> resolveCredentialsFrom(Map<String, String> env, Path envFile) {
        String accessKey = trimmed(env.get(ENV_ACCESS_KEY));
        String secretKey = trimmed(env.get(ENV_SECRET_KEY));
        if (accessKey == null || secretKey == null) {
            Map<String, String> dotEnv = parseDotEnv(envFile);
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
     * Parses a {@code .env.local}-style file into key/value pairs.
     *
     * <p>Accepted per line (blank lines and lines starting with {@code #} are skipped; a UTF-8 byte
     * order mark is ignored):</p>
     * <ul>
     *   <li>{@code KEY=value}, optionally prefixed with {@code export } (POSIX shell style); the key
     *       is a shell identifier ({@code [A-Za-z_][A-Za-z0-9_]*}) and whitespace around it and the
     *       {@code =} is ignored;</li>
     *   <li>an unquoted value ends at the first {@code #} that follows whitespace (an inline
     *       comment) and is trimmed; a {@code #} inside a value ({@code abc#def}) is kept;</li>
     *   <li>a value in single quotes is taken literally; a value in double quotes may use the
     *       escapes {@code \"}, {@code \\}, {@code \n}, {@code \r} and {@code \t}. After the closing
     *       quote only whitespace or a {@code #} comment may follow.</li>
     * </ul>
     * <p>Any other line is malformed and fails the whole file with an
     * {@link IllegalArgumentException} naming the line number and, where there is one, the key.
     * The message never contains a value, so it is safe to print. Keys with empty values are
     * omitted. A missing file yields an empty map.</p>
     *
     * @param envFile the file to parse
     * @return the parsed key/value pairs
     * @throws IllegalArgumentException when a line is malformed
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
            System.err.println("equinix-sdk-java-intelligence: could not read " + envFile + ": "
                    + e.getClass().getSimpleName());
            return values;
        }
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String where = envFile.getFileName() + " line " + (index + 1);
            if (trimmed.startsWith("export") && trimmed.length() > 6 && Character.isWhitespace(trimmed.charAt(6))) {
                trimmed = trimmed.substring(6).trim();
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException(where + " is not a KEY=VALUE line.");
            }
            String key = trimmed.substring(0, eq).trim();
            if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException(where + ": the key before '=' is not a valid variable name"
                        + " (letters, digits and underscores, not starting with a digit).");
            }
            String value = parseDotEnvValue(trimmed.substring(eq + 1).trim(), where, key);
            if (!value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    /** Parses the text after {@code =}; messages name the key, never the value. */
    private static String parseDotEnvValue(String raw, String where, String key) {
        if (raw.isEmpty()) {
            return raw;
        }
        char first = raw.charAt(0);
        if (first != '"' && first != '\'') {
            int comment = -1;
            for (int i = 1; i < raw.length(); i++) {
                if (raw.charAt(i) == '#' && Character.isWhitespace(raw.charAt(i - 1))) {
                    comment = i;
                    break;
                }
            }
            return (comment < 0 ? raw : raw.substring(0, comment)).trim();
        }
        StringBuilder out = new StringBuilder();
        int i = 1;
        boolean closed = false;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == first) {
                closed = true;
                i++;
                break;
            }
            if (first == '"' && c == '\\') {
                if (i + 1 >= raw.length()) {
                    break;
                }
                char next = raw.charAt(i + 1);
                switch (next) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    default -> throw new IllegalArgumentException(where + ": the value of " + key
                            + " uses an unsupported escape sequence inside double quotes.");
                }
                i += 2;
                continue;
            }
            out.append(c);
            i++;
        }
        if (!closed) {
            throw new IllegalArgumentException(where + ": the value of " + key + " has an unterminated "
                    + (first == '"' ? "double" : "single") + " quote.");
        }
        String rest = raw.substring(i).trim();
        if (!rest.isEmpty() && !rest.startsWith("#")) {
            throw new IllegalArgumentException(where + ": the value of " + key + " has text after its "
                    + "closing quote; only a # comment may follow.");
        }
        return out.toString();
    }

    /**
     * The credentials fallback file: {@code --env-file <path>} (or {@code --env-file=<path>}) when
     * given, else {@code .env.local} in {@code workingDir}. The connect scripts pass it so the server
     * finds the repository's {@code .env.local} even when the MCP host ignores the entry's
     * {@code cwd}.
     *
     * @param args the launch arguments
     * @param workingDir the process working directory
     * @return the file to read credentials from when the environment lacks them
     */
    static Path resolveEnvFile(String[] args, Path workingDir) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--env-file") && i + 1 < args.length) {
                    return Path.of(args[i + 1]);
                }
                if (args[i].startsWith("--env-file=")) {
                    return Path.of(args[i].substring("--env-file=".length()));
                }
            }
        }
        return workingDir.resolve(DOT_ENV_FILE);
    }

    /**
     * Resolves the toolset filter: a {@code --toolsets <csv>} (or {@code --toolsets=<csv>})
     * argument wins over {@code EQUINIX_MCP_TOOLSETS}; blank means every read-only toolset
     * ({@code mutate} is never selected by default).
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
