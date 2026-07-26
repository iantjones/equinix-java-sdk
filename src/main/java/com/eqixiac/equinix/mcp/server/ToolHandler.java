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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The body of an MCP tool: takes the parsed argument JSON and the shared
 * {@link ServerContext}, returns the structured result payload.
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>{@code arguments} is never {@code null} — a call with no arguments arrives as an
 *       empty object node;</li>
 *   <li>the returned {@link ObjectNode} becomes the tool result's {@code structuredContent}
 *       (and, serialized, its text content);</li>
 *   <li>throwing any exception is safe: the server converts it into an MCP tool
 *       <em>error result</em> ({@code isError: true} with the exception message) rather than
 *       a protocol failure, so validation can simply
 *       {@code throw new IllegalArgumentException("field 'x' is required")}.</li>
 * </ul>
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * Executes the tool.
     *
     * @param arguments the parsed tool-call arguments (an object node, never {@code null})
     * @param context the shared server context of lazily-built SDK facades
     * @return the structured result payload
     * @throws Exception on any failure — surfaced to the caller as a tool error result
     */
    ObjectNode handle(JsonNode arguments, ServerContext context) throws Exception;
}
