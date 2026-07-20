package api.equinix.javasdk.mcp.auth;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.mcp.McpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Legacy {@code client_credentials}-grant authenticator (formerly {@code McpTokenManager}).
 *
 * <p><strong>This authenticator cannot work against the documented Equinix MCP
 * servers.</strong> Live evidence (probed 2026-07-20): {@code https://mcp.equinix.com/fabric}
 * points resource-metadata at the authorization server {@code https://as.equinix.com},
 * whose {@code /.well-known/oauth-authorization-server} metadata advertises
 * {@code grant_types_supported=["authorization_code","refresh_token"]} — no
 * {@code client_credentials} grant at all. The SDK's regular {@code api.equinix.com}
 * access-key/secret tokens are therefore never accepted by {@code mcp.equinix.com};
 * unauthenticated calls get an Apigee-style 401 naming that AS.</p>
 *
 * <p>It is retained only as a config-override escape hatch (e.g. a private gateway that
 * fronts an MCP server with classic client-credentials tokens) and as the byte-compatible
 * default wiring for {@code new Mcp(credentials)}. For the real servers, log in once with
 * {@link McpLogin} and use {@link DeviceCodeAuthenticator} (see
 * {@code McpClientConfig.deviceAuth(clientId, refreshToken)}).</p>
 *
 * <p>Tokens are cached and refreshed 5 minutes before expiry.</p>
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class ClientCredentialsAuthenticator implements McpAuthenticator {

    private static final Duration REFRESH_BUFFER = Duration.ofMinutes(5);

    private final EquinixCredentials credentials;
    private final String tokenEndpoint;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant expiresAt;

    /**
     * Returns a valid access token, refreshing if necessary.
     *
     * @return the OAuth2 bearer token
     * @throws McpException if token acquisition fails
     */
    @Override
    public synchronized String bearerToken() {
        if (accessToken != null && expiresAt != null && Instant.now().isBefore(expiresAt.minus(REFRESH_BUFFER))) {
            return accessToken;
        }
        return refreshToken();
    }

    /**
     * Forces a token refresh regardless of expiry.
     *
     * @return the new OAuth2 bearer token
     * @throws McpException if token acquisition fails
     */
    synchronized String refreshToken() {
        try {
            HttpPost request = new HttpPost(tokenEndpoint);
            request.setHeader("Content-Type", "application/json");

            String body = objectMapper.writeValueAsString(new TokenRequest(
                    credentials.getAccessKey(),
                    credentials.getSecretKey(),
                    "client_credentials"
            ));
            request.setEntity(new StringEntity(body));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    throw new McpException("OAuth2 token request failed with HTTP " + statusCode + ": " + responseBody);
                }

                JsonNode json = objectMapper.readTree(responseBody);
                this.accessToken = json.get("access_token").asText();
                int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
                this.expiresAt = Instant.now().plusSeconds(expiresIn);

                return this.accessToken;
            }
        } catch (McpException e) {
            throw e;
        } catch (IOException e) {
            throw new McpException("Failed to acquire OAuth2 token", e);
        }
    }

    /**
     * Clears the cached token, forcing a fresh acquisition on next call.
     */
    @Override
    public synchronized void invalidate() {
        this.accessToken = null;
        this.expiresAt = null;
    }

    private record TokenRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("client_id") String clientId,
            @com.fasterxml.jackson.annotation.JsonProperty("client_secret") String clientSecret,
            @com.fasterxml.jackson.annotation.JsonProperty("grant_type") String grantType) {
    }
}
