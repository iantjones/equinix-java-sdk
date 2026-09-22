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

package com.eqixiac.equinix.mcp.server.broker;

import java.time.Instant;

/**
 * One dry-run-validated proposal awaiting confirmation: the single-use confirm token, the
 * change type, the <em>exact</em> canonicalized spec the dry run validated (the confirm phase
 * executes this stored spec — never a re-supplied one), the SHA-256 the token is bound to,
 * the mint and expiry instants, and the price context the proposal reported.
 *
 * <p>The token is a lookup key into one server process's {@link ProposalStore}. It is not an
 * activation key of any cloud provider or of the Connection Coordinator specification, carries no
 * data, and is meaningless outside the process that minted it.</p>
 *
 * @param token the single-use confirm token handed back by {@code fabric_propose_change}
 * @param changeType the kind of create this proposal performs
 * @param canonicalSpec the canonical-form spec JSON (see {@code SpecHash})
 * @param specSha256 the SHA-256 (hex) of {@code canonicalSpec}, re-verified at confirm time
 * @param mintedAt when the dry run passed and the token was minted; the confirm prompt reports the
 *        proposal's age from it
 * @param expiresAt when the proposal stops being confirmable
 * @param priceSummary the one-line price context reported by the propose phase, repeated verbatim in
 *        the confirm prompt so the approver sees the figure the proposal showed; {@code null} when
 *        the proposal was minted without one
 */
public record PendingChange(String token, ChangeType changeType, String canonicalSpec,
                            String specSha256, Instant mintedAt, Instant expiresAt, String priceSummary) {
}
