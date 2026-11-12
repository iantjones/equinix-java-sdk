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

import java.util.List;

/**
 * A workload as the graph sees it: where it runs, where it fails over to and what it needs. Copied
 * from the {@code PlacementPlan} so a graph is self-contained.
 *
 * @param workload       the workload label (or {@code cloud:<id>} for a cloud no workload names)
 * @param primary        metro code it runs in, or {@code null} when it could not be placed
 * @param standby        metro code it fails over to, or {@code null} for none
 * @param clouds         spec cloud ids it must reach
 * @param bandwidthMbps  its demand in Mbps, or {@code null} when unsized (no capacity check is possible)
 * @param maxLatencyMs   its round-trip limit from every site, or {@code null}
 * @param cloudOnly      {@code true} for the placement made for a cloud no workload names
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PlacementView(String workload, String primary, String standby, List<String> clouds,
                            Integer bandwidthMbps, Double maxLatencyMs, boolean cloudOnly) {

    /** Makes {@code clouds} immutable and non-null. */
    public PlacementView {
        clouds = clouds == null ? List.of() : List.copyOf(clouds);
    }

    /** @return the graph node id of this workload */
    public String nodeId() {
        return "workload:" + workload;
    }
}
