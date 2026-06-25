package api.equinix.javasdk.fabric.mcp;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Manages OAuth2 access token lifecycle for MCP server authentication.
 *
 * <p>Tokens are cached and automatically refreshed 5 minutes before their expiry.
 * Uses the same OAuth2 client credentials as the core Equinix SDK.</p>
 *
 * @author ianjones
 */
class McpTokenManager {

    private static final Duration REFRESH_BUFFER = Duration.ofMinutes(5);

    private final EquinixCredentials credentials;
    private final String tokenEndpoint;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant expiresAt;

    McpTokenManager(EquinixCredentials credentials, String tokenEndpoint,
                    CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.credentials = credentials;
        this.tokenEndpoint = tokenEndpoint;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     *
     * @return the OAuth2 bearer token
     * @throws McpException if token acquisition fails
     */
    synchronized String getToken() {
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
    synchronized void invalidate() {
        this.accessToken = null;
        this.expiresAt = null;
    }

    private record TokenRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("client_id") String clientId,
            @com.fasterxml.jackson.annotation.JsonProperty("client_secret") String clientSecret,
            @com.fasterxml.jackson.annotation.JsonProperty("grant_type") String grantType) {
    }
}
