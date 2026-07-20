package api.equinix.javasdk.mcp;

import api.equinix.javasdk.mcp.auth.DeviceCodeAuthenticator;
import api.equinix.javasdk.mcp.auth.McpAuthenticator;
import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the MCP client including endpoint URLs, timeouts, retry policy,
 * and the authenticator that supplies bearer tokens.
 *
 * <p>Use the builder for customization, or call {@link #defaults()} for standard settings
 * that point to the production Equinix MCP server endpoints.</p>
 *
 * <p><strong>Authentication:</strong> the documented MCP servers accept only OAuth 2.1
 * tokens from {@code https://as.equinix.com} (no {@code client_credentials} grant —
 * live-verified 2026-07-20), so for real use build the config with
 * {@link #deviceAuth(String, String)} using the credentials {@code McpLogin} wrote to
 * {@code .env.local}. When no {@link #getAuthenticator() authenticator} is set, the
 * {@code Mcp} client falls back to the legacy client-credentials authenticator built
 * from its {@code EquinixCredentials} — kept for byte-compatibility and custom-gateway
 * overrides only; it cannot authenticate against {@code mcp.equinix.com}.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Documented servers: device-code credentials from McpLogin / .env.local
 * McpClientConfig config = McpClientConfig.deviceAuth(clientId, refreshToken);
 *
 * // Or full customization
 * McpClientConfig config = McpClientConfig.builder()
 *     .fabricEndpoint("https://mcp.equinix.com/fabric")
 *     .authenticator(new DeviceCodeAuthenticator(clientId, refreshToken))
 *     .connectTimeoutMs(10_000)
 *     .build();
 * }</pre>
 *
 * @author ianjones
 */
@Getter
@Builder
public class McpClientConfig {

    private static final String DEFAULT_FABRIC_ENDPOINT = "https://mcp.equinix.com/fabric";
    private static final String DEFAULT_PEERING_ENDPOINT = "https://mcp.equinix.com/peeringInsights";
    private static final String DEFAULT_TOKEN_ENDPOINT = "https://api.equinix.com/oauth2/v1/token";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_MAX_RETRIES = 2;

    @Builder.Default
    private String fabricEndpoint = DEFAULT_FABRIC_ENDPOINT;

    @Builder.Default
    private String peeringInsightsEndpoint = DEFAULT_PEERING_ENDPOINT;

    /**
     * Legacy client-credentials token endpoint, used only by the fallback
     * {@code ClientCredentialsAuthenticator} when no {@link #getAuthenticator()
     * authenticator} is configured. Irrelevant for {@link #deviceAuth(String, String)}
     * configs, whose {@link DeviceCodeAuthenticator} talks to
     * {@code https://as.equinix.com/oauth2/token}.
     */
    @Builder.Default
    private String tokenEndpoint = DEFAULT_TOKEN_ENDPOINT;

    @Builder.Default
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;

    @Builder.Default
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;

    @Builder.Default
    private int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * The authenticator supplying bearer tokens for MCP requests. When {@code null}
     * (the default), {@code Mcp} builds a legacy {@code ClientCredentialsAuthenticator}
     * from the {@code EquinixCredentials} passed to its constructor — which the
     * documented servers reject; see the class javadoc.
     */
    private McpAuthenticator authenticator;

    /**
     * Returns a configuration with all default values.
     *
     * @return the default configuration
     */
    public static McpClientConfig defaults() {
        return McpClientConfig.builder().build();
    }

    /**
     * Returns a default-endpoint configuration authenticated with the OAuth 2.1
     * device-flow credentials that {@code McpLogin} wrote to {@code .env.local}
     * ({@code EQUINIX_MCP_CLIENT_ID} / {@code EQUINIX_MCP_REFRESH_TOKEN}).
     *
     * @param clientId the public client id registered by {@code McpLogin}
     * @param refreshToken the refresh token minted by the device-code flow
     * @return a configuration using a {@link DeviceCodeAuthenticator}
     */
    public static McpClientConfig deviceAuth(String clientId, String refreshToken) {
        return McpClientConfig.builder()
                .authenticator(new DeviceCodeAuthenticator(clientId, refreshToken))
                .build();
    }
}
