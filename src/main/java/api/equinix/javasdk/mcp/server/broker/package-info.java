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
 * The Safe Mutation Broker — the embedded MCP server's only route to a mutation, enforced by
 * the server rather than by prompt discipline: <em>agents propose, dry-run diffs decide,
 * humans confirm</em>.
 *
 * <p>The broker is a two-phase protocol over two tools in the opt-in {@code mutate} toolset
 * (never served unless the operator names it in the toolset selection):</p>
 *
 * <ol>
 *   <li>{@code fabric_propose_change} — takes a typed create specification
 *       ({@link api.equinix.javasdk.mcp.server.broker.ChangeType}: connection, network, or
 *       service-token create; there are no update or delete change types) and executes the
 *       <strong>real, spec-documented dry run</strong> through the SDK's fluent creators
 *       ({@code dryRun=true} on the actual Fabric v4 endpoint). The Equinix API validates the
 *       exact payload that a confirm would send, but provisions nothing. The tool returns the
 *       validated entity, a rate-card price context where estimable (honest {@code unpriced}
 *       otherwise), and a single-use confirm token bound to the SHA-256 of the canonicalized
 *       spec.</li>
 *   <li>{@code fabric_confirm_change} — takes only the confirm token, re-verifies the spec
 *       hash, and executes the <em>stored</em> spec via the same creator without
 *       {@code dryRun}. The agent cannot alter the spec between phases: a different spec is a
 *       different proposal with a different token.</li>
 * </ol>
 *
 * <p>Tokens live in the in-memory, single-process
 * {@link api.equinix.javasdk.mcp.server.broker.ProposalStore} for 10 minutes and are consumed
 * on the first confirm attempt, successful or not. Expired, unknown, and replayed tokens each
 * fail with a message telling the agent to re-propose.</p>
 *
 * <p>Policy, matching the read-only catalog's rules: no delete tools of any kind, and no
 * mutation outside this broker.</p>
 */
package api.equinix.javasdk.mcp.server.broker;
