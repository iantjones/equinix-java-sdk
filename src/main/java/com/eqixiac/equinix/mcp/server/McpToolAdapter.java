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

package com.eqixiac.equinix.mcp.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Converts a {@link ToolRegistration} into the MCP SDK's
 * {@link McpServerFeatures.SyncToolSpecification}: parses the raw argument map into JSON,
 * invokes the {@link ToolHandler}, and returns the payload as {@code structuredContent} plus a
 * serialized text block. Any exception becomes an MCP tool <em>error result</em>
 * ({@code isError: true}) — never a protocol failure — with the diagnostic logged to stderr.
 *
 * <p>The SDK hands each {@code callHandler} the client {@link McpSyncServerExchange}. Rather than
 * widen the {@link ToolHandler} signature (every handler and every test would change), the adapter
 * binds the exchange onto the {@link ServerContext} for the duration of the call via
 * {@link ServerContext#withExchange}, so a handler that needs to prompt the user can read it back
 * with {@link ServerContext#currentExchange()} and the rest stay untouched.</p>
 */
final class McpToolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(McpToolAdapter.class);

    private McpToolAdapter() {
    }

    static McpServerFeatures.SyncToolSpecification toSpecification(ToolRegistration registration,
                                                                   ServerContext context) {
        McpSchema.Tool.Builder tool = McpSchema.Tool
                .builder(registration.getName(), registration.getInputSchema())
                .description(registration.getDescription())
                .annotations(McpSchema.ToolAnnotations.builder()
                        .title(registration.getTitle())
                        .readOnlyHint(registration.isReadOnly())
                        .destructiveHint(registration.getDestructive() != null
                                ? registration.getDestructive()
                                : !registration.isReadOnly())
                        .idempotentHint(registration.isIdempotent())
                        .openWorldHint(registration.isOpenWorld())
                        .build());
        if (registration.getTitle() != null) {
            tool.title(registration.getTitle());
        }
        if (registration.getOutputSchema() != null) {
            tool.outputSchema(registration.getOutputSchema());
        }

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool.build())
                .callHandler((exchange, request) -> execute(registration, context, exchange, request))
                .build();
    }

    private static McpSchema.CallToolResult execute(ToolRegistration registration, ServerContext context,
                                                    McpSyncServerExchange exchange,
                                                    McpSchema.CallToolRequest request) {
        ObjectMapper mapper = context.objectMapper();
        try {
            Map<String, Object> rawArguments = request.arguments();
            JsonNode arguments = rawArguments == null
                    ? mapper.createObjectNode() : mapper.valueToTree(rawArguments);
            // Bind the client exchange for this call so a handler can elicit; cleared on return.
            ObjectNode payload = context.withExchange(exchange,
                    () -> registration.getHandler().handle(arguments, context));
            return McpSchema.CallToolResult.builder()
                    .structuredContent(mapper.convertValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }))
                    .addTextContent(mapper.writeValueAsString(payload))
                    .isError(false)
                    .build();
        }
        catch (Exception | LinkageError e) {
            logger.warn("Tool '{}' failed: {}", registration.getName(), e.toString());
            return errorResult(registration.getName(), e);
        }
    }

    private static McpSchema.CallToolResult errorResult(String toolName, Throwable e) {
        String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                ? e.getClass().getSimpleName() : e.getMessage();
        return McpSchema.CallToolResult.builder()
                .addTextContent(toolName + " failed: " + message)
                .isError(true)
                .build();
    }

    /** Serializes a payload compactly, for handlers that need embedded JSON strings. */
    static String compact(ObjectMapper mapper, ObjectNode payload) {
        try {
            return mapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload serialization failed", e);
        }
    }
}
