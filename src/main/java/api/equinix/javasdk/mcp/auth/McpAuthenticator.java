package api.equinix.javasdk.mcp.auth;

/**
 * Supplies OAuth bearer tokens for the {@link api.equinix.javasdk.Mcp} client.
 *
 * <p>Live evidence (probed 2026-07-20): the Equinix MCP servers at
 * {@code https://mcp.equinix.com} demand tokens issued by the OAuth 2.1 authorization
 * server {@code https://as.equinix.com}, which advertises only the
 * {@code authorization_code} and {@code refresh_token} grants (PKCE S256, public
 * clients allowed). Use {@link DeviceCodeAuthenticator} — bootstrapped once via
 * {@link McpLogin} — for those servers. {@link ClientCredentialsAuthenticator} exists
 * only for legacy/custom-gateway overrides and cannot authenticate against the
 * documented MCP endpoints.</p>
 *
 * <p>Implementations must be thread-safe: {@code Mcp} may call {@code bearerToken()}
 * concurrently and calls {@code invalidate()} followed by {@code bearerToken()} when
 * the server answers 401.</p>
 *
 * @author ianjones
 */
public interface McpAuthenticator {

    /**
     * Returns a valid bearer token, acquiring or refreshing one if necessary.
     *
     * @return the OAuth bearer token (never {@code null})
     * @throws api.equinix.javasdk.mcp.McpException if token acquisition fails
     */
    String bearerToken();

    /**
     * Discards any cached access token so the next {@link #bearerToken()} call
     * acquires a fresh one. Called by the client after a 401 response.
     */
    void invalidate();
}
