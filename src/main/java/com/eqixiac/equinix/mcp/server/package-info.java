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
 * The embedded Equinix Intelligence MCP Server - a <em>community</em> Model Context Protocol
 * server, not affiliated with or endorsed by Equinix, and separate from Equinix's official Fabric
 * MCP server. That server is in Public Preview at {@code https://mcp.equinix.com/fabric}: it covers
 * Fabric and Network Edge, signs users in with OAuth 2.1 (Equinix portal login), grants one of the
 * viewer, operator or manager permission levels at consent, and offers create and update tools but
 * no delete tools.
 *
 * <p>An MCP host (Claude Desktop, Cursor, VS Code, ...) launches
 * {@link com.eqixiac.equinix.mcp.server.EquinixMcpServerMain} as a subprocess and speaks
 * newline-delimited JSON-RPC over stdio - the only transport this server offers. Every tool
 * executes through this SDK's typed domain clients and design engines under the operator's own
 * client-credentials ({@code EQUINIX_ACCESS_KEY} / {@code EQUINIX_SECRET_KEY}); there is no
 * browser consent flow and no separate authorization server.</p>
 *
 * <p><b>The design loop.</b> One input, the {@code DesignSpec}, drives every design tool
 * ({@code design} toolset): {@code design_spec_template} and {@code design_validate_spec} (issues with
 * JSON paths and fixes, the normalised spec, its fingerprint), {@code design_optimize}
 * (recommendation, Pareto frontier, sensitivity, findings, cost with sources and currency conversions,
 * the spec with the used rates pinned), {@code design_analyze_resilience} (N-1/N-2, single points of
 * failure, blast radius, remediation, Mermaid graph), {@code design_growth_and_terms},
 * {@code design_estimate_tco}, {@code design_compare_cloud_egress}, {@code design_analyze_peering},
 * and the plan lifecycle {@code design_plan_deployment} / {@code design_get_plan} /
 * {@code design_plan_drift} / {@code design_export_terraform}, and {@code design_decision_record} (the
 * whole case for a design as one ADR-style document). Results carry the spec fingerprint and a
 * provenance block; optimization results and plans are kept in the process's
 * {@link com.eqixiac.equinix.mcp.server.DesignStore} so later calls can pass a
 * {@code spec_fingerprint} or {@code plan_id}. The {@code estate} toolset adds
 * {@code design_analyze_estate}, which reads the customer's own inventory and always requires explicit
 * project ids. With the design toolset the server also serves MCP resources (the DesignSpec JSON Schema,
 * a starter spec, stored plans) and prompts ({@link com.eqixiac.equinix.mcp.server.DesignResources}).</p>
 *
 * <p>Design rules the tool catalog follows:</p>
 * <ul>
 *   <li>every tool embeds engine logic (optimizer, resilience simulation, wizard, growth, TCO and
 *       savings, cloud-to-cloud path comparison, peering, latency, estate analysis) or cross-domain
 *       reach (operations, Network Edge, IBX SmartView); none is a 1:1 mirror of a REST endpoint;</li>
 *   <li>domain-prefixed {@code snake_case} names ({@code design_*}, {@code ops_*},
 *       {@code ne_*}, {@code ibx_*});</li>
 *   <li>read-only: no delete tools, and mutations are out of scope for this catalog - 
 *       they arrive only through the Safe Mutation Broker's dry-run-first two-phase tools,
 *       registered via the same {@link com.eqixiac.equinix.mcp.server.ToolRegistration}
 *       seam. The confirm phase asks the client for the user's approval through an MCP form
 *       elicitation ({@link com.eqixiac.equinix.mcp.server.ServerContext#confirmWithHuman(String)})
 *       and executes only on an accept; a client without elicitation support is reported as
 *       {@code unsupported_by_client};</li>
 *   <li><b>Beta</b>: {@code design_compare_cloud_to_cloud} and
 *       {@code design_list_multicloud_environments} cover native provider-to-provider multicloud
 *       links from dated reference data bundled with the SDK. The server calls no cloud-provider
 *       control-plane API and never creates such a link; a figure with no citable source is
 *       {@code null} with a reason, never zero;</li>
 *   <li>results are structured JSON sized for a model's context: summaries by default, more with
 *       {@code detail=standard|full} or by id, and every capped list says how many entries were
 *       omitted; a tool call never hangs: live pricing lookups share one per-call time budget and
 *       degrade gracefully to dated reference figures;</li>
 *   <li>text from remote systems (Equinix API fields and errors, PeeringDB names, pricing
 *       errors) is sanitised, bounded and listed under {@code untrusted_data}
 *       ({@link com.eqixiac.equinix.mcp.server.UntrustedText});</li>
 *   <li>a client {@code notifications/cancelled} stops a tool call at its next safe point
 *       ({@link com.eqixiac.equinix.mcp.server.CancellationSignal}); long read-only engine runs
 *       (optimization, resilience, estate) are abandoned at once.</li>
 * </ul>
 *
 * <p>Extension seam: build a {@link com.eqixiac.equinix.mcp.server.ToolRegistration} (name,
 * JSON schema, annotations, {@link com.eqixiac.equinix.mcp.server.ToolHandler}) and pass it to
 * {@code EquinixMcpServer.builder().additionalTools(...)}; the handler receives the parsed
 * argument JSON and the shared {@link com.eqixiac.equinix.mcp.server.ServerContext} of
 * lazily-built SDK facades.</p>
 */
package com.eqixiac.equinix.mcp.server;
