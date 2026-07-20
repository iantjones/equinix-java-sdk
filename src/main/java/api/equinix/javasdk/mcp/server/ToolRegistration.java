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

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * One complete MCP tool definition: name, description, JSON input/output schema, behavior
 * annotations, owning {@link Toolset}, and the {@link ToolHandler} that executes it.
 *
 * <p>This is the server's registration seam. The built-in catalog is expressed as
 * {@code ToolRegistration}s, and additional tools — notably the Safe Mutation Broker's
 * dry-run-first two-phase mutation tools — plug in through the very same type via
 * {@code EquinixMcpServer.builder().additionalTools(...)}. Nothing about the built-in tools
 * is special: the server converts every registration identically.</p>
 *
 * <p>Conventions:</p>
 * <ul>
 *   <li>{@code name} is domain-prefixed {@code snake_case} ({@code design_estimate_tco});</li>
 *   <li>{@code inputSchema} / {@code outputSchema} are plain JSON-Schema maps (2020-12
 *       dialect). Schema {@code description}s are written <em>for the calling LLM</em> —
 *       they are prompts, not documentation footnotes;</li>
 *   <li>annotations are honest: {@code readOnly} for tools with no side effects,
 *       {@code openWorld} where the handler reaches beyond Equinix's APIs (e.g. live
 *       cloud-provider pricing endpoints, PeeringDB).</li>
 * </ul>
 */
@Value
@Builder
public class ToolRegistration {

    /** The unique, domain-prefixed snake_case tool name (e.g. {@code design_optimize_placement}). */
    String name;

    /** Short human-readable title shown by MCP hosts. */
    String title;

    /** The tool description sent to the model — written as a prompt for an LLM. */
    String description;

    /** JSON Schema (2020-12) for the tool arguments. Required. */
    Map<String, Object> inputSchema;

    /**
     * Optional JSON Schema for the structured result. When present the handler's payload is
     * returned as {@code structuredContent} and validated against it, so keep it permissive
     * ({@code additionalProperties: true}) unless the payload shape is a hard contract.
     */
    Map<String, Object> outputSchema;

    /** {@code true} when the tool performs no mutation anywhere. All 12 built-in tools are read-only. */
    @Builder.Default
    boolean readOnly = true;

    /** {@code true} when the handler calls services outside Equinix (live cloud pricing, PeeringDB). */
    @Builder.Default
    boolean openWorld = false;

    /** {@code true} when repeating the call with the same arguments has no additional effect. */
    @Builder.Default
    boolean idempotent = true;

    /**
     * Optional override for the MCP {@code destructiveHint}. When {@code null} (the default)
     * the hint is derived as {@code !readOnly}, which is right for the read-only catalog. The
     * Safe Mutation Broker's tools set this to {@code false} explicitly: they are not
     * read-only (the pair exists to mutate), but every change type is a purely additive
     * create — nothing this server exposes can delete or overwrite existing resources.
     */
    Boolean destructive;

    /** The toolset this tool belongs to, used by the {@code --toolsets} filter. */
    Toolset toolset;

    /** The tool body. */
    ToolHandler handler;
}
