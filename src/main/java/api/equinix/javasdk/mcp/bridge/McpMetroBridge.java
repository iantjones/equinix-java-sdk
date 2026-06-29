package api.equinix.javasdk.mcp.bridge;

import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed bridge for MCP metro-related tools.
 *
 * <p>Wraps the raw {@code get_metro} and {@code list_metro} MCP tools with typed
 * Java return objects for integration with the Metro Optimizer.</p>
 *
 * @author ianjones
 */
public class McpMetroBridge {

    private final Mcp client;

    McpMetroBridge(Mcp client) {
        this.client = client;
    }

    /**
     * Retrieves details for a specific metro by its code.
     *
     * @param metroCode the metro code (e.g., "SV", "NY", "DC")
     * @return the metro details
     */
    public McpMetro getMetro(String metroCode) {
        McpToolResult result = client.callTool("get_metro", Map.of("metroCode", metroCode));
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        return parseMetro(json);
    }

    /**
     * Lists all metros available in Equinix Fabric.
     *
     * @return list of all available metros
     */
    public List<McpMetro> listMetros() {
        McpToolResult result = client.callTool("list_metro", Map.of());
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        List<McpMetro> metros = new ArrayList<>();

        if (json != null && json.has("data") && json.get("data").isArray()) {
            for (JsonNode node : json.get("data")) {
                metros.add(parseMetro(node));
            }
        } else if (json != null && json.isArray()) {
            for (JsonNode node : json) {
                metros.add(parseMetro(node));
            }
        }
        return metros;
    }

    private McpMetro parseMetro(JsonNode json) {
        if (json == null) return new McpMetro();
        return new McpMetro(
                getTextOrNull(json, "code"),
                getTextOrNull(json, "name"),
                getTextOrNull(json, "region"),
                getTextOrNull(json, "country"),
                json.has("connectedMetros") ? json.get("connectedMetros").size() : 0,
                json
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /**
     * Typed representation of an Equinix metro from the MCP server.
     */
    public static class McpMetro {
        private final String code;
        private final String name;
        private final String region;
        private final String country;
        private final int connectedMetroCount;
        private final JsonNode rawJson;

        McpMetro() {
            this(null, null, null, null, 0, null);
        }

        McpMetro(String code, String name, String region, String country,
                 int connectedMetroCount, JsonNode rawJson) {
            this.code = code;
            this.name = name;
            this.region = region;
            this.country = country;
            this.connectedMetroCount = connectedMetroCount;
            this.rawJson = rawJson;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getRegion() { return region; }
        public String getCountry() { return country; }
        public int getConnectedMetroCount() { return connectedMetroCount; }
        public JsonNode getRawJson() { return rawJson; }

        @Override
        public String toString() {
            return "McpMetro{code='" + code + "', name='" + name + "', region='" + region + "'}";
        }
    }
}
