/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.mcp.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static api.equinix.javasdk.mcp.server.Schemas.integer;
import static api.equinix.javasdk.mcp.server.Schemas.object;
import static api.equinix.javasdk.mcp.server.Schemas.props;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("McpToolAdapter — registration → SDK spec, result shape, error surface")
class McpToolAdapterTest {

    private final ServerContext context = ServerContext.builder().environment(Map.of()).build();

    private static ToolRegistration registration(ToolHandler handler) {
        return ToolRegistration.builder()
                .name("design_test_tool")
                .title("Test tool")
                .description("A tool for tests.")
                .inputSchema(object(props("n", integer("A number."))))
                .outputSchema(Schemas.looseObject("Test payload."))
                .toolset(Toolset.DESIGN)
                .handler(handler)
                .build();
    }

    private static McpSchema.CallToolResult call(McpServerFeatures.SyncToolSpecification spec,
                                                 Map<String, Object> arguments) {
        return spec.callHandler().apply(null,
                McpSchema.CallToolRequest.builder("design_test_tool").arguments(arguments).build());
    }

    @Test
    @DisplayName("a successful handler returns structuredContent plus the same JSON as text")
    void successShape() throws Exception {
        McpServerFeatures.SyncToolSpecification spec = McpToolAdapter.toSpecification(
                registration((args, ctx) -> {
                    ObjectNode payload = ctx.objectMapper().createObjectNode();
                    payload.put("doubled", args.get("n").asInt() * 2);
                    return payload;
                }), context);

        McpSchema.CallToolResult result = call(spec, Map.of("n", 21));

        assertFalse(Boolean.TRUE.equals(result.isError()), "success must not be an error result");
        assertInstanceOf(Map.class, result.structuredContent(), "structuredContent is a JSON object");
        assertEquals(42, ((Map<?, ?>) result.structuredContent()).get("doubled"));
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().get(0);
        assertTrue(text.text().contains("\"doubled\":42"), "text content mirrors the payload: " + text.text());
    }

    @Test
    @DisplayName("a throwing handler surfaces as an MCP tool error result, not an exception")
    void errorSurface() {
        McpServerFeatures.SyncToolSpecification spec = McpToolAdapter.toSpecification(
                registration((args, ctx) -> {
                    throw new IllegalArgumentException("'n' is required and must be a number.");
                }), context);

        McpSchema.CallToolResult result = call(spec, Map.of());

        assertTrue(Boolean.TRUE.equals(result.isError()), "handler exceptions become isError results");
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().get(0);
        assertTrue(text.text().contains("design_test_tool failed:"), text.text());
        assertTrue(text.text().contains("'n' is required"), "the handler's message is preserved: " + text.text());
    }

    @Test
    @DisplayName("null arguments arrive at the handler as an empty object node")
    void nullArguments() {
        McpServerFeatures.SyncToolSpecification spec = McpToolAdapter.toSpecification(
                registration((args, ctx) -> {
                    assertNotNull(args);
                    assertTrue(args.isObject());
                    ObjectNode payload = ctx.objectMapper().createObjectNode();
                    payload.put("ok", true);
                    return payload;
                }), context);

        McpSchema.CallToolResult result = call(spec, null);
        assertFalse(Boolean.TRUE.equals(result.isError()));
    }

    @Test
    @DisplayName("annotations are honest: readOnly, non-destructive, openWorld as registered")
    void annotationsCarryOver() {
        McpServerFeatures.SyncToolSpecification spec = McpToolAdapter.toSpecification(
                registration((args, ctx) -> ctx.objectMapper().createObjectNode()), context);

        McpSchema.Tool tool = spec.tool();
        assertEquals("design_test_tool", tool.name());
        assertNotNull(tool.inputSchema());
        assertNotNull(tool.outputSchema());
        assertEquals(Boolean.TRUE, tool.annotations().readOnlyHint());
        assertEquals(Boolean.FALSE, tool.annotations().destructiveHint());
        assertEquals(Boolean.FALSE, tool.annotations().openWorldHint());
    }
}
