package api.equinix.javasdk.mcp.auth;

import api.equinix.javasdk.mcp.McpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * OAuth 2.1 public-client authenticator for the Equinix MCP servers, driven by a
 * long-lived refresh token obtained once via the device-code flow ({@link McpLogin}).
 *
 * <p>Live evidence (probed 2026-07-20): {@code https://as.equinix.com} — the authorization
 * server named by {@code mcp.equinix.com}'s protected-resource metadata — supports the
 * {@code refresh_token} grant for public clients ({@code token_endpoint_auth_method}
 * {@code "none"}): the client authenticates with only its {@code client_id} in the
 * form-encoded body, no secret. This class exchanges the stored refresh token for
 * short-lived access tokens at {@value #DEFAULT_TOKEN_ENDPOINT}, caches them until
 * shortly before expiry, and transparently re-exchanges on {@link #invalidate()}.</p>
 *
 * <p><strong>Refresh-token rotation:</strong> when the AS returns a new
 * {@code refresh_token} alongside the access token, this authenticator adopts it for all
 * subsequent refreshes and notifies the optional {@code onRefreshTokenRotated} callback so
 * the caller can persist it (e.g. back into {@code .env.local}). Losing a rotated token
 * means re-running {@link McpLogin}, so persist it when the AS rotates.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // clientId + refreshToken come from .env.local, written by McpLogin
 * Mcp mcp = new Mcp(McpClientConfig.deviceAuth(clientId, refreshToken));
 * }</pre>
 *
 * @author ianjones
 * @see McpLogin
 * @see McpAuthenticator
 */
public class DeviceCodeAuthenticator implements McpAuthenticator, Closeable {

    /** Live-verified token endpoint of {@code https://as.equinix.com} (2026-07-20). */
    public static final String DEFAULT_TOKEN_ENDPOINT = "https://as.equinix.com/oauth2/token";

    private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final String clientId;
    private final String tokenEndpoint;
    private final Consumer<String> onRefreshTokenRotated;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String refreshToken;
    private String accessToken;
    private Instant expiresAt;
    private CloseableHttpClient httpClient;

    /**
     * Creates an authenticator against the live {@code as.equinix.com} token endpoint
     * with no rotation callback.
     *
     * @param clientId the public client id registered by {@link McpLogin}
     * @param refreshToken the refresh token minted by the device-code flow
     */
    public DeviceCodeAuthenticator(String clientId, String refreshToken) {
        this(clientId, refreshToken, DEFAULT_TOKEN_ENDPOINT, null);
    }

    /**
     * Creates an authenticator against the live {@code as.equinix.com} token endpoint.
     *
     * @param clientId the public client id registered by {@link McpLogin}
     * @param refreshToken the refresh token minted by the device-code flow
     * @param onRefreshTokenRotated invoked with the new refresh token whenever the AS
     *        rotates it; persist the value or the next process start will fail
     */
    public DeviceCodeAuthenticator(String clientId, String refreshToken, Consumer<String> onRefreshTokenRotated) {
        this(clientId, refreshToken, DEFAULT_TOKEN_ENDPOINT, onRefreshTokenRotated);
    }

    /**
     * Creates an authenticator with an explicit token endpoint (tests, private gateways).
     *
     * @param clientId the public client id
     * @param refreshToken the refresh token
     * @param tokenEndpoint the OAuth token endpoint URL
     * @param onRefreshTokenRotated optional rotation callback, may be {@code null}
     */
    public DeviceCodeAuthenticator(String clientId, String refreshToken, String tokenEndpoint,
                                   Consumer<String> onRefreshTokenRotated) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken");
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint, "tokenEndpoint");
        this.onRefreshTokenRotated = onRefreshTokenRotated;
    }

    /**
     * Returns a valid access token, exchanging the refresh token if the cached one is
     * absent or within {@code 60s} of expiry.
     */
    @Override
    public synchronized String bearerToken() {
        if (accessToken != null && expiresAt != null && Instant.now().isBefore(expiresAt.minus(REFRESH_BUFFER))) {
            return accessToken;
        }
        return exchangeRefreshToken();
    }

    /**
     * Discards the cached access token (the refresh token is kept). The next
     * {@link #bearerToken()} call performs a fresh refresh-token exchange.
     */
    @Override
    public synchronized void invalidate() {
        this.accessToken = null;
        this.expiresAt = null;
    }

    /**
     * Returns the refresh token currently in use — the constructor-supplied one, or the
     * latest the AS rotated in. Persist this across restarts.
     */
    public synchronized String getCurrentRefreshToken() {
        return refreshToken;
    }

    /** Returns the public client id this authenticator presents. */
    public String getClientId() {
        return clientId;
    }

    @Override
    public synchronized void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    private String exchangeRefreshToken() {
        try {
            HttpPost request = new HttpPost(tokenEndpoint);
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("grant_type", "refresh_token"));
            form.add(new BasicNameValuePair("refresh_token", refreshToken));
            // Public client: client_id in the body, no client_secret (AS auth method "none").
            form.add(new BasicNameValuePair("client_id", clientId));
            request.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client().execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                        : "";

                if (statusCode != 200) {
                    throw new McpException("Refresh-token exchange at " + tokenEndpoint
                            + " failed with HTTP " + statusCode + ": " + responseBody
                            + " - the stored refresh token may be expired or revoked;"
                            + " re-run the device-code login"
                            + " (java -cp target/classes api.equinix.javasdk.mcp.auth.McpLogin)"
                            + " to mint a fresh one.");
                }

                JsonNode json = objectMapper.readTree(responseBody);
                JsonNode token = json.get("access_token");
                if (token == null || token.asText().isBlank()) {
                    throw new McpException("Token endpoint " + tokenEndpoint
                            + " returned HTTP 200 but no access_token: " + responseBody);
                }
                this.accessToken = token.asText();
                int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
                this.expiresAt = Instant.now().plusSeconds(expiresIn);

                adoptRotatedRefreshToken(json);
                return this.accessToken;
            }
        } catch (McpException e) {
            throw e;
        } catch (IOException e) {
            throw new McpException("Refresh-token exchange at " + tokenEndpoint + " failed: " + e.getMessage(), e);
        }
    }

    private void adoptRotatedRefreshToken(JsonNode json) {
        JsonNode rotated = json.get("refresh_token");
        if (rotated != null && !rotated.asText().isBlank() && !rotated.asText().equals(refreshToken)) {
            this.refreshToken = rotated.asText();
            if (onRefreshTokenRotated != null) {
                onRefreshTokenRotated.accept(this.refreshToken);
            }
        }
    }

    private synchronized CloseableHttpClient client() {
        if (httpClient == null) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(CONNECT_TIMEOUT_MS)
                    .setSocketTimeout(READ_TIMEOUT_MS)
                    .build();
            httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        }
        return httpClient;
    }
}
