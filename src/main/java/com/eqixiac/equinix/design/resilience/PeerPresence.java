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
package com.eqixiac.equinix.design.resilience;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;
import java.util.TreeSet;

/**
 * Where a peer network is present, for drawing it into a {@link DesignGraph}.
 *
 * @param asn     the autonomous system number
 * @param label   a readable name, or {@code null}
 * @param metros  metro codes where the network is present (IX or facility)
 * @param source  where the presence came from (for example {@code PeeringDB via PeeringIntelligence})
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PeerPresence(long asn, String label, Set<String> metros, String source) {

    /** Makes {@code metros} immutable, sorted and non-null. */
    public PeerPresence {
        metros = metros == null ? Set.of() : java.util.Collections.unmodifiableSet(new TreeSet<>(metros));
    }
}
