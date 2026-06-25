package api.equinix.javasdk.fabric.mcp;

import api.equinix.javasdk.core.exception.EquinixClientException;
import api.equinix.javasdk.fabric.mcp.model.McpJsonRpcResponse;
import lombok.Getter;

/**
 * Exception thrown when an MCP tool invocation or protocol operation fails.
 *
 * <p>Carries the JSON-RPC error code and message from the server response when available.</p>
 *
 * @author ianjones
 */
@Getter
public class McpException extends EquinixClientException {
    private static final long serialVersionUID = 1L;

    private final int errorCode;

    public McpException(String message) {
        super(message);
        this.errorCode = -1;
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }

    public McpException(int errorCode, String message) {
        super("MCP error " + errorCode + ": " + message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception from a JSON-RPC error response.
     *
     * @param error the JSON-RPC error object
     * @return a new McpException
     */
    public static McpException fromJsonRpcError(McpJsonRpcResponse.McpJsonRpcError error) {
        return new McpException(error.getCode(), error.getMessage());
    }
}
