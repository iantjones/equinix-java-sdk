package api.equinix.javasdk.mcp;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the MCP client including endpoint URLs, timeouts, and retry policy.
 *
 * <p>Use the builder for customization, or call {@link #defaults()} for standard settings
 * that point to the production Equinix MCP server endpoints.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * McpClientConfig config = McpClientConfig.builder()
 *     .fabricEndpoint("https://mcp.equinix.com/fabric")
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

    @Builder.Default
    private String tokenEndpoint = DEFAULT_TOKEN_ENDPOINT;

    @Builder.Default
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;

    @Builder.Default
    private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;

    @Builder.Default
    private int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Returns a configuration with all default values.
     *
     * @return the default configuration
     */
    public static McpClientConfig defaults() {
        return McpClientConfig.builder().build();
    }
}
