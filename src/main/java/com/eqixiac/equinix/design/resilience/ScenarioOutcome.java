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
 * What still works in one scenario, compared with the baseline.
 *
 * @param scenario               the scenario
 * @param verdict                the outcome
 * @param serviceIndex           mean over placed workloads of their service index, 0..1
 * @param relativeSurvival       {@code serviceIndex} divided by the baseline's (1 = nothing lost); 0..1
 * @param siteWeightAffected     share of site weight that lost a workload it reached at baseline (or newly
 *                               breaks a latency limit), 0..1
 * @param siteWeightDisconnected share of site weight with no surviving access circuit, 0..1
 * @param workloadsAffected      workloads that lost anything
 * @param workloadsDown          workloads that went down
 * @param requiredCloudsAffected required clouds some workload reached at baseline and no longer reaches
 * @param demandMbps             sized cloud demand the baseline serves, Mbps
 * @param servedMbps             of it, what the surviving connections carry, Mbps
 * @param shortfallMbps          {@code demandMbps - servedMbps}
 * @param constraintBreaks       latency limits broken in this scenario (workload {@code maxLatencyMs}, spec {@code maxLatencyMs})
 * @param zoneViolations         active paths that leave the spec's {@code geoZones}
 * @param workloads              one outcome per placed workload
 * @param connectionLoadMbps     per cloud connection edge id: the sized bandwidth routed onto it in this scenario
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ScenarioOutcome(FailureScenario scenario, Verdict verdict, double serviceIndex, double relativeSurvival,
                              double siteWeightAffected, double siteWeightDisconnected, List<String> workloadsAffected,
                              List<String> workloadsDown, List<String> requiredCloudsAffected, double demandMbps,
                              double servedMbps, double shortfallMbps, List<String> constraintBreaks,
                              List<String> zoneViolations, List<WorkloadOutcome> workloads,
                              java.util.Map<String, Double> connectionLoadMbps) {

    /** Makes the lists immutable and non-null. */
    public ScenarioOutcome {
        workloadsAffected = workloadsAffected == null ? List.of() : List.copyOf(workloadsAffected);
        workloadsDown = workloadsDown == null ? List.of() : List.copyOf(workloadsDown);
        requiredCloudsAffected = requiredCloudsAffected == null ? List.of() : List.copyOf(requiredCloudsAffected);
        constraintBreaks = constraintBreaks == null ? List.of() : List.copyOf(constraintBreaks);
        zoneViolations = zoneViolations == null ? List.of() : List.copyOf(zoneViolations);
        workloads = workloads == null ? List.of() : List.copyOf(workloads);
        connectionLoadMbps = connectionLoadMbps == null ? java.util.Map.of()
                : java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(connectionLoadMbps));
    }

    /** @return the share of the baseline's sized cloud demand still carried, 0..1 (1 when there is no sized demand) */
    public double capacityShare() {
        return demandMbps <= 0 ? 1.0 : servedMbps / demandMbps;
    }

    /**
     * The blast radius: how much of the design is lost, 0 (nothing) to 1 (everything), used to rank
     * scenarios worst first.
     *
     * @return {@code 1 - relativeSurvival}
     */
    public double impact() {
        return 1.0 - relativeSurvival;
    }
}
