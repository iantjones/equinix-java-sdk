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

package com.eqixiac.equinix.design.spec;

import com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Something that runs and needs connectivity.
 *
 * @param label               unique name within the spec
 * @param type                archetype that supplies default latency sensitivity and facility needs; may be {@code null}
 * @param bandwidthMbps       sustained bandwidth to provision for, in Mbps; may be {@code null} (unsized)
 * @param latencySensitivity  overrides the archetype's sensitivity; may be {@code null}
 * @param maxLatencyMs        hard round-trip ceiling from every site to this workload, in ms; may be {@code null}
 * @param clouds              ids of the {@link CloudIntent}s this workload must reach privately; never {@code null}
 * @param pinnedMetro         metro code the workload must be placed in, or {@code null}
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record WorkloadIntent(String label, WorkloadType type, Integer bandwidthMbps,
                             LatencySensitivity latencySensitivity, Double maxLatencyMs, List<String> clouds,
                             String pinnedMetro) {

    /** Normalises {@code clouds} to an immutable, non-null list. */
    public WorkloadIntent {
        clouds = clouds == null ? List.of() : List.copyOf(clouds);
    }
}
