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

/** The finding types a {@link ResilienceReport} carries. */
public final class ResilienceFindings {

    private ResilienceFindings() {
    }

    /**
     * One scenario in the blast-radius ranking (worst first).
     *
     * @param rank               1 for the worst
     * @param scenarioId         the scenario
     * @param description        what fails
     * @param kind               the scenario kind
     * @param verdict            the outcome
     * @param impact             share of the baseline service lost, 0..1
     * @param siteWeightAffected share of site weight affected, 0..1
     * @param shortfallMbps      capacity shortfall, Mbps
     * @param workloadsDown      workloads that went down
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RankedScenario(int rank, String scenarioId, String description, ScenarioKind kind, Verdict verdict,
                                 double impact, double siteWeightAffected, double shortfallMbps,
                                 List<String> workloadsDown) {
        /** Makes the list immutable. */
        public RankedScenario {
            workloadsDown = workloadsDown == null ? List.of() : List.copyOf(workloadsDown);
        }
    }

    /**
     * A single element whose failure causes an outage.
     *
     * @param scenarioId  the single-failure scenario
     * @param kind        what fails
     * @param description what fails, in words
     * @param effect      what goes down, in words
     */
    public record SinglePointOfFailure(String scenarioId, ScenarioKind kind, String description, String effect) {
    }

    /** How serious a correlated risk is. */
    public enum Severity {
        /** The redundant elements share a region: one regional event takes both. */
        MEDIUM,
        /** The redundant elements share a metro: one metro event takes both. */
        HIGH
    }

    /**
     * Two (or more) elements that are meant to back each other up but share a failure domain.
     *
     * @param elements     the redundant elements (node or edge ids)
     * @param sharedDomain what they share (for example {@code metro DC}, {@code Equinix region AMER},
     *                     {@code cloud region AWS us-east-1})
     * @param severity     how serious
     * @param scenarioId   the scenario that takes them together, when one was simulated; may be {@code null}
     * @param description  the risk in words
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CorrelatedRisk(List<String> elements, String sharedDomain, Severity severity, String scenarioId,
                                 String description) {
        /** Makes the list immutable. */
        public CorrelatedRisk {
            elements = elements == null ? List.of() : List.copyOf(elements);
        }
    }

    /**
     * A concrete change and the scenarios it addresses.
     *
     * @param action     what to change
     * @param addresses  ids of the scenarios it would remove or soften
     * @param rationale  why, in words; says whether the effect was re-simulated
     * @param verified   {@code true} when the change was applied to the graph and re-simulated and the scenarios
     *                   no longer cause an outage or degradation
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Remediation(String action, List<String> addresses, String rationale, boolean verified) {
        /** Makes the list immutable. */
        public Remediation {
            addresses = addresses == null ? List.of() : List.copyOf(addresses);
        }
    }

    /** An N-1 or N-2 verdict level. */
    public enum Level {
        /** Every scenario is unaffected or survives at full service. */
        PASS,
        /** No outage, but some scenario degrades service. */
        DEGRADED,
        /** Some scenario causes an outage. */
        FAIL,
        /** There was nothing to simulate. */
        NOT_APPLICABLE
    }

    /**
     * The N-1 or N-2 verdict.
     *
     * @param level      the verdict
     * @param scenarios  how many scenarios it covers
     * @param survived   how many were unaffected or survived at full service
     * @param degraded   how many degraded
     * @param outages    how many caused an outage
     * @param statement  the verdict in words, including its coverage
     */
    public record NVerdict(Level level, int scenarios, int survived, int degraded, int outages, String statement) {
    }

    /**
     * What the pairwise (N-2) simulation covered.
     *
     * @param total      distinct pairs of single-element failures
     * @param evaluated  pairs simulated
     * @param exhaustive {@code true} when every pair was simulated
     * @param worst      the worst pairs, worst first (at most ten)
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record PairSummary(int total, int evaluated, boolean exhaustive, List<RankedScenario> worst) {
        /** Makes the list immutable. */
        public PairSummary {
            worst = worst == null ? List.of() : List.copyOf(worst);
        }
    }
}
