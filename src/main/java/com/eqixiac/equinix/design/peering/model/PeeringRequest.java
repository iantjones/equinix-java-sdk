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

package com.eqixiac.equinix.design.peering.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures all inputs for a peering intelligence analysis.
 *
 * <p>Built by the {@link com.eqixiac.equinix.design.peering.PeeringIntelligence.Builder}
 * fluent API. Contains the target ASNs (with optional labels), customer metro locations,
 * the customer's own ASN (for mutual peering discovery), and analysis flags.</p>
 *
 * @author ianjones
 * @see com.eqixiac.equinix.design.peering.PeeringIntelligence
 */
@Value
@Builder
public class PeeringRequest {

    @Singular
    Map<Long, String> targetAsns;

    @Singular("customerMetro")
    Set<MetroId> customerMetros;

    long customerAsn;

    boolean includeCapacity;

    boolean includePolicies;

    boolean includeResiliency;
}
