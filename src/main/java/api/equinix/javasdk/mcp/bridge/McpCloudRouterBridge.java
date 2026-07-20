package api.equinix.javasdk.mcp.bridge;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class McpCloudRouterBridge {

    private final Mcp client;

    /**
     * Searches for Cloud Routers using advanced filtering.
     *
     * @param filters the search filters
     * @return list of matching Cloud Routers
     */
    public List<McpCloudRouter> searchRouters(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_routers", filters);
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
     * <p>Invokes the documented {@code list_router_packages} MCP tool, narrowing the
     * listing to the requested package via the {@code routerPackageCode} argument.</p>
     *
     * @param packageCode the package code (e.g., "STANDARD", "PREMIUM")
     * @return the package details as JSON
     */
    public JsonNode getRouterPackage(String packageCode) {
        McpToolResult result = client.callTool("list_router_packages",
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
        return McpCloudRouter.builder()
                .uuid(getTextOrNull(json, "uuid"))
                .name(getTextOrNull(json, "name"))
                .state(getTextOrNull(json, "state"))
                .packageType(getTextOrNull(json, "package"))
                .metroCode(json.has("location") && json.get("location").has("metroCode")
                        ? json.get("location").get("metroCode").asText() : null)
                .connectionsCount(json.has("connectionsCount") ? json.get("connectionsCount").asInt() : 0)
                .rawJson(json)
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /**
     * Typed representation of a Fabric Cloud Router from the MCP server.
     */
    @Getter
    public static class McpCloudRouter {
        private final String uuid;
        private final String name;
        private final String state;
        private final String packageType;
        private final String metroCode;
        private final int connectionsCount;
        private final JsonNode rawJson;

        /**
         * Constructs a router snapshot. Argument order is pinned here — five
         * consecutive {@code String} parameters make positional calls
         * swap-prone, so build instances via the package-private builder.
         *
         * @param uuid             the router UUID
         * @param name             the router name
         * @param state            the lifecycle state
         * @param packageType      the package code (e.g. {@code "STANDARD"})
         * @param metroCode        the metro code, or {@code null}
         * @param connectionsCount the number of attached connections
         * @param rawJson          the raw MCP JSON payload
         */
        @Builder(access = AccessLevel.PACKAGE)
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

        @Override
        public String toString() {
            return "McpCloudRouter{uuid='" + uuid + "', name='" + name + "', metro='" + metroCode + "'}";
        }
    }
}
