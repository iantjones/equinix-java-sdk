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
 * @param maxLatencyMs        hard round-trip ceiling to this workload, in ms, from every site in {@code sites}
 *                            (from every site of the spec when {@code sites} is empty); may be {@code null}
 * @param clouds              ids of the {@link CloudIntent}s this workload must reach privately; never {@code null}
 * @param pinnedMetro         metro code the workload must be placed in, or {@code null}
 * @param sites               labels of the {@link SiteIntent}s that use this workload; empty means every site.
 *                            Placement and {@code maxLatencyMs} consider only these sites, so a regional
 *                            workload can carry a regional latency limit. Never {@code null}
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record WorkloadIntent(String label, WorkloadType type, Integer bandwidthMbps,
                             LatencySensitivity latencySensitivity, Double maxLatencyMs, List<String> clouds,
                             String pinnedMetro, List<String> sites) {

    /** Normalises {@code clouds} and {@code sites} to immutable, non-null lists. */
    public WorkloadIntent {
        clouds = clouds == null ? List.of() : List.copyOf(clouds);
        sites = sites == null ? List.of() : List.copyOf(sites);
    }

    /**
     * A workload used by every site.
     *
     * @param label              unique name within the spec
     * @param type               archetype, or {@code null}
     * @param bandwidthMbps      sustained bandwidth in Mbps, or {@code null}
     * @param latencySensitivity latency class, or {@code null}
     * @param maxLatencyMs       round-trip ceiling in ms, or {@code null}
     * @param clouds             cloud ids to reach
     * @param pinnedMetro        metro the workload must run in, or {@code null}
     */
    public WorkloadIntent(String label, WorkloadType type, Integer bandwidthMbps,
                          LatencySensitivity latencySensitivity, Double maxLatencyMs, List<String> clouds,
                          String pinnedMetro) {
        this(label, type, bandwidthMbps, latencySensitivity, maxLatencyMs, clouds, pinnedMetro, List.of());
    }
}
