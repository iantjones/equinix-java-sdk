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
 *       ({@link com.eqixiac.equinix.mcp.server.broker.ChangeType}: connection, network, or
 *       service-token create; there are no update or delete change types) and executes the
 *       <strong>real, spec-documented dry run</strong> through the SDK's fluent creators
 *       ({@code dryRun=true} on the actual Fabric v4 endpoint). The Equinix API validates the
 *       exact payload that a confirm would send, but provisions nothing. The tool returns the
 *       validated entity, a rate-card price context where estimable (honest {@code unpriced}
 *       otherwise), and a single-use confirm token bound to the SHA-256 of the canonicalized
 *       spec.</li>
 *   <li>{@code fabric_confirm_change} — takes only the confirm token, re-verifies the spec
 *       hash, asks the MCP client to obtain the user's approval, and on approval executes the
 *       <em>stored</em> spec via the same creator without {@code dryRun}. The agent cannot alter
 *       the spec between phases: a different spec is a different proposal with a different
 *       token.</li>
 * </ol>
 *
 * <p>Tokens live in the in-memory, single-process
 * {@link com.eqixiac.equinix.mcp.server.broker.ProposalStore} for 10 minutes and are consumed
 * on the first confirm attempt, successful or not. Expired, unknown, and replayed tokens each
 * fail with a message telling the agent to re-propose. Every confirm result and every token error
 * carries the four redemption predicates as {@code confirm_checks}: {@code proposal_exists},
 * {@code not_expired}, {@code not_previously_used}, {@code spec_matches_binding}.</p>
 *
 * <h2>Human confirmation</h2>
 * <p>A tool argument cannot prove a person approved a mutation, because the calling model writes
 * every argument. The MCP client can: it is the party that renders an elicitation to the user.
 * {@code fabric_confirm_change} therefore sends a form elicitation (change type, target, price
 * context, proposal age, spec SHA-256) after consuming the token and before executing.</p>
 * <table>
 *   <caption>{@code fabric_confirm_change} by client capability and answer</caption>
 *   <tr><th>Client</th><th>Answer</th><th>{@code human_confirmation.status}</th><th>Executed</th></tr>
 *   <tr><td>declared form elicitation</td><td>accept with {@code confirm = true}</td>
 *       <td>{@code accepted}</td><td>yes</td></tr>
 *   <tr><td>declared form elicitation</td><td>decline, or accept without {@code confirm = true}</td>
 *       <td>{@code declined}</td><td>no</td></tr>
 *   <tr><td>declared form elicitation</td><td>cancel</td><td>{@code cancelled}</td><td>no</td></tr>
 *   <tr><td>declared form elicitation</td><td>none within {@code EQUINIX_MCP_ELICIT_TIMEOUT_MS}
 *       (default 300000 ms)</td><td>{@code timed_out}</td><td>no</td></tr>
 *   <tr><td>declared form elicitation</td><td>the round trip failed</td><td>{@code failed}</td><td>no</td></tr>
 *   <tr><td>did not declare it</td><td>not asked</td><td>{@code unsupported_by_client}</td>
 *       <td>yes; approval rests on the calling agent</td></tr>
 * </table>
 * <p>The token stays consumed in every row. Limits: an {@code accepted} status proves the client
 * reported an acceptance, not that a person read the prompt; and a client without elicitation
 * support gets no server-side check at all. <b>Beta</b>: the prompt is modelled on the "Confused
 * Deputy Resolution" requirement of the Connection Coordinator specification
 * ({@code connection-coordinator/docs/Protocols.md}, marked work in progress there) and is an
 * analogy to it, not an implementation of it.</p>
 *
 * <h2>Vocabulary</h2>
 * <p>The {@code chg-} confirm token is a lookup key into one process's proposal store. It is not an
 * activation key. Activation keys are issued by a cloud provider when a customer creates a native
 * multicloud link and are entered at the other provider; the broker neither mints nor accepts
 * them.</p>
 *
 * <p>Policy, matching the read-only catalog's rules: no delete tools of any kind, and no
 * mutation outside this broker.</p>
 */
package com.eqixiac.equinix.mcp.server.broker;
