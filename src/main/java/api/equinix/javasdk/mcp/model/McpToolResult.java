package api.equinix.javasdk.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the result of an MCP tool invocation.
 *
 * <p>The result contains a list of content items (typically text or structured data)
 * and an {@code isError} flag indicating whether the tool execution failed.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpToolResult {

    @JsonProperty("content")
    private List<ContentItem> content;

    @JsonProperty("isError")
    private boolean isError;

    /**
     * Returns the first text content item, or {@code null} if none exists.
     */
    public String getTextContent() {
        if (content == null) return null;
        return content.stream()
                .filter(c -> "text".equals(c.getType()))
                .map(ContentItem::getText)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the raw JSON data from the first text content item, parsed from the text field.
     */
    public JsonNode getJsonContent(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        String text = getTextContent();
        if (text == null) return null;
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A single content item within a tool result.
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentItem {

        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("data")
        private String data;
    }
}
