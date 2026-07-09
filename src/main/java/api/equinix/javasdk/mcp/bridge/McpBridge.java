package api.equinix.javasdk.mcp.bridge;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.mcp.model.McpToolDefinition;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * High-level facade for Equinix MCP server interactions.
 *
 * <p>Provides domain-specific sub-bridges for metros, connections, Cloud Routers,
 * and observability, each offering typed Java methods wrapping the underlying MCP tools.
 * This is the primary entry point for MCP integration with the SDK's optimization
 * and resiliency modules.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * McpBridge mcp = fabric.mcp();
 *
 * // Query metro data via MCP
 * List<McpMetroBridge.McpMetro> metros = mcp.metros().listMetros();
 *
 * // Validate a connection configuration
 * McpConnectionBridge.McpConnectionValidation validation =
 *     mcp.connections().validateConnection(connectionSpec);
 *
 * // Get live metrics for a connection
 * McpObservabilityBridge.McpMetrics metrics =
 *     mcp.observability().getMetrics("connection", connUuid, "bandwidth", startTime, endTime);
 * }</pre>
 *
 * @author ianjones
 * @see McpMetroBridge
 * @see McpConnectionBridge
 * @see McpCloudRouterBridge
 * @see McpObservabilityBridge
 */
@RequiredArgsConstructor
public class McpBridge {

    private final Mcp client;
    private McpMetroBridge metroBridge;
    private McpConnectionBridge connectionBridge;
    private McpCloudRouterBridge cloudRouterBridge;
    private McpObservabilityBridge observabilityBridge;

    /**
     * Returns the metro bridge for querying Equinix metro data via MCP.
     *
     * @return the {@link McpMetroBridge}
     */
    public McpMetroBridge metros() {
        if (metroBridge == null) {
            metroBridge = new McpMetroBridge(client);
        }
        return metroBridge;
    }

    /**
     * Returns the connection bridge for validating and searching connections via MCP.
     *
     * @return the {@link McpConnectionBridge}
     */
    public McpConnectionBridge connections() {
        if (connectionBridge == null) {
            connectionBridge = new McpConnectionBridge(client);
        }
        return connectionBridge;
    }

    /**
     * Returns the Cloud Router bridge for searching and managing routers via MCP.
     *
     * @return the {@link McpCloudRouterBridge}
     */
    public McpCloudRouterBridge cloudRouters() {
        if (cloudRouterBridge == null) {
            cloudRouterBridge = new McpCloudRouterBridge(client);
        }
        return cloudRouterBridge;
    }

    /**
     * Returns the observability bridge for accessing metrics, streams, and events via MCP.
     *
     * @return the {@link McpObservabilityBridge}
     */
    public McpObservabilityBridge observability() {
        if (observabilityBridge == null) {
            observabilityBridge = new McpObservabilityBridge(client);
        }
        return observabilityBridge;
    }

    /**
     * Returns the available MCP tools, loading them from the server if not yet cached.
     *
     * @return map of tool name to tool definition
     */
    public Map<String, McpToolDefinition> availableTools() {
        Map<String, McpToolDefinition> registry = client.getToolRegistry();
        if (registry.isEmpty()) {
            return client.listTools();
        }
        return registry;
    }

    /**
     * Returns the underlying MCP client for direct tool invocation.
     *
     * @return the {@link Mcp}
     */
    public Mcp getClient() {
        return client;
    }
}
