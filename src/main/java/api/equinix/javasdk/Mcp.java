package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.exception.EquinixRateLimitException;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.mcp.McpClientConfig;
import api.equinix.javasdk.mcp.McpException;
import api.equinix.javasdk.mcp.McpTokenManager;
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
 * JSON-RPC 2.0 client for Equinix MCP (Model Context Protocol) servers — the root-level
 * entry point for MCP, alongside {@link Fabric}, {@link NetworkEdge}, {@link IAM}, and the
 * other domain clients.
 *
 * <p>Provides programmatic access to the Equinix Fabric and Peering Insights MCP servers,
 * enabling tool invocation via the standard MCP protocol. This client handles JSON-RPC
 * serialization, OAuth2 token management, and error mapping.</p>
 *
 * <p>Follows the same standalone HTTP client pattern as
 * {@link api.equinix.javasdk.design.peering.client.PeeringDbClient}, using Apache HttpClient
 * and Jackson independently from the SDK's core HTTP infrastructure.</p>
 *
 * <p>To expose <em>this SDK's</em> Fabric resources as MCP tools (the server/bridge side)
 * rather than consume an external MCP server, use {@code Fabric.mcp()} instead.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * Mcp mcp = new Mcp(credentials);
 * mcp.initialize();
 *
 * // List available tools
 * Map<String, McpToolDefinition> tools = mcp.listTools();
 *
 * // Invoke a tool
 * McpToolResult result = mcp.callTool("list_metro", Map.of());
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
    private final McpTokenManager tokenManager;
    private final Map<String, McpToolDefinition> toolRegistry = new ConcurrentHashMap<>();

    private boolean initialized = false;

    /**
     * Creates an MCP client with default configuration.
     *
     * @param credentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Mcp(EquinixCredentials credentials) {
        this(credentials, McpClientConfig.defaults());
    }

    /**
     * Creates an MCP client with custom configuration.
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

        this.tokenManager = new McpTokenManager(credentials, config.getTokenEndpoint(),
                this.httpClient, this.objectMapper);
    }

    /**
     * Performs the MCP initialization handshake with the Fabric server.
     *
     * <p>Sends the {@code initialize} JSON-RPC request and verifies the server
     * supports the expected protocol version. Must be called before invoking tools.</p>
     *
     * @throws McpException if initialization fails
     */
    public void initialize() {
        McpJsonRpcRequest request = McpJsonRpcRequest.initialize(
                "equinix-java-sdk", "1.2.0");

        executeRpc(config.getFabricEndpoint(), request);
        this.initialized = true;
    }

    /**
     * Retrieves the list of available MCP tools from the Fabric server and caches them.
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
     * Invokes an MCP tool on the Fabric server.
     *
     * @param toolName the name of the tool to invoke (e.g., "get_metro", "search_connection")
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
     * Returns whether this client has been initialized.
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
            httpPost.setHeader("Authorization", "Bearer " + tokenManager.getToken());

            String body = objectMapper.writeValueAsString(rpcRequest);
            httpPost.setEntity(new StringEntity(body, "UTF-8"));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(httpResponse.getEntity());

                if (statusCode == 401) {
                    tokenManager.invalidate();
                    // Retry once with fresh token
                    httpPost.setHeader("Authorization", "Bearer " + tokenManager.getToken());
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

    private void ensureInitialized() {
        if (!initialized) {
            throw new McpException("Mcp not initialized. Call initialize() before invoking tools.");
        }
    }
}
