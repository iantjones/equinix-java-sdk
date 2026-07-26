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
 * Result value types for Peering Intelligence, all Lombok {@code @Value} (get-prefixed
 * accessors) unless a hand-written accessor is documented.
 *
 * <p>The top-level {@link com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult}
 * aggregates the {@link com.eqixiac.equinix.design.peering.model.PresenceMatrix} (ASN x metro
 * grid of {@link com.eqixiac.equinix.design.peering.model.PresenceCell}s), per-network
 * {@link com.eqixiac.equinix.design.peering.model.NetworkPresence}, per-metro
 * {@link com.eqixiac.equinix.design.peering.model.MetroPresenceReport}, the optional
 * {@link com.eqixiac.equinix.design.peering.model.ResiliencyAssessment} (blast radius,
 * correlated failures, ranked failover paths, diversity scores),
 * {@link com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView}s, and
 * {@link com.eqixiac.equinix.design.peering.model.PeeringOpportunity} findings.
 * {@link com.eqixiac.equinix.design.peering.model.EquinixIXMapping} is the live-data bridge
 * from PeeringDB IX/facility ids to Fabric {@code MetroId}s.</p>
 *
 * @see com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult
 * @see com.eqixiac.equinix.design.peering.PeeringIntelligence
 */
package com.eqixiac.equinix.design.peering.model;
