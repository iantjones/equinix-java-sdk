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

import com.eqixiac.equinix.design.resilience.ResilienceFindings.CorrelatedRisk;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.NVerdict;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.PairSummary;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.RankedScenario;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Remediation;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.SinglePointOfFailure;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The resilience analysis of one design: its graph, what every failure scenario leaves working, the
 * blast-radius ranking, single points of failure, correlated risks, N-1 and N-2 verdicts, estimated
 * availability and remediation. Produced by {@link ResilienceAnalyzer}; Jackson-serialisable
 * ({@link #toJson()}) with a readable {@link #toMarkdown()}.
 *
 * @param specFingerprint          the analysed spec's {@code fingerprint()}
 * @param specName                 the spec's name
 * @param portfolio                the analysed portfolio key
 * @param portfolioSource          where the portfolio came from (the optimiser's recommendation, or given)
 * @param dataAsOf                 when the design facts were gathered
 * @param graph                    the design graph
 * @param baseline                 the design with nothing failed
 * @param singleFailures           every single failure, element failures first, then shared-fate events
 * @param pairs                    what the N-2 simulation covered and its worst pairs
 * @param blastRadius              the single failures ranked worst first
 * @param singlePointsOfFailure    single elements whose loss causes an outage
 * @param correlatedRisks          redundant elements sharing a failure domain
 * @param n1                       the N-1 verdict (single-element failures; shared-fate events are reported, not counted)
 * @param n2                       the N-2 verdict over the simulated pairs
 * @param remediations             concrete changes, each tied to the scenarios it addresses
 * @param availability             estimated availability per workload
 * @param availabilityAssumptions  the per-element availabilities used and their source
 * @param assumptions              every assumption the analysis made
 * @param notes                    what the reader should know about the run
 * @param timingsMs                where the time went, in ms, by phase
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ResilienceReport(String specFingerprint, String specName, String portfolio, String portfolioSource,
                               Instant dataAsOf, DesignGraph graph, ScenarioOutcome baseline,
                               List<ScenarioOutcome> singleFailures, PairSummary pairs, List<RankedScenario> blastRadius,
                               List<SinglePointOfFailure> singlePointsOfFailure, List<CorrelatedRisk> correlatedRisks,
                               NVerdict n1, NVerdict n2, List<Remediation> remediations,
                               List<WorkloadAvailability> availability, AvailabilityAssumptions availabilityAssumptions,
                               List<String> assumptions, List<String> notes, Map<String, Long> timingsMs) {

    /** Makes the collections immutable and non-null. */
    public ResilienceReport {
        singleFailures = singleFailures == null ? List.of() : List.copyOf(singleFailures);
        blastRadius = blastRadius == null ? List.of() : List.copyOf(blastRadius);
        singlePointsOfFailure = singlePointsOfFailure == null ? List.of() : List.copyOf(singlePointsOfFailure);
        correlatedRisks = correlatedRisks == null ? List.of() : List.copyOf(correlatedRisks);
        remediations = remediations == null ? List.of() : List.copyOf(remediations);
        availability = availability == null ? List.of() : List.copyOf(availability);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        notes = notes == null ? List.of() : List.copyOf(notes);
        timingsMs = timingsMs == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(timingsMs));
    }

    /**
     * @param scenarioId a scenario id
     * @return the single-failure outcome with that id, or {@code null}
     */
    public ScenarioOutcome outcome(String scenarioId) {
        return singleFailures.stream().filter(o -> o.scenario().id().equals(scenarioId)).findFirst().orElse(null);
    }

    /** @return a few sentences: portfolio, N-1, N-2, worst scenario, single points of failure, availability */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Design '").append(specName).append("' on ").append(portfolio).append(" (").append(portfolioSource)
                .append("). ").append(n1.statement()).append(' ').append(n2.statement());
        if (!blastRadius.isEmpty()) {
            RankedScenario worst = blastRadius.get(0);
            sb.append(String.format(Locale.ROOT, " Worst single failure: %s (%s, %.0f%% of service lost).",
                    worst.description(), worst.verdict().name().toLowerCase(Locale.ROOT), 100 * worst.impact()));
        }
        sb.append(' ').append(singlePointsOfFailure.size()).append(" single point(s) of failure, ")
                .append(correlatedRisks.size()).append(" correlated risk(s), ").append(remediations.size())
                .append(" remediation(s).");
        if (!availability.isEmpty()) {
            WorkloadAvailability low = availability.stream()
                    .min(java.util.Comparator.comparingDouble(WorkloadAvailability::endToEndAvailability)).orElseThrow();
            sb.append(String.format(Locale.ROOT, " Lowest estimated availability: '%s' %.4f%% (about %.0f min/year, "
                    + "under planning assumptions, not SLAs).", low.workload(), low.percent(), low.downtimeMinutesPerYear()));
        }
        return sb.toString();
    }

    /**
     * @return a Markdown report: verdicts, scenario table, blast-radius ranking, single points of failure,
     *         correlated risks, N-2 summary, availability, remediation, the graph (Mermaid and table),
     *         assumptions, notes and timings
     */
    public String toMarkdown() {
        return ResilienceMarkdown.render(this);
    }

    /** @return the report as indented JSON (ISO-8601 instants) */
    @JsonIgnore
    public String toJson() {
        return ResilienceJson.write(this);
    }
}
