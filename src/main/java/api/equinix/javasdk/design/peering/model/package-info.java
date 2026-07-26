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
 * <p>The top-level {@link api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult}
 * aggregates the {@link api.equinix.javasdk.design.peering.model.PresenceMatrix} (ASN x metro
 * grid of {@link api.equinix.javasdk.design.peering.model.PresenceCell}s), per-network
 * {@link api.equinix.javasdk.design.peering.model.NetworkPresence}, per-metro
 * {@link api.equinix.javasdk.design.peering.model.MetroPresenceReport}, the optional
 * {@link api.equinix.javasdk.design.peering.model.ResiliencyAssessment} (blast radius,
 * correlated failures, ranked failover paths, diversity scores),
 * {@link api.equinix.javasdk.design.peering.model.UnifiedConnectivityView}s, and
 * {@link api.equinix.javasdk.design.peering.model.PeeringOpportunity} findings.
 * {@link api.equinix.javasdk.design.peering.model.EquinixIXMapping} is the live-data bridge
 * from PeeringDB IX/facility ids to Fabric {@code MetroId}s.</p>
 *
 * @see api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult
 * @see api.equinix.javasdk.design.peering.PeeringIntelligence
 */
package api.equinix.javasdk.design.peering.model;
