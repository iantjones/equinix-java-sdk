package api.equinix.javasdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Live probe of the Equinix Fabric MCP Server ({@code https://mcp.equinix.com/fabric}).
 *
 * <p><b>Read-only by construction:</b> the probe performs OAuth token exchange, the MCP
 * {@code initialize} handshake, and {@code tools/list}. It never issues {@code tools/call},
 * so no Fabric resource can be touched no matter what credentials it is given.</p>
 *
 * <p>What it verifies (and produces):
 * <ul>
 *     <li>The refresh-token grant works against {@code https://as.equinix.com/oauth2/token}
 *         as a public client (no client secret - the AS advertises auth method "none").
 *         Note this authorization server does NOT support {@code client_credentials}: the
 *         SDK's api.equinix.com access keys can never authenticate here. Only the
 *         device-flow refresh token from {@code McpLogin} works.</li>
 *     <li>The Streamable HTTP MCP endpoint accepts the bearer, completes {@code initialize},
 *         and serves {@code tools/list} (JSON or SSE-framed responses both handled,
 *         pagination followed via {@code nextCursor}).</li>
 *     <li>The LIVE tool catalog is dumped to {@code target/live-mcp-catalog.json} for
 *         inspection, then diffed against the pinned manifest
 *         ({@code src/main/resources/mcp-catalog.json}). Any drift - documented tools the
 *         server no longer offers, or live tools the pin does not know - fails with a
 *         readable missing/extra/possibly-renamed report.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * # one-time: device-code login writes EQUINIX_MCP_CLIENT_ID / EQUINIX_MCP_REFRESH_TOKEN
 * # into .env.local (see .env.local.example)
 * java -cp target/classes api.equinix.javasdk.mcp.auth.McpLogin
 *
 * mvn test -Plive-mcp
 * </pre>
 *
 * <p>Credentials are resolved from environment variables first, then JVM system
 * properties, then {@code .env.local} in the project basedir. When absent the test is
 * skipped via {@link Assumptions}, not failed.</p>
 */
@Tag("live-mcp")
class McpLiveProbeTest {

    private static final String TOKEN_ENDPOINT = "https://as.equinix.com/oauth2/token";
    private static final String MCP_ENDPOINT = "https://mcp.equinix.com/fabric";
    private static final String PROTOCOL_VERSION = "2025-06-18";
    private static final String PINNED_MANIFEST_RESOURCE = "/mcp-catalog.json";
    private static final Path LIVE_DUMP_PATH = Path.of("target", "live-mcp-catalog.json");

    private static final String ENV_CLIENT_ID = "EQUINIX_MCP_CLIENT_ID";
    private static final String ENV_REFRESH_TOKEN = "EQUINIX_MCP_REFRESH_TOKEN";

    private static final String SKIP_MESSAGE =
            ENV_CLIENT_ID + " / " + ENV_REFRESH_TOKEN + " not set - skipping live MCP probe. "
                    + "Populate .env.local (copy .env.local.example) via the one-time device-code login: "
                    + "java -cp target/classes api.equinix.javasdk.mcp.auth.McpLogin, "
                    + "then run: mvn test -Plive-mcp";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static CloseableHttpClient httpClient;

    /** Session id issued by the Streamable HTTP server on initialize (echoed on later calls). */
    private static String mcpSessionId;

    @BeforeAll
    static void createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setSocketTimeout(60_000)
                .build();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @AfterAll
    static void closeHttpClient() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    @DisplayName("Live tools/list matches the pinned mcp-catalog.json manifest")
    void liveCatalogMatchesPinnedManifest() throws IOException {
        String clientId = resolveCredential(ENV_CLIENT_ID);
        String refreshToken = resolveCredential(ENV_REFRESH_TOKEN);
        Assumptions.assumeTrue(clientId != null && refreshToken != null, SKIP_MESSAGE);

        // 1. Refresh-token exchange (public client: no secret, form-encoded).
        String accessToken = exchangeRefreshToken(clientId, refreshToken);

        // 2. MCP initialize handshake (Streamable HTTP).
        initialize(accessToken);

        // 3. tools/list (follows nextCursor pagination).
        List<JsonNode> liveTools = listAllTools(accessToken);
        assertTrue(!liveTools.isEmpty(),
                "Live tools/list returned zero tools - the server answered but exposed nothing."
                        + " Check the account's Private Beta entitlement before trusting any diff.");

        // 4. Dump the live catalog for inspection.
        dumpLiveCatalog(liveTools);

        // 5. Diff live names vs the pinned manifest.
        Set<String> pinnedNames = loadPinnedToolNames();
        Set<String> liveNames = new TreeSet<>();
        for (JsonNode tool : liveTools) {
            JsonNode name = tool.get("name");
            if (name != null && !name.asText().isBlank()) {
                liveNames.add(name.asText());
            }
        }

        String report = buildDriftReport(pinnedNames, liveNames);
        if (report == null) {
            System.out.println("[live-mcp] Live catalog matches pinned manifest: "
                    + liveNames.size() + " tools. Dump written to " + LIVE_DUMP_PATH + ".");
        } else {
            System.out.println(report);
            fail(report);
        }
    }

    // ── OAuth ────────────────────────────────────────────────────────────────

    private static String exchangeRefreshToken(String clientId, String refreshToken) throws IOException {
        HttpPost post = new HttpPost(TOKEN_ENDPOINT);
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("grant_type", "refresh_token"));
        form.add(new BasicNameValuePair("refresh_token", refreshToken));
        form.add(new BasicNameValuePair("client_id", clientId));
        post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status != 200) {
                fail("Refresh-token exchange at " + TOKEN_ENDPOINT + " failed with HTTP " + status
                        + ": " + body + "\nThe stored refresh token may be expired or revoked -"
                        + " re-run the device-code login (java -cp target/classes"
                        + " api.equinix.javasdk.mcp.auth.McpLogin) to mint a fresh one in .env.local.");
            }
            JsonNode json = MAPPER.readTree(body);
            JsonNode accessToken = json.get("access_token");
            assertNotNull(accessToken, "Token endpoint returned 200 but no access_token: " + body);
            return accessToken.asText();
        }
    }

    // ── MCP protocol (Streamable HTTP) ───────────────────────────────────────

    private static void initialize(String accessToken) throws IOException {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.set("capabilities", MAPPER.createObjectNode());
        ObjectNode clientInfo = MAPPER.createObjectNode();
        clientInfo.put("name", "equinix-java-sdk-live-probe");
        clientInfo.put("version", "2.0.1");
        params.set("clientInfo", clientInfo);

        JsonNode result = executeRpc("initialize", params, 1, accessToken);
        assertNotNull(result, "MCP initialize returned no result.");

        // Best-effort initialized notification; some servers require it, none should
        // break the probe if they reject it.
        ObjectNode notification = MAPPER.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "notifications/initialized");
        try {
            postRpc(notification, accessToken);
        } catch (IOException e) {
            System.out.println("[live-mcp] notifications/initialized not accepted ("
                    + e.getMessage() + ") - continuing.");
        }
    }

    private static List<JsonNode> listAllTools(String accessToken) throws IOException {
        List<JsonNode> tools = new ArrayList<>();
        String cursor = null;
        int requestId = 2;
        do {
            ObjectNode params = MAPPER.createObjectNode();
            if (cursor != null) {
                params.put("cursor", cursor);
            }
            JsonNode result = executeRpc("tools/list", params, requestId++, accessToken);
            assertNotNull(result, "MCP tools/list returned no result.");

            JsonNode toolsNode = result.get("tools");
            assertNotNull(toolsNode, "tools/list result carries no 'tools' array: " + result);
            toolsNode.forEach(tools::add);

            JsonNode next = result.get("nextCursor");
            cursor = (next != null && !next.isNull() && !next.asText().isBlank()) ? next.asText() : null;
        } while (cursor != null);
        return tools;
    }

    /** Sends a JSON-RPC request and returns the {@code result} node (fails loud on errors). */
    private static JsonNode executeRpc(String method, ObjectNode params, int id, String accessToken)
            throws IOException {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);

        JsonNode response = postRpc(request, accessToken);
        assertNotNull(response, "No JSON-RPC message could be parsed from the '" + method + "' response.");
        JsonNode error = response.get("error");
        if (error != null && !error.isNull()) {
            fail("MCP '" + method + "' returned a JSON-RPC error: " + error);
        }
        return response.get("result");
    }

    /**
     * POSTs one JSON-RPC message to the MCP endpoint and parses the response, handling
     * both plain JSON and SSE ({@code text/event-stream}) framing. Returns {@code null}
     * for empty-bodied (notification-ack) responses.
     */
    private static JsonNode postRpc(ObjectNode message, String accessToken) throws IOException {
        HttpPost post = new HttpPost(MCP_ENDPOINT);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Accept", "application/json, text/event-stream");
        post.setHeader("MCP-Protocol-Version", PROTOCOL_VERSION);
        if (mcpSessionId != null) {
            post.setHeader("Mcp-Session-Id", mcpSessionId);
        }
        post.setEntity(new StringEntity(MAPPER.writeValueAsString(message),
                ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                    : "";

            Header sessionHeader = response.getFirstHeader("Mcp-Session-Id");
            if (sessionHeader != null && sessionHeader.getValue() != null) {
                mcpSessionId = sessionHeader.getValue();
            }

            if (status == 401 || status == 403) {
                fail("MCP endpoint " + MCP_ENDPOINT + " rejected the bearer with HTTP " + status
                        + ": " + body + "\nThe token was accepted by as.equinix.com but refused by"
                        + " the resource - check the Private Beta entitlement and that the token"
                        + " carries the 'api:temporary-full' scope.");
            }
            if (status == 202 || status == 204 || body.isBlank()) {
                return null; // notification acknowledged, nothing to parse
            }
            if (status != 200) {
                throw new IOException("MCP request '" + message.path("method").asText()
                        + "' failed with HTTP " + status + ": " + truncate(body, 2000));
            }

            Header contentType = response.getFirstHeader("Content-Type");
            boolean sse = contentType != null && contentType.getValue() != null
                    && contentType.getValue().toLowerCase(Locale.ROOT).contains("text/event-stream");
            return sse ? parseSseResponse(body) : MAPPER.readTree(body);
        }
    }

    /**
     * Extracts the first JSON-RPC response message (a JSON object with a "result" or
     * "error" member) from an SSE-framed body.
     */
    private static JsonNode parseSseResponse(String body) throws IOException {
        StringBuilder data = new StringBuilder();
        List<String> events = new ArrayList<>();
        for (String line : body.split("\r?\n", -1)) {
            if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append('\n');
                }
                data.append(line.substring(5).trim());
            } else if (line.isEmpty() && data.length() > 0) {
                events.add(data.toString());
                data.setLength(0);
            }
        }
        if (data.length() > 0) {
            events.add(data.toString());
        }
        for (String event : events) {
            try {
                JsonNode node = MAPPER.readTree(event);
                if (node.isObject() && (node.has("result") || node.has("error"))) {
                    return node;
                }
            } catch (IOException ignored) {
                // non-JSON SSE event (comment/keep-alive) - skip
            }
        }
        throw new IOException("SSE response contained no JSON-RPC result/error message: "
                + truncate(body, 2000));
    }

    // ── Pinned manifest + drift report ───────────────────────────────────────

    private static Set<String> loadPinnedToolNames() throws IOException {
        try (var in = McpLiveProbeTest.class.getResourceAsStream(PINNED_MANIFEST_RESOURCE)) {
            assertNotNull(in, "Pinned manifest " + PINNED_MANIFEST_RESOURCE + " not on the classpath"
                    + " (expected at src/main/resources/mcp-catalog.json).");
            JsonNode manifest = MAPPER.readTree(in);
            Set<String> names = new TreeSet<>();
            JsonNode families = manifest.get("families");
            assertNotNull(families, "Pinned manifest has no 'families' array.");
            for (JsonNode family : families) {
                JsonNode tools = family.get("tools");
                if (tools != null) {
                    for (JsonNode tool : tools) {
                        names.add(tool.get("name").asText());
                    }
                }
            }
            return names;
        }
    }

    private static void dumpLiveCatalog(List<JsonNode> liveTools) throws IOException {
        ObjectNode dump = MAPPER.createObjectNode();
        dump.put("probedAt", Instant.now().toString());
        dump.put("endpoint", MCP_ENDPOINT);
        dump.put("toolCount", liveTools.size());
        ArrayNode toolsArray = dump.putArray("tools");
        liveTools.forEach(toolsArray::add);

        Files.createDirectories(LIVE_DUMP_PATH.getParent());
        Files.writeString(LIVE_DUMP_PATH,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dump),
                StandardCharsets.UTF_8);
        System.out.println("[live-mcp] Dumped " + liveTools.size() + " live tools to " + LIVE_DUMP_PATH);
    }

    /**
     * Builds a human-readable drift report, or returns {@code null} when live and
     * pinned catalogs agree exactly. Missing/extra pairs within a small edit distance
     * are additionally surfaced as possible renames.
     */
    private static String buildDriftReport(Set<String> pinnedNames, Set<String> liveNames) {
        Set<String> missing = new TreeSet<>(pinnedNames);
        missing.removeAll(liveNames);
        Set<String> extra = new TreeSet<>(liveNames);
        extra.removeAll(pinnedNames);

        if (missing.isEmpty() && extra.isEmpty()) {
            return null;
        }

        Map<String, String> possibleRenames = new LinkedHashMap<>();
        for (String missingName : new LinkedHashSet<>(missing)) {
            String bestMatch = null;
            int bestDistance = Integer.MAX_VALUE;
            for (String extraName : extra) {
                int distance = levenshtein(missingName, extraName);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = extraName;
                }
            }
            if (bestMatch != null && bestDistance <= 3) {
                possibleRenames.put(missingName, bestMatch);
            }
        }
        possibleRenames.forEach((from, to) -> {
            missing.remove(from);
            extra.remove(to);
        });

        StringBuilder report = new StringBuilder();
        report.append("LIVE MCP CATALOG DRIFT against pinned mcp-catalog.json (")
                .append(pinnedNames.size()).append(" pinned vs ").append(liveNames.size())
                .append(" live). Full live dump: ").append(LIVE_DUMP_PATH).append('\n');
        if (!possibleRenames.isEmpty()) {
            report.append("  POSSIBLY RENAMED (").append(possibleRenames.size()).append("):\n");
            possibleRenames.forEach((from, to) ->
                    report.append("    pinned '").append(from).append("' -> live '").append(to).append("'\n"));
        }
        if (!missing.isEmpty()) {
            report.append("  MISSING from live server (pinned but not served, ")
                    .append(missing.size()).append("):\n");
            missing.forEach(name -> report.append("    ").append(name).append('\n'));
        }
        if (!extra.isEmpty()) {
            report.append("  EXTRA on live server (served but not pinned, ")
                    .append(extra.size()).append("):\n");
            extra.forEach(name -> report.append("    ").append(name).append('\n'));
        }
        report.append("If the docs changed, re-pin src/main/resources/mcp-catalog.json (and the"
                + " EXPECTED_TOOL_COUNT in McpCatalogTest) from the docs page; if they did not,"
                + " the Private Beta server has drifted from its documentation.");
        return report.toString();
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    // ── Credential resolution ────────────────────────────────────────────────

    private static final Pattern ENV_LINE = Pattern.compile("^\\s*([A-Z_]+)\\s*=\\s*(.+?)\\s*$");

    /**
     * Resolves a credential from (in order): environment variable, JVM system property,
     * {@code .env.local} in the project basedir. Returns {@code null} when absent or blank.
     */
    private static String resolveCredential(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        if ((value == null || value.isBlank())) {
            value = readFromEnvLocal(key);
        }
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String readFromEnvLocal(String key) {
        Path envLocal = Path.of(".env.local");
        if (!Files.isRegularFile(envLocal)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(envLocal, StandardCharsets.UTF_8)) {
                Matcher matcher = ENV_LINE.matcher(line);
                if (matcher.matches() && matcher.group(1).equals(key)) {
                    return matcher.group(2);
                }
            }
        } catch (IOException e) {
            System.out.println("[live-mcp] Could not read .env.local: " + e.getMessage());
        }
        return null;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "... (truncated)";
    }
}
