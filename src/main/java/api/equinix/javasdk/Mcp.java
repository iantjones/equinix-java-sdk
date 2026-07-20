package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.exception.EquinixRateLimitException;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.mcp.McpClientConfig;
import api.equinix.javasdk.mcp.McpException;
import api.equinix.javasdk.mcp.auth.ClientCredentialsAuthenticator;
import api.equinix.javasdk.mcp.auth.McpAuthenticator;
import api.equinix.javasdk.mcp.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-RPC 2.0 client for the Equinix MCP (Model Context Protocol) servers — the root-level
 * entry point for MCP, alongside {@link Fabric}, {@link NetworkEdge}, {@link IAM}, and the
 * other domain clients.
 *
 * <p><strong>BETA — private-beta service, unstable client API.</strong> The Equinix MCP
 * servers are in Private Beta (access via {@code fabric-intelligence-support@equinix.com} or
 * your Equinix account representative), and this client should be treated as beta: its API
 * may change incompatibly between releases. The {@code peeringInsights} endpoint reached by
 * {@link #callPeeringTool(String, Map)} is <em>undocumented and experimental</em> — only the
 * Fabric server appears in Equinix's MCP documentation — and may change or disappear without
 * notice.</p>
 *
 * <p>This class is a <em>client-side</em> bridge: it consumes the remote Fabric MCP server at
 * {@code https://mcp.equinix.com/fabric}. The SDK does not run an MCP server of its own, and
 * neither this class nor {@code Fabric.mcp()} exposes the SDK's resources as MCP tools —
 * {@code Fabric.mcp()} simply wraps this same client in typed helpers
 * ({@code api.equinix.javasdk.mcp.bridge.McpBridge}).</p>
 *
 * <p><strong>Authentication — live-verified against the server on 2026-07-20.</strong> The
 * server demands OAuth 2.1 bearer tokens issued by {@code https://as.equinix.com}
 * (authorization-code and refresh-token grants only, PKCE S256, dynamic client registration).
 * That authorization server offers no {@code client_credentials} grant, so the SDK's regular
 * {@code api.equinix.com} client-credentials tokens can never be accepted here. Sign in
 * interactively with the device-code flow via {@code McpLogin}.</p>
 *
 * <p>The client handles JSON-RPC serialization, bearer-token attachment, and error mapping.
 * It follows the same standalone HTTP client pattern as
 * {@link api.equinix.javasdk.design.peering.client.PeeringDbClient}, using Apache HttpClient
 * and Jackson independently from the SDK's core HTTP infrastructure.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // clientId + refreshToken from .env.local, written once by McpLogin's device-code flow
 * Mcp mcp = new Mcp(McpClientConfig.deviceAuth(clientId, refreshToken));
 *
 * // List available tools — the client initializes itself on first use
 * Map<String, McpToolDefinition> tools = mcp.listTools();
 *
 * // Invoke a tool
 * McpToolResult result = mcp.callTool("list_metros", Map.of());
 * System.out.println(result.getTextContent());
 * }</pre>
 *
 * @author ianjones
 * @see McpClientConfig
 * @see McpToolResult
 */
public class Mcp implements Closeable {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final McpClientConfig config;
    private final McpAuthenticator authenticator;
    private final Map<String, McpToolDefinition> toolRegistry = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    /**
     * Creates an MCP client with default configuration and the legacy client-credentials
     * authenticator.
     *
     * <p><strong>Note (live-verified 2026-07-20):</strong> the documented MCP servers do not
     * accept client-credentials tokens — see the class javadoc. Prefer
     * {@link #Mcp(McpClientConfig)} with {@code McpClientConfig.deviceAuth(clientId, refreshToken)}.</p>
     *
     * @param credentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Mcp(EquinixCredentials credentials) {
        this(credentials, McpClientConfig.defaults());
    }

    /**
     * Creates an MCP client whose configuration carries its own
     * {@link api.equinix.javasdk.mcp.auth.McpAuthenticator} — the standard entry point for
     * the OAuth 2.1 device-flow credentials written by
     * {@code api.equinix.javasdk.mcp.auth.McpLogin}:
     *
     * <pre>{@code
     * Mcp mcp = new Mcp(McpClientConfig.deviceAuth(clientId, refreshToken));
     * }</pre>
     *
     * @param config the client configuration; {@code config.getAuthenticator()} must be set
     * @throws McpException if the configuration carries no authenticator
     */
    public Mcp(McpClientConfig config) {
        this((EquinixCredentials) null, requireAuthenticator(config));
    }

    private static McpClientConfig requireAuthenticator(McpClientConfig config) {
        if (config.getAuthenticator() == null) {
            throw new McpException("Mcp(McpClientConfig) requires config.authenticator to be set - use"
                    + " McpClientConfig.deviceAuth(clientId, refreshToken) (credentials from McpLogin /"
                    + " .env.local), or the Mcp(EquinixCredentials, McpClientConfig) constructor for the"
                    + " legacy client-credentials fallback.");
        }
        return config;
    }

    /**
     * Creates an MCP client with custom configuration. When the configuration carries an
     * {@code authenticator}, it is used as-is (and {@code credentials} may be {@code null});
     * otherwise the legacy client-credentials authenticator is built from
     * {@code credentials} and {@code config.getTokenEndpoint()}.
     *
     * @param credentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the client configuration
     */
    public Mcp(EquinixCredentials credentials, McpClientConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getConnectTimeoutMs())
                .setSocketTimeout(config.getReadTimeoutMs())
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        McpAuthenticator configured = config.getAuthenticator();
        this.authenticator = configured != null
                ? configured
                : new ClientCredentialsAuthenticator(credentials, config.getTokenEndpoint(),
                        this.httpClient, this.objectMapper);
    }

    /**
     * Performs the MCP initialization handshake with the Fabric server.
     *
     * <p>Sends the {@code initialize} JSON-RPC request and validates the
     * {@code protocolVersion} the server returns against the version this client offered:
     * if the server names a different version, an {@link McpException} identifying both
     * versions is thrown. A response that omits {@code protocolVersion} is accepted
     * as-is (nothing to validate against).</p>
     *
     * <p>Calling this explicitly is optional — the client initializes itself on the first
     * {@link #listTools()} / {@link #callTool(String, Map)} invocation. Calling it again
     * re-runs the handshake.</p>
     *
     * @throws McpException if initialization fails or the server's protocol version does
     *         not match the client's
     */
    public synchronized void initialize() {
        McpJsonRpcRequest request = McpJsonRpcRequest.initialize("equinix-java-sdk", sdkVersion());
        Object offeredVersion = request.getParams().get("protocolVersion");

        McpJsonRpcResponse response = executeRpc(config.getFabricEndpoint(), request);

        JsonNode result = response.getResult();
        String serverVersion = result == null ? null : result.path("protocolVersion").asText(null);
        if (serverVersion != null && !serverVersion.equals(offeredVersion)) {
            throw new McpException("MCP protocol version mismatch: client offered '" + offeredVersion
                    + "' but server returned '" + serverVersion + "'");
        }
        this.initialized = true;
    }

    private static String sdkVersion() {
        String version = Mcp.class.getPackage().getImplementationVersion();
        return version != null ? version : "2.0.0";
    }

    /**
     * Retrieves the list of available MCP tools from the Fabric server and caches them.
     * Runs the initialization handshake first if the client is not yet initialized.
     *
     * @return an unmodifiable map of tool name to tool definition
     * @throws McpException if the request fails
     */
    public Map<String, McpToolDefinition> listTools() {
        ensureInitialized();

        McpJsonRpcResponse response = executeRpc(config.getFabricEndpoint(),
                McpJsonRpcRequest.toolsList());

        JsonNode toolsNode = response.getResult().get("tools");
        if (toolsNode != null && toolsNode.isArray()) {
            List<McpToolDefinition> tools = objectMapper.convertValue(toolsNode,
                    new TypeReference<List<McpToolDefinition>>() {});
            toolRegistry.clear();
            for (McpToolDefinition tool : tools) {
                toolRegistry.put(tool.getName(), tool);
            }
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(toolRegistry));
    }

    /**
     * Invokes an MCP tool on the Fabric server. Runs the initialization handshake first
     * if the client is not yet initialized.
     *
     * @param toolName the name of the tool to invoke (e.g., "get_metro", "search_connections")
     * @param arguments the tool arguments as key-value pairs
     * @return the tool result
     * @throws McpException if the tool invocation fails or the tool returns an error
     */
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        return callTool(config.getFabricEndpoint(), toolName, arguments);
    }

    /**
     * Invokes an MCP tool on the Peering Insights server.
     *
     * <p><strong>Experimental:</strong> the {@code peeringInsights} endpoint appears in no
     * Equinix MCP documentation (only the Fabric server is documented) and may change or
     * disappear without notice.</p>
     *
     * @param toolName the name of the tool to invoke
     * @param arguments the tool arguments as key-value pairs
     * @return the tool result
     * @throws McpException if the tool invocation fails
     */
    public McpToolResult callPeeringTool(String toolName, Map<String, Object> arguments) {
        return callTool(config.getPeeringInsightsEndpoint(), toolName, arguments);
    }

    /**
     * Returns the cached tool registry from the last {@link #listTools()} call.
     *
     * @return unmodifiable map of tool name to definition, or empty if not yet loaded
     */
    public Map<String, McpToolDefinition> getToolRegistry() {
        return Collections.unmodifiableMap(toolRegistry);
    }

    /**
     * Returns whether the initialization handshake has completed — either explicitly via
     * {@link #initialize()} or lazily on the first tools call.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the Jackson ObjectMapper used by this client.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void close() throws IOException {
        // A config-supplied authenticator (e.g. DeviceCodeAuthenticator) owns its own HTTP
        // client; the fallback ClientCredentialsAuthenticator shares ours and is not Closeable.
        if (authenticator instanceof Closeable closeableAuthenticator) {
            closeableAuthenticator.close();
        }
        httpClient.close();
    }

    private McpToolResult callTool(String endpoint, String toolName, Map<String, Object> arguments) {
        ensureInitialized();

        McpJsonRpcRequest request = McpJsonRpcRequest.toolCall(toolName, arguments);
        McpJsonRpcResponse response = executeRpc(endpoint, request);

        McpToolResult result = objectMapper.convertValue(response.getResult(), McpToolResult.class);
        if (result.isError()) {
            throw new McpException("Tool '" + toolName + "' returned an error: " + result.getTextContent());
        }
        return result;
    }

    private McpJsonRpcResponse executeRpc(String endpoint, McpJsonRpcRequest rpcRequest) {
        int retries = 0;
        while (true) {
            try {
                return doExecuteRpc(endpoint, rpcRequest);
            } catch (EquinixRateLimitException | EquinixServerException e) {
                if (retries >= config.getMaxRetries()) throw e;
                retries++;
                try { Thread.sleep(1000L * retries); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new McpException("Interrupted during retry", ie);
                }
            }
        }
    }

    private McpJsonRpcResponse doExecuteRpc(String endpoint, McpJsonRpcRequest rpcRequest) {
        try {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "equinix-java-sdk/Mcp");
            httpPost.setHeader("Authorization", "Bearer " + authenticator.bearerToken());

            String body = objectMapper.writeValueAsString(rpcRequest);
            httpPost.setEntity(new StringEntity(body, "UTF-8"));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(httpResponse.getEntity());

                if (statusCode == 401) {
                    authenticator.invalidate();
                    // Retry once with fresh token
                    httpPost.setHeader("Authorization", "Bearer " + authenticator.bearerToken());
                    try (CloseableHttpResponse retryResponse = httpClient.execute(httpPost)) {
                        statusCode = retryResponse.getStatusLine().getStatusCode();
                        responseBody = EntityUtils.toString(retryResponse.getEntity());
                        if (statusCode == 401) {
                            throw new McpException("Authentication failed after token refresh");
                        }
                    }
                }

                if (statusCode == 429) {
                    throw new EquinixRateLimitException("MCP rate limit exceeded", 429, endpoint, null, null);
                }

                if (statusCode >= 500) {
                    throw new EquinixServerException("MCP server error: HTTP " + statusCode, statusCode, endpoint, null, null);
                }

                if (statusCode != 200) {
                    throw new McpException("MCP request failed with HTTP " + statusCode + ": " + responseBody);
                }

                McpJsonRpcResponse rpcResponse = objectMapper.readValue(responseBody, McpJsonRpcResponse.class);

                if (rpcResponse.isError()) {
                    throw McpException.fromJsonRpcError(rpcResponse.getError());
                }

                return rpcResponse;
            }
        } catch (McpException | EquinixRateLimitException | EquinixServerException e) {
            throw e;
        } catch (IOException e) {
            throw new McpException("MCP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Lazily runs the initialization handshake on first use. Synchronized (as is
     * {@link #initialize()}) so concurrent first calls perform a single handshake.
     */
    private synchronized void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }
}
