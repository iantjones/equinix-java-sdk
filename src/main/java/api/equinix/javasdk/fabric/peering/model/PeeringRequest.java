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

package api.equinix.javasdk.fabric.peering.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures all inputs for a peering intelligence analysis.
 *
 * <p>Built by the {@link api.equinix.javasdk.fabric.peering.PeeringIntelligence.Builder}
 * fluent API. Contains the target ASNs (with optional labels), customer metro locations,
 * the customer's own ASN (for mutual peering discovery), and analysis flags.</p>
 *
 * @author ianjones
 * @see api.equinix.javasdk.fabric.peering.PeeringIntelligence
 */
@Value
@Builder
public class PeeringRequest {

    /** Target ASNs to analyze, mapped to human-readable labels (e.g., 16509 → "AWS"). */
    @Singular
    Map<Long, String> targetAsns;

    /** Customer's metro locations for resiliency analysis (optional). */
    @Singular("customerMetro")
    Set<MetroCode> customerMetros;

    /** Customer's own ASN for mutual peering opportunity discovery (optional, 0 if not set). */
    long customerAsn;

    /** Whether to include IX port speed/capacity in the analysis. */
    boolean includeCapacity;

    /** Whether to include peering policy feasibility in the analysis. */
    boolean includePolicies;

    /** Whether to cross-reference Equinix Fabric service profile availability. */
    boolean includeFabricConnections;

    /** Whether to perform resiliency and blast radius analysis. */
    boolean includeResiliency;
}
