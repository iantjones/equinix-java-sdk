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
 *   <li>{@code arguments} is never {@code null} - a call with no arguments arrives as an
 *       empty object node;</li>
 *   <li>the returned {@link ObjectNode} becomes the tool result's {@code structuredContent}
 *       (and, serialized, its text content);</li>
 *   <li>throwing any exception is safe: the server converts it into an MCP tool
 *       <em>error result</em> ({@code isError: true} with the exception message) rather than
 *       a protocol failure, so validation can simply
 *       {@code throw new IllegalArgumentException("field 'x' is required")};</li>
 *   <li>text that comes from a remote system (an Equinix API field, a PeeringDB name, a pricing
 *       error) is put into the payload through {@link UntrustedText}, so the server can sanitise
 *       it and label it as data.</li>
 * </ul>
 *
 * <h2>Cancellation</h2>
 * <p>The server always invokes
 * {@link #handle(JsonNode, ServerContext, CancellationSignal)}. Its default checks the signal once
 * and then runs {@link #handle(JsonNode, ServerContext)}, which suits a read-only tool: a call the
 * client already cancelled does not start. A handler that must observe cancellation while it runs
 * (for example before sending an irreversible request) implements {@link Cancellable}.</p>
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * Executes the tool.
     *
     * @param arguments the parsed tool-call arguments (an object node, never {@code null})
     * @param context the shared server context of lazily-built SDK facades
     * @return the structured result payload
     * @throws Exception on any failure - surfaced to the caller as a tool error result
     */
    ObjectNode handle(JsonNode arguments, ServerContext context) throws Exception;

    /**
     * Executes the tool under a cancellation signal. The default refuses to start a call that is
     * already cancelled and otherwise delegates to {@link #handle(JsonNode, ServerContext)}.
     *
     * @param arguments the parsed tool-call arguments (an object node, never {@code null})
     * @param context the shared server context
     * @param cancellation the call's cancellation signal; never {@code null}
     * @return the structured result payload
     * @throws Exception on any failure - surfaced to the caller as a tool error result
     */
    default ObjectNode handle(JsonNode arguments, ServerContext context, CancellationSignal cancellation)
            throws Exception {
        cancellation.throwIfCancelled("before it started");
        return handle(arguments, context);
    }

    /**
     * A handler that observes its cancellation signal while it runs. Implement it with a
     * three-argument lambda; calling the two-argument form runs it with a never-cancelled signal.
     */
    @FunctionalInterface
    interface Cancellable extends ToolHandler {

        /**
         * Executes the tool, checking {@code cancellation} at each point where stopping is still
         * safe.
         *
         * @param arguments the parsed tool-call arguments
         * @param context the shared server context
         * @param cancellation the call's cancellation signal
         * @return the structured result payload
         * @throws Exception on any failure
         */
        @Override
        ObjectNode handle(JsonNode arguments, ServerContext context, CancellationSignal cancellation)
                throws Exception;

        /**
         * Runs the handler with a never-cancelled signal (direct invocations and tests).
         *
         * @param arguments the parsed tool-call arguments
         * @param context the shared server context
         * @return the structured result payload
         * @throws Exception on any failure
         */
        @Override
        default ObjectNode handle(JsonNode arguments, ServerContext context) throws Exception {
            return handle(arguments, context, CancellationSignal.none());
        }
    }
}
