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
 * Represents a mutual peering opportunity — a metro where both the customer's ASN
 * and a target ASN are present at an Equinix IX but not currently peering.
 *
 * <p>This is an immediately actionable finding: both networks are in the same building,
 * on the same IX LAN. Establishing peering requires only a BGP session configuration
 * (or, if the target uses route servers, may be automatic). The opportunity includes
 * a feasibility assessment based on the target's peering policy.</p>
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

    boolean targetUsesRouteServer;

    int targetSpeedMbps;

    double feasibility;

    String complexity;

    String recommendation;
}
