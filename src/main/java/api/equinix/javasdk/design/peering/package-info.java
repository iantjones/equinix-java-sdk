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
 * Peering Intelligence: interconnection analysis combining PeeringDB IX/facility data with the
 * live Equinix Fabric catalog.
 *
 * <p>{@link api.equinix.javasdk.design.peering.PeeringIntelligence} is the entry point — a
 * fluent builder reached via {@code fabric.peeringIntelligence()} or
 * {@code Design.over(fabric).peeringIntelligence()}. An analysis produces a
 * {@link api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult} containing an
 * ASN-by-metro presence matrix, per-network profiles, Fabric on-ramp availability (live
 * service-profile cross-referencing with corporate-to-product name bridging), optional
 * resiliency analysis (blast radius, correlated failures, ranked failover paths, geographic
 * diversity), and mutual-IX-presence peering opportunities.</p>
 *
 * <p>Data honesty is a design rule throughout: partial data is reported via
 * {@code warnings()} rather than silently presented as complete, unknown distances are
 * excluded rather than scored as zero, and disabled analyses are labelled "not analyzed"
 * rather than rendered as real zeroes.</p>
 *
 * <p>Sub-packages: {@code client} (the PeeringDB HTTP client and wire models), {@code model}
 * (analysis result types), {@code enums} (connectivity, policy, diversity, and failure-scope
 * classifications).</p>
 *
 * @see api.equinix.javasdk.design.peering.PeeringIntelligence
 * @see api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult
 */
package api.equinix.javasdk.design.peering;
