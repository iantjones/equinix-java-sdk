package api.equinix.javasdk.fabric.mcp.bridge;

import api.equinix.javasdk.fabric.mcp.McpClient;
import api.equinix.javasdk.fabric.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed bridge for MCP Cloud Router-related tools.
 *
 * <p>Wraps Cloud Router search, package lookup, and diagnostic command tools
 * for integration with the Deployment Wizard.</p>
 *
 * @author ianjones
 */
public class McpCloudRouterBridge {

    private final McpClient client;

    McpCloudRouterBridge(McpClient client) {
        this.client = client;
    }

    /**
     * Searches for Cloud Routers using advanced filtering.
     *
     * @param filters the search filters
     * @return list of matching Cloud Routers
     */
    public List<McpCloudRouter> searchRouters(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_router", filters);
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        List<McpCloudRouter> routers = new ArrayList<>();

        if (json != null && json.has("data") && json.get("data").isArray()) {
            for (JsonNode node : json.get("data")) {
                routers.add(parseRouter(node));
            }
        }
        return routers;
    }

    /**
     * Retrieves details for a specific Cloud Router package.
     *
     * @param packageCode the package code (e.g., "STANDARD", "PREMIUM")
     * @return the package details as JSON
     */
    public JsonNode getRouterPackage(String packageCode) {
        McpToolResult result = client.callTool("get_router_package",
                Map.of("routerPackageCode", packageCode));
        return result.getJsonContent(client.getObjectMapper());
    }

    /**
     * Executes a diagnostic command (ping/traceroute) on a Cloud Router.
     *
     * @param routerUuid the Cloud Router UUID
     * @param commandType the command type ("ping" or "traceroute")
     * @param target the target host or IP address
     * @return the command result as JSON
     */
    public JsonNode executeCommand(String routerUuid, String commandType, String target) {
        McpToolResult result = client.callTool("create_router_commands", Map.of(
                "routerUuid", routerUuid,
                "type", commandType,
                "host", target
        ));
        return result.getJsonContent(client.getObjectMapper());
    }

    private McpCloudRouter parseRouter(JsonNode json) {
        return new McpCloudRouter(
                getTextOrNull(json, "uuid"),
                getTextOrNull(json, "name"),
                getTextOrNull(json, "state"),
                getTextOrNull(json, "package"),
                json.has("location") && json.get("location").has("metroCode")
                        ? json.get("location").get("metroCode").asText() : null,
                json.has("connectionsCount") ? json.get("connectionsCount").asInt() : 0,
                json
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /**
     * Typed representation of a Fabric Cloud Router from the MCP server.
     */
    public static class McpCloudRouter {
        private final String uuid;
        private final String name;
        private final String state;
        private final String packageType;
        private final String metroCode;
        private final int connectionsCount;
        private final JsonNode rawJson;

        McpCloudRouter(String uuid, String name, String state, String packageType,
                       String metroCode, int connectionsCount, JsonNode rawJson) {
            this.uuid = uuid;
            this.name = name;
            this.state = state;
            this.packageType = packageType;
            this.metroCode = metroCode;
            this.connectionsCount = connectionsCount;
            this.rawJson = rawJson;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public String getState() { return state; }
        public String getPackageType() { return packageType; }
        public String getMetroCode() { return metroCode; }
        public int getConnectionsCount() { return connectionsCount; }
        public JsonNode getRawJson() { return rawJson; }

        @Override
        public String toString() {
            return "McpCloudRouter{uuid='" + uuid + "', name='" + name + "', metro='" + metroCode + "'}";
        }
    }
}
