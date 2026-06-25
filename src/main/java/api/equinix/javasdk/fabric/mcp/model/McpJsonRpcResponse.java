package api.equinix.javasdk.fabric.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 response envelope for MCP protocol communication.
 *
 * <p>Contains either a {@code result} on success or an {@code error} on failure,
 * along with the correlation {@code id} matching the original request.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpJsonRpcResponse {

    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("id")
    private int id;

    @JsonProperty("result")
    private JsonNode result;

    @JsonProperty("error")
    private McpJsonRpcError error;

    /**
     * Returns {@code true} if the response contains an error.
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * JSON-RPC 2.0 error object containing a code, message, and optional data.
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpJsonRpcError {

        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private JsonNode data;

        @Override
        public String toString() {
            return "McpJsonRpcError{code=" + code + ", message='" + message + "'}";
        }
    }
}
