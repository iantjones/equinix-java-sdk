package api.equinix.javasdk.mcp.bridge;

import api.equinix.javasdk.mcp.McpClient;
import api.equinix.javasdk.mcp.model.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed bridge for MCP observability tools (metrics, streams, cloud events, alerts).
 *
 * <p>Provides access to real-time telemetry data for enriching optimization scoring
 * and resiliency assessments with live infrastructure state.</p>
 *
 * @author ianjones
 */
public class McpObservabilityBridge {

    private final McpClient client;

    McpObservabilityBridge(McpClient client) {
        this.client = client;
    }

    /**
     * Retrieves metrics for a specific asset (port, connection, etc.).
     *
     * @param assetType the asset type (e.g., "port", "connection")
     * @param assetId the asset UUID
     * @param metricType the metric type (e.g., "bandwidth", "latency")
     * @param startTime the start time in ISO-8601 format
     * @param endTime the end time in ISO-8601 format
     * @return the metrics data
     */
    public McpMetrics getMetrics(String assetType, String assetId, String metricType,
                                 String startTime, String endTime) {
        McpToolResult result = client.callTool("get_metrics", Map.of(
                "assetType", assetType,
                "assetId", assetId,
                "metricType", metricType,
                "startTime", startTime,
                "endTime", endTime
        ));
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        return parseMetrics(json);
    }

    /**
     * Searches for metrics across multiple assets.
     *
     * @param filters the search filters
     * @return list of metrics entries
     */
    public List<McpMetrics> searchMetrics(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_metrics", filters);
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        List<McpMetrics> metrics = new ArrayList<>();

        if (json != null && json.has("data") && json.get("data").isArray()) {
            for (JsonNode node : json.get("data")) {
                metrics.add(parseMetrics(node));
            }
        }
        return metrics;
    }

    /**
     * Lists available observability streams.
     *
     * @return list of stream summaries
     */
    public List<McpStream> listStreams() {
        McpToolResult result = client.callTool("list_streams", Map.of());
        JsonNode json = result.getJsonContent(client.getObjectMapper());
        List<McpStream> streams = new ArrayList<>();

        if (json != null && json.has("data") && json.get("data").isArray()) {
            for (JsonNode node : json.get("data")) {
                streams.add(parseStream(node));
            }
        }
        return streams;
    }

    /**
     * Searches for cloud events across Fabric resources.
     *
     * @param filters the search filters
     * @return the events data as JSON
     */
    public JsonNode searchCloudEvents(Map<String, Object> filters) {
        McpToolResult result = client.callTool("search_cloud_events", filters);
        return result.getJsonContent(client.getObjectMapper());
    }

    /**
     * Lists alert rules for a specific stream.
     *
     * @param streamId the stream UUID
     * @return list of alert rules as JSON
     */
    public JsonNode listAlertRules(String streamId) {
        McpToolResult result = client.callTool("list_stream_alert_rules",
                Map.of("streamId", streamId));
        return result.getJsonContent(client.getObjectMapper());
    }

    private McpMetrics parseMetrics(JsonNode json) {
        if (json == null) return new McpMetrics(null, null, 0, 0, 0, json);
        return new McpMetrics(
                getTextOrNull(json, "assetId"),
                getTextOrNull(json, "metricType"),
                json.has("min") ? json.get("min").asDouble() : 0,
                json.has("max") ? json.get("max").asDouble() : 0,
                json.has("avg") ? json.get("avg").asDouble() : 0,
                json
        );
    }

    private McpStream parseStream(JsonNode json) {
        return new McpStream(
                getTextOrNull(json, "uuid"),
                getTextOrNull(json, "name"),
                getTextOrNull(json, "type"),
                getTextOrNull(json, "state"),
                json
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    /**
     * Typed representation of metrics data from the MCP server.
     */
    public static class McpMetrics {
        private final String assetId;
        private final String metricType;
        private final double min;
        private final double max;
        private final double avg;
        private final JsonNode rawJson;

        McpMetrics(String assetId, String metricType, double min, double max,
                   double avg, JsonNode rawJson) {
            this.assetId = assetId;
            this.metricType = metricType;
            this.min = min;
            this.max = max;
            this.avg = avg;
            this.rawJson = rawJson;
        }

        public String getAssetId() { return assetId; }
        public String getMetricType() { return metricType; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getAvg() { return avg; }
        public JsonNode getRawJson() { return rawJson; }
    }

    /**
     * Typed representation of an observability stream from the MCP server.
     */
    public static class McpStream {
        private final String uuid;
        private final String name;
        private final String type;
        private final String state;
        private final JsonNode rawJson;

        McpStream(String uuid, String name, String type, String state, JsonNode rawJson) {
            this.uuid = uuid;
            this.name = name;
            this.type = type;
            this.state = state;
            this.rawJson = rawJson;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getState() { return state; }
        public JsonNode getRawJson() { return rawJson; }
    }
}
