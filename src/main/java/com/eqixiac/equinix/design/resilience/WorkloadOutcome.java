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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What happens to one workload in one scenario.
 *
 * @param workload          the workload label
 * @param status            how it fares
 * @param activeMetro       the metro it runs in during the scenario, or {@code null} when no host survives
 * @param failedOver        {@code true} when it runs in its standby metro
 * @param siteShare         share (0..1) of the site weight with a known path that still reaches it within its limits
 * @param capacityShare     share (0..1) of its bandwidth its cloud connections still carry (1 when it needs no cloud
 *                          or a capacity is unknown; 0 for a cloud it cannot reach)
 * @param serviceIndex      {@code siteShare x capacityShare}: the share of the workload still served
 * @param sitePaths         per site: the path taken and its round trip, or {@code unreachable}
 * @param cloudPaths        per cloud: the connection used, the path and its round trip, or {@code unreachable}
 * @param worstSiteLatencyMs the worst known round trip from a site that reaches it, or {@code null} when none is known
 * @param issues            what it lost, in words
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record WorkloadOutcome(String workload, WorkloadStatus status, String activeMetro, boolean failedOver,
                              double siteShare, double capacityShare, double serviceIndex,
                              Map<String, String> sitePaths, Map<String, String> cloudPaths,
                              Double worstSiteLatencyMs, List<String> issues) {

    /** Makes the collections immutable and insertion-ordered. */
    public WorkloadOutcome {
        sitePaths = sitePaths == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(sitePaths));
        cloudPaths = cloudPaths == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(cloudPaths));
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
