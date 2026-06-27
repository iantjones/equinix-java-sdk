package api.equinix.javasdk.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC 2.0 request envelope for MCP protocol communication.
 *
 * <p>Encapsulates the standard JSON-RPC 2.0 fields: {@code jsonrpc} version,
 * auto-incrementing {@code id}, method name, and optional parameters.</p>
 *
 * @author ianjones
 */
@Getter
public class McpJsonRpcRequest {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    @JsonProperty("jsonrpc")
    private final String jsonrpc = "2.0";

    @JsonProperty("id")
    private final int id;

    @JsonProperty("method")
    private final String method;

    @JsonProperty("params")
    private final Map<String, Object> params;

    public McpJsonRpcRequest(String method, Map<String, Object> params) {
        this.id = ID_COUNTER.getAndIncrement();
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a {@code tools/call} request for invoking an MCP tool.
     *
     * @param toolName the name of the MCP tool to invoke
     * @param arguments the tool arguments
     * @return a new request configured for tool invocation
     */
    public static McpJsonRpcRequest toolCall(String toolName, Map<String, Object> arguments) {
        return new McpJsonRpcRequest("tools/call", Map.of(
                "name", toolName,
                "arguments", arguments
        ));
    }

    /**
     * Creates a {@code tools/list} request to enumerate available tools.
     *
     * @return a new request for listing tools
     */
    public static McpJsonRpcRequest toolsList() {
        return new McpJsonRpcRequest("tools/list", Map.of());
    }

    /**
     * Creates an {@code initialize} request for the MCP handshake.
     *
     * @param clientName the name of this client
     * @param clientVersion the version of this client
     * @return a new initialization request
     */
    public static McpJsonRpcRequest initialize(String clientName, String clientVersion) {
        return new McpJsonRpcRequest("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                        "name", clientName,
                        "version", clientVersion
                )
        ));
    }
}
