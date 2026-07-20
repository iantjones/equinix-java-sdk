package api.equinix.javasdk.mcp.auth;

import api.equinix.javasdk.mcp.McpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time interactive login for the Equinix MCP servers — RFC 7591 dynamic client
 * registration followed by the RFC 8628 device-authorization ("device code") flow
 * against {@code https://as.equinix.com}, persisting the resulting credentials into
 * {@code .env.local} in the repo root.
 *
 * <p>Live evidence (probed 2026-07-20): the AS metadata at
 * {@code https://as.equinix.com/.well-known/oauth-authorization-server} advertises
 * {@code registration_endpoint=https://as.equinix.com/connect/register} (RFC 7591 DCR),
 * {@code device_authorization_endpoint=https://as.equinix.com/oauth2/device_authorization}
 * (RFC 8628), public clients ({@code token_endpoint_auth_method} {@code "none"}), and the
 * {@code authorization_code} + {@code refresh_token} grants. There is no
 * {@code client_credentials} grant — the SDK's {@code api.equinix.com} access keys can
 * never authenticate here, which is why this interactive login exists.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * java -cp target/classes api.equinix.javasdk.mcp.auth.McpLogin
 * </pre>
 *
 * <p>The tool prints a verification URL and user code, waits for you to approve the
 * device in a browser, then writes {@code EQUINIX_MCP_CLIENT_ID} and
 * {@code EQUINIX_MCP_REFRESH_TOKEN} into {@code .env.local} (created from
 * {@code .env.local.example} when absent; all other lines preserved). Afterwards run the
 * live probe: {@code mvn test -Plive-mcp}, or construct the client with
 * {@code new Mcp(McpClientConfig.deviceAuth(clientId, refreshToken))}.</p>
 *
 * <p>If the AS refuses the device grant entirely, the tool fails loud and prints
 * guidance for the authorization-code + PKCE alternative — it deliberately does not
 * automate a browser/loopback flow.</p>
 *
 * @author ianjones
 * @see DeviceCodeAuthenticator
 */
public final class McpLogin {

    static final String DEFAULT_REGISTRATION_ENDPOINT = "https://as.equinix.com/connect/register";
    static final String DEFAULT_DEVICE_AUTHORIZATION_ENDPOINT = "https://as.equinix.com/oauth2/device_authorization";
    static final String DEFAULT_TOKEN_ENDPOINT = "https://as.equinix.com/oauth2/token";

    static final String CLIENT_NAME = "equinix-java-sdk";
    static final String SCOPE = "openid profile email api:temporary-full";
    static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    static final String ENV_CLIENT_ID_KEY = "EQUINIX_MCP_CLIENT_ID";
    static final String ENV_REFRESH_TOKEN_KEY = "EQUINIX_MCP_REFRESH_TOKEN";

    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;
    private static final int SLOW_DOWN_BACKOFF_SECONDS = 5;
    private static final int DEFAULT_EXPIRES_IN_SECONDS = 900;

    private final String registrationEndpoint;
    private final String deviceAuthorizationEndpoint;
    private final String tokenEndpoint;
    private final Path envFile;
    private final PrintStream out;
    private final Sleeper sleeper;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Whether the AS accepted the device-code grant at registration time. */
    private boolean deviceGrantRegistered;

    /** Creates a login tool against the live {@code as.equinix.com} endpoints. */
    public McpLogin() {
        this(DEFAULT_REGISTRATION_ENDPOINT, DEFAULT_DEVICE_AUTHORIZATION_ENDPOINT, DEFAULT_TOKEN_ENDPOINT,
                Path.of(".env.local"), System.out, Thread::sleep);
    }

    /** Test/override constructor: explicit endpoints, env-file path, output stream, and sleeper. */
    McpLogin(String registrationEndpoint, String deviceAuthorizationEndpoint, String tokenEndpoint,
             Path envFile, PrintStream out, Sleeper sleeper) {
        this.registrationEndpoint = registrationEndpoint;
        this.deviceAuthorizationEndpoint = deviceAuthorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.envFile = envFile;
        this.out = out;
        this.sleeper = sleeper;
    }

    /**
     * Entry point: {@code java -cp target/classes api.equinix.javasdk.mcp.auth.McpLogin}.
     * Exits non-zero on failure.
     */
    public static void main(String[] args) {
        try {
            new McpLogin().login();
        } catch (Exception e) {
            System.err.println();
            System.err.println("MCP login FAILED: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the full flow: DCR registration, device authorization, token polling, and
     * {@code .env.local} writeback.
     *
     * @throws McpException on any authorization-server rejection (fail-loud, with guidance)
     */
    public void login() {
        out.println("Equinix MCP login - OAuth 2.1 device-code flow against " + tokenEndpoint);
        try (CloseableHttpClient http = newHttpClient()) {
            String clientId = registerClient(http);
            JsonNode deviceAuth = authorizeDevice(http, clientId);
            printUserAction(deviceAuth);
            TokenPair tokens = pollForTokens(http, clientId, deviceAuth);
            writeEnvFile(clientId, tokens.refreshToken());
            out.println();
            out.println("Login successful.");
            out.println("Wrote " + ENV_CLIENT_ID_KEY + " and " + ENV_REFRESH_TOKEN_KEY + " to " + envFile
                    + " (refresh token not displayed).");
            out.println("Next step - verify against the live server: mvn test -Plive-mcp");
        } catch (IOException e) {
            throw new McpException("MCP login failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McpException("MCP login interrupted while waiting for device approval", e);
        }
    }

    // ── Step 1: RFC 7591 dynamic client registration ─────────────────────────

    private String registerClient(CloseableHttpClient http) throws IOException {
        out.println();
        out.println("[1/3] Registering a public client at " + registrationEndpoint + " ...");

        HttpOutcome first = postRegistration(http, List.of(DEVICE_CODE_GRANT, "refresh_token"));
        if (first.success()) {
            deviceGrantRegistered = true;
            String clientId = requireField(first.json(), "client_id", "registration response");
            out.println("      Registered client_id " + clientId + " (device_code + refresh_token grants).");
            return clientId;
        }

        out.println("      The AS rejected registration with the device_code grant (HTTP "
                + first.status() + ": " + first.body() + ").");
        out.println("      Retrying registration with authorization_code + refresh_token ...");

        HttpOutcome second = postRegistration(http, List.of("authorization_code", "refresh_token"));
        if (second.success()) {
            deviceGrantRegistered = false;
            String clientId = requireField(second.json(), "client_id", "registration response");
            out.println("      Registered client_id " + clientId + " (authorization_code + refresh_token grants).");
            out.println("      NOTE: the device grant was refused at registration; the device-authorization"
                    + " step below may be refused too.");
            return clientId;
        }

        throw new McpException("Dynamic client registration at " + registrationEndpoint
                + " failed for both grant sets. device_code attempt: HTTP " + first.status() + ": " + first.body()
                + " | authorization_code attempt: HTTP " + second.status() + ": " + second.body()
                + ". Contact fabric-intelligence-support@equinix.com about Private Beta access.");
    }

    private HttpOutcome postRegistration(CloseableHttpClient http, List<String> grantTypes) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("client_name", CLIENT_NAME);
        ArrayNode grants = body.putArray("grant_types");
        grantTypes.forEach(grants::add);
        body.put("token_endpoint_auth_method", "none");
        body.put("scope", SCOPE);

        HttpPost post = new HttpPost(registrationEndpoint);
        post.setEntity(new StringEntity(mapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
        return execute(http, post);
    }

    // ── Step 2: RFC 8628 device authorization ────────────────────────────────

    private JsonNode authorizeDevice(CloseableHttpClient http, String clientId) throws IOException {
        out.println();
        out.println("[2/3] Requesting a device code from " + deviceAuthorizationEndpoint + " ...");

        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("client_id", clientId));
        form.add(new BasicNameValuePair("scope", SCOPE));
        HttpPost post = new HttpPost(deviceAuthorizationEndpoint);
        post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

        HttpOutcome outcome = execute(http, post);
        if (!outcome.success()) {
            String guidance = authorizationCodeGuidance(clientId);
            out.println(guidance);
            throw new McpException("The authorization server refused the device-authorization request"
                    + " (HTTP " + outcome.status() + ": " + outcome.body() + ")."
                    + (deviceGrantRegistered ? "" : " The device grant was already refused at registration, so"
                            + " this AS appears not to allow the device flow for dynamically registered clients.")
                    + "\n" + guidance);
        }
        return outcome.json();
    }

    private void printUserAction(JsonNode deviceAuth) {
        String userCode = requireField(deviceAuth, "user_code", "device-authorization response");
        JsonNode complete = deviceAuth.get("verification_uri_complete");
        String uri = complete != null && !complete.asText().isBlank()
                ? complete.asText()
                : requireField(deviceAuth, "verification_uri", "device-authorization response");

        out.println();
        out.println("  ============================ ACTION REQUIRED ============================");
        out.println("  Open this URL in a browser and approve the device:");
        out.println();
        out.println("      " + uri);
        out.println();
        out.println("  Verification code (confirm it matches what the page shows):");
        out.println();
        out.println("      " + userCode);
        out.println("  =========================================================================");
        out.println();
    }

    // ── Step 3: poll the token endpoint ──────────────────────────────────────

    private TokenPair pollForTokens(CloseableHttpClient http, String clientId, JsonNode deviceAuth)
            throws IOException, InterruptedException {
        String deviceCode = requireField(deviceAuth, "device_code", "device-authorization response");
        long intervalSeconds = deviceAuth.has("interval")
                ? deviceAuth.get("interval").asLong() : DEFAULT_POLL_INTERVAL_SECONDS;
        long expiresInSeconds = deviceAuth.has("expires_in")
                ? deviceAuth.get("expires_in").asLong() : DEFAULT_EXPIRES_IN_SECONDS;

        out.println("[3/3] Waiting for approval (polling every " + intervalSeconds + "s, code valid for "
                + expiresInSeconds + "s) ...");

        long elapsedSeconds = 0;
        while (true) {
            if (elapsedSeconds + intervalSeconds > expiresInSeconds) {
                throw new McpException("The device code expired after " + expiresInSeconds
                        + "s without approval. Re-run McpLogin and approve the device promptly.");
            }
            sleeper.sleep(intervalSeconds * 1000L);
            elapsedSeconds += intervalSeconds;

            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("grant_type", DEVICE_CODE_GRANT));
            form.add(new BasicNameValuePair("device_code", deviceCode));
            // Public client: client_id only, no secret (AS auth method "none").
            form.add(new BasicNameValuePair("client_id", clientId));
            HttpPost post = new HttpPost(tokenEndpoint);
            post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

            HttpOutcome outcome = execute(http, post);
            if (outcome.success()) {
                String accessToken = requireField(outcome.json(), "access_token", "token response");
                JsonNode refresh = outcome.json().get("refresh_token");
                if (refresh == null || refresh.asText().isBlank()) {
                    throw new McpException("The token endpoint granted an access token but no refresh_token: "
                            + outcome.body() + " - without one the SDK cannot re-authenticate non-interactively."
                            + " Verify the 'refresh_token' grant was registered and the AS issues refresh tokens"
                            + " for scope '" + SCOPE + "'.");
                }
                return new TokenPair(accessToken, refresh.asText());
            }

            String error = outcome.json() != null && outcome.json().has("error")
                    ? outcome.json().get("error").asText() : "";
            switch (error) {
                case "authorization_pending" -> { /* keep polling */ }
                case "slow_down" -> {
                    intervalSeconds += SLOW_DOWN_BACKOFF_SECONDS;
                    out.println("      AS asked to slow down - polling every " + intervalSeconds + "s now.");
                }
                case "access_denied" -> throw new McpException(
                        "Approval was denied by the user (or an administrator) - nothing was written."
                                + " Re-run McpLogin to try again.");
                case "expired_token" -> throw new McpException(
                        "The device code expired before approval. Re-run McpLogin and approve promptly.");
                default -> throw new McpException("Token polling at " + tokenEndpoint
                        + " failed with HTTP " + outcome.status() + ": " + outcome.body());
            }
        }
    }

    // ── .env.local writeback ─────────────────────────────────────────────────

    private void writeEnvFile(String clientId, String refreshToken) throws IOException {
        List<String> lines = readEnvTemplate();
        upsert(lines, ENV_CLIENT_ID_KEY, clientId);
        upsert(lines, ENV_REFRESH_TOKEN_KEY, refreshToken);
        if (envFile.getParent() != null) {
            Files.createDirectories(envFile.getParent());
        }
        Files.write(envFile, lines, StandardCharsets.UTF_8);
    }

    private List<String> readEnvTemplate() throws IOException {
        if (Files.isRegularFile(envFile)) {
            return new ArrayList<>(Files.readAllLines(envFile, StandardCharsets.UTF_8));
        }
        Path example = envFile.resolveSibling(envFile.getFileName().toString() + ".example");
        if (Files.isRegularFile(example)) {
            out.println("      " + envFile + " not found - creating it from " + example + ".");
            return new ArrayList<>(Files.readAllLines(example, StandardCharsets.UTF_8));
        }
        out.println("      Neither " + envFile + " nor " + example + " found - creating a fresh " + envFile + ".");
        List<String> fresh = new ArrayList<>();
        fresh.add("# Created by api.equinix.javasdk.mcp.auth.McpLogin - keep this file out of version control.");
        fresh.add(ENV_CLIENT_ID_KEY + "=");
        fresh.add(ENV_REFRESH_TOKEN_KEY + "=");
        return fresh;
    }

    /** Replaces the {@code KEY=...} line in place (first match), or appends one. */
    private static void upsert(List<String> lines, String key, String value) {
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith(key) && trimmed.substring(key.length()).trim().startsWith("=")) {
                lines.set(i, key + "=" + value);
                return;
            }
        }
        lines.add(key + "=" + value);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String authorizationCodeGuidance(String clientId) {
        return "The AS rejected the device flow. Alternative (not automated by this SDK):\n"
                + "  run an OAuth 2.1 authorization-code flow with PKCE (S256) against https://as.equinix.com\n"
                + "  (endpoints in https://as.equinix.com/.well-known/oauth-authorization-server), using\n"
                + "  client_id '" + clientId + "', scope '" + SCOPE + "', and a redirect URI you register,\n"
                + "  then exchange the code at " + DEFAULT_TOKEN_ENDPOINT + " and paste the refresh_token\n"
                + "  into .env.local as " + ENV_REFRESH_TOKEN_KEY + " (and the client_id as "
                + ENV_CLIENT_ID_KEY + ").";
    }

    private HttpOutcome execute(CloseableHttpClient http, HttpPost post) throws IOException {
        try (CloseableHttpResponse response = http.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                    : "";
            JsonNode json = null;
            if (!body.isBlank()) {
                try {
                    json = mapper.readTree(body);
                } catch (IOException notJson) {
                    // leave json null - callers fall back to the raw body
                }
            }
            return new HttpOutcome(status, body, json);
        }
    }

    private static String requireField(JsonNode json, String field, String context) {
        JsonNode value = json == null ? null : json.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new McpException("The " + context + " carries no '" + field + "': "
                    + (json == null ? "(non-JSON body)" : json.toString()));
        }
        return value.asText();
    }

    private static CloseableHttpClient newHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setSocketTimeout(30_000)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /** Injectable clock-free sleeper so tests can run the polling loop instantly. */
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }

    private record HttpOutcome(int status, String body, JsonNode json) {
        boolean success() {
            return status == 200 || status == 201;
        }
    }
}
