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

/**
 * The embedded Equinix Intelligence MCP Server — a <em>community</em> Model Context Protocol
 * server, not affiliated with or endorsed by Equinix, and unrelated to Equinix's own
 * private-beta Fabric MCP server.
 *
 * <p>An MCP host (Claude Desktop, Cursor, VS Code, …) launches
 * {@link com.eqixiac.equinix.mcp.server.EquinixMcpServerMain} as a subprocess and speaks
 * newline-delimited JSON-RPC over stdio — the only transport this server offers. Every tool
 * executes through this SDK's typed domain clients and design engines under the operator's own
 * client-credentials ({@code EQUINIX_ACCESS_KEY} / {@code EQUINIX_SECRET_KEY}); there is no
 * browser consent flow and no separate authorization server.</p>
 *
 * <p>Design rules the tool catalog follows:</p>
 * <ul>
 *   <li>a deliberately small catalog — every tool embeds engine logic (optimizer, wizard,
 *       TCO/savings, peering, latency) or cross-domain reach (portal, Network Edge,
 *       IBX SmartView); none is a 1:1 mirror of a REST endpoint;</li>
 *   <li>domain-prefixed {@code snake_case} names ({@code design_*}, {@code portal_*},
 *       {@code ne_*}, {@code ibx_*});</li>
 *   <li>read-only: no delete tools, and mutations are out of scope for this catalog —
 *       they arrive only through the Safe Mutation Broker's dry-run-first two-phase tools,
 *       registered via the same {@link com.eqixiac.equinix.mcp.server.ToolRegistration}
 *       seam;</li>
 *   <li>results are structured JSON with size guards (lists are truncated to a sane cap and
 *       say so in the payload), and a tool call never hangs — external pricing lookups run
 *       under a hard timeout and degrade gracefully.</li>
 * </ul>
 *
 * <p>Extension seam: build a {@link com.eqixiac.equinix.mcp.server.ToolRegistration} (name,
 * JSON schema, annotations, {@link com.eqixiac.equinix.mcp.server.ToolHandler}) and pass it to
 * {@code EquinixMcpServer.builder().additionalTools(...)}; the handler receives the parsed
 * argument JSON and the shared {@link com.eqixiac.equinix.mcp.server.ServerContext} of
 * lazily-built SDK facades.</p>
 */
package com.eqixiac.equinix.mcp.server;
