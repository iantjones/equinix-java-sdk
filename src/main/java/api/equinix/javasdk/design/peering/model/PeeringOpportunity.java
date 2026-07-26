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

package api.equinix.javasdk.design.peering.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.enums.PeeringPolicy;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a mutual IX presence — an Equinix IX where both the customer's ASN and a target
 * ASN are connected, and peering could therefore be established (or may already exist).
 *
 * <p>PeeringDB records IX <em>membership</em>, not BGP sessions, so whether the two networks
 * already peer at this IX is not — and cannot be — determined from this data; the finding is
 * mutual presence, and opportunities for pairs that already peer should simply be ignored.
 * Where peering does not yet exist, the finding is immediately actionable: both networks are on
 * the same IX LAN, so establishing peering requires only a BGP session configuration (or, if the
 * target uses route servers, may be automatic). The opportunity includes a feasibility
 * assessment based on the target's peering policy.</p>
 *
 * <p>Exactly one opportunity is emitted per (target ASN, IX) pair: a target with several
 * parallel ports on the same IX LAN is still a single peering opportunity, with
 * {@code getTargetSpeedMbps()} aggregating the capacity across those sessions and
 * {@code getTargetSessionCount()} reporting how many there are.</p>
 *
 * @author ianjones
 * @see PeeringPolicy
 * @see PeeringIntelligenceResult
 */
@Value
@Builder
public class PeeringOpportunity {

    long customerAsn;

    long targetAsn;

    String targetLabel;

    MetroId metro;

    String ixName;

    int ixId;

    PeeringPolicy targetPolicy;

    /** Whether ANY of the target's sessions on this IX peers with the route servers. */
    boolean targetUsesRouteServer;

    /**
     * The target's total port capacity on this IX in Mbps, aggregated across all of its
     * sessions there (a {@code long}: parallel high-speed ports can exceed an int).
     */
    long targetSpeedMbps;

    /** How many parallel sessions (ports) the target has on this IX. */
    int targetSessionCount;

    double feasibility;

    String complexity;

    String recommendation;
}
