package api.equinix.javasdk.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * Represents an MCP tool definition returned by the {@code tools/list} method.
 *
 * <p>Each tool has a unique name, a human-readable description, and a JSON Schema
 * defining its input parameters.</p>
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpToolDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private JsonNode inputSchema;

    @Override
    public String toString() {
        return "McpToolDefinition{name='" + name + "', description='" + description + "'}";
    }
}
