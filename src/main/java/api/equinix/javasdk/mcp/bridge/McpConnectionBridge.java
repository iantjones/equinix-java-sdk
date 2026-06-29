package api.equinix.javasdk.mcp.bridge;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed bridge for MCP connection-related tools.
 *
 * <p>Wraps connection validation, search, and creation MCP tools with typed
 * Java return objects for integration with the Deployment Wizard.</p>
 *
 * @author ianjones
 */
public class McpConnectionBridge {

    private final Mcp client;

    McpConnectionBridge(Mcp client) {
        this.client = client;
    }

    /**
     * Validates a connection configuration before creation.
     *
     * @param connectionSpec the connection specification to validate
     * @return the validation result
     */
    public McpConnectionValidation validateConnection(Map<String, Object> connectionSpec) {
        McpToolResult result = client.callTool("validate_connection", connectionSpec);
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        return parseValidation(json);
    }

    /**
     * Searches for connections using advanced filtering.
     *
     * @param filters the search filters (e.g., metro, state, type)
     * @return list of matching connections
     */
    public List<McpConnection> searchConnections(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_connection", filters);
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        List<McpConnection> connections = new ArrayList<>();

        if (json != null && json.has("data") && json.get("data").isArray()) {
            for (JsonNode node : json.get("data")) {
                connections.add(parseConnection(node));
            }
        }
        return connections;
    }

    /**
     * Retrieves pricing information for connections.
     *
     * @param filters the pricing query filters
     * @return the raw pricing data as JSON
     */
    public JsonNode searchPrices(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_prices", filters);
        return result.getJsonContent(client.getObjectMapper());
    }

    private McpConnectionValidation parseValidation(JsonNode json) {
        if (json == null) return new McpConnectionValidation(false, "No response from MCP server", json);

        boolean valid = json.has("valid") && json.get("valid").asBoolean();
        String message = json.has("message") ? json.get("message").asText() : null;
        return new McpConnectionValidation(valid, message, json);
    }

    private McpConnection parseConnection(JsonNode json) {
        return new McpConnection(
                getTextOrNull(json, "uuid"),
                getTextOrNull(json, "name"),
                getTextOrNull(json, "type"),
                getTextOrNull(json, "state"),
                json.has("bandwidth") ? json.get("bandwidth").asInt() : 0,
                json
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /**
     * Validation result from the MCP {@code validate_connection} tool.
     */
    public static class McpConnectionValidation {
        private final boolean valid;
        private final String message;
        private final JsonNode rawJson;

        McpConnectionValidation(boolean valid, String message, JsonNode rawJson) {
            this.valid = valid;
            this.message = message;
            this.rawJson = rawJson;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public JsonNode getRawJson() { return rawJson; }
    }

    /**
     * Typed representation of a Fabric connection from the MCP server.
     */
    public static class McpConnection {
        private final String uuid;
        private final String name;
        private final String type;
        private final String state;
        private final int bandwidth;
        private final JsonNode rawJson;

        McpConnection(String uuid, String name, String type, String state,
                      int bandwidth, JsonNode rawJson) {
            this.uuid = uuid;
            this.name = name;
            this.type = type;
            this.state = state;
            this.bandwidth = bandwidth;
            this.rawJson = rawJson;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getState() { return state; }
        public int getBandwidth() { return bandwidth; }
        public JsonNode getRawJson() { return rawJson; }

        @Override
        public String toString() {
            return "McpConnection{uuid='" + uuid + "', name='" + name + "', state='" + state + "'}";
        }
    }
}
