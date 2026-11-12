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

import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;

/**
 * How much of a portfolio's baseline service survives its worst single-element failure, measured by
 * simulation: the shared core of {@link SimulatedResilienceObjective} and {@link MinSurvivalConstraint}.
 *
 * @param known          {@code false} when there is nothing to protect (no placed workload, no site with a known latency)
 * @param worstSurvival  the lowest {@link ScenarioOutcome#relativeSurvival()} over the N-1 scenarios, 0..1
 *                       (1 when there is no N-1 scenario); {@code NaN} when unknown
 * @param worst          the scenario that gives it (the baseline when nothing is served even then), or {@code null}
 * @param scenarios      how many N-1 scenarios were simulated
 * @param zoneViolations zone violations found at baseline or in any N-1 scenario
 * @param reason         why it is unknown, or a description of the worst scenario
 */
public record N1Survival(boolean known, double worstSurvival, ScenarioOutcome worst, int scenarios,
                         List<String> zoneViolations, String reason) {

    /** Makes the list immutable. */
    public N1Survival {
        zoneViolations = zoneViolations == null ? List.of() : List.copyOf(zoneViolations);
    }

    /**
     * Builds the portfolio's graph (placing workloads with the {@code WorkloadPlacer}) and simulates every
     * single-element failure.
     *
     * @param portfolio the metros
     * @param context   the facts of the run
     * @param options   options; {@code null} for the defaults
     * @return the measurement
     */
    public static N1Survival of(MetroPortfolio portfolio, DesignContext context, ResilienceOptions options) {
        DesignGraph graph = DesignGraphBuilder.build(context, portfolio,
                options == null ? ResilienceOptions.defaults() : options);
        FailureSimulator sim = new FailureSimulator(graph);
        ScenarioOutcome base = sim.baseline();
        List<String> violations = new ArrayList<>(base.zoneViolations());
        boolean anything = graph.placements().stream().anyMatch(p -> p.primary() != null)
                || !graph.edges(EdgeKind.SITE_ACCESS).isEmpty();
        if (!anything) {
            return new N1Survival(false, Double.NaN, null, 0, violations, "nothing to protect: no workload placed and "
                    + "no site with a known latency");
        }
        if (base.serviceIndex() <= 0) {
            return new N1Survival(true, 0.0, base, 0, violations, "nothing is served even with no failure (no site "
                    + "within its limits, or no cloud reached)");
        }
        double worst = 1.0;
        ScenarioOutcome worstOutcome = null;
        int count = 0;
        for (FailureScenario s : ScenarioCatalog.singles(graph)) {
            if (!s.kind().isSingleElement()) {
                continue;
            }
            ScenarioOutcome o = sim.simulate(s);
            count++;
            for (String v : o.zoneViolations()) {
                if (!violations.contains(v)) {
                    violations.add(v + " (after " + s.description() + ")");
                }
            }
            if (worstOutcome == null || o.relativeSurvival() < worst - 1e-12) {
                worstOutcome = o;
                worst = o.relativeSurvival();
            }
        }
        String reason = worstOutcome == null ? "no single-element failure to simulate"
                : String.format(java.util.Locale.ROOT, "%.0f%% of the baseline service survives the worst single "
                + "failure (%s, %s)", 100 * worst, worstOutcome.scenario().description(),
                worstOutcome.verdict().name().toLowerCase(java.util.Locale.ROOT));
        return new N1Survival(true, worst, worstOutcome, count, violations, reason);
    }
}
