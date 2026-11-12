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

package com.eqixiac.equinix.design.solve;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.DesignSolution;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.SolveProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolveFoundationTest {

    private static final Direction[] MIN_MIN = {Direction.MINIMIZE, Direction.MINIMIZE};

    // WeightProfiles

    @Test
    @DisplayName("profiles resolve to weights that sum to 1; overrides and plug-in ids are honoured")
    void weightProfiles() {
        Map<String, Double> balanced = WeightProfiles.resolve(null);
        assertEquals(1.0, balanced.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);
        assertEquals(0.30, balanced.get(Objective.LATENCY), 1e-12);

        Map<String, Double> custom = WeightProfiles.resolve(new DesignObjectives("COST_FIRST", Map.of("carbon", 0.45)));
        assertEquals(custom.get(Objective.COST), custom.get("carbon"), 1e-12, "0.45 each before normalising");
        assertEquals(1.0, custom.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);

        Map<String, Double> only = WeightProfiles.resolve(new DesignObjectives(null, Map.of("latency", 3.0, "cost", 1.0)));
        assertEquals(Map.of("latency", 0.75, "cost", 0.25), only, "no profile + weights = only the listed objectives");

        assertThrows(IllegalArgumentException.class, () -> WeightProfiles.of("fastest"));
        assertThrows(IllegalArgumentException.class,
                () -> WeightProfiles.resolve(new DesignObjectives(null, Map.of("cost", 0.0))));
    }

    // Pareto

    @Test
    @DisplayName("dominance needs no-worse everywhere and better somewhere; unknown is worst")
    void paretoDominance() {
        assertTrue(Pareto.dominates(new double[]{1, 1}, new double[]{2, 1}, MIN_MIN));
        assertFalse(Pareto.dominates(new double[]{1, 1}, new double[]{1, 1}, MIN_MIN), "equal rows do not dominate");
        assertFalse(Pareto.dominates(new double[]{1, 3}, new double[]{2, 1}, MIN_MIN), "a trade-off");
        assertTrue(Pareto.dominates(new double[]{5, 5}, new double[]{5, Double.NaN}, MIN_MIN));
        assertFalse(Pareto.dominates(new double[]{5, Double.NaN}, new double[]{9, 5}, MIN_MIN));
        assertTrue(Pareto.dominates(new double[]{9}, new double[]{3}, new Direction[]{Direction.MAXIMIZE}));
    }

    @Test
    @DisplayName("the frontier keeps the trade-offs and duplicates, drops the dominated")
    void paretoFrontier() {
        List<double[]> rows = List.of(
                new double[]{1, 9}, new double[]{5, 5}, new double[]{9, 1},
                new double[]{6, 6}, new double[]{5, 5}, new double[]{9, 9});
        assertEquals(List.of(0, 1, 2, 4), Pareto.frontier(rows, MIN_MIN));
    }

    // ExhaustiveSolver

    private static DesignSpec spec() {
        return DesignSpec.builder().name("t")
                .site(SiteIntent.builder().label("East").metro("DC").weight(1.0).build())
                .site(SiteIntent.builder().label("West").metro("SV").weight(1.0).build())
                .workload(WorkloadIntent.builder().label("w").bandwidthMbps(100).build())
                .build();
    }

    private static FakeDesignContext context() {
        return new FakeDesignContext(spec())
                .metro("DC", Region.AMER).metro("SV", Region.AMER).metro("CH", Region.AMER).metro("LD", Region.EMEA)
                .latency("DC", "East", 1).latency("DC", "West", 66)
                .latency("SV", "East", 66).latency("SV", "West", 1)
                .latency("CH", "East", 17).latency("CH", "West", 47)
                .latency("LD", "East", 73).latency("LD", "West", 139);
    }

    /** Site-weighted latency to the nearest metro of the portfolio. */
    private static Objective latency() {
        return objective(Objective.LATENCY, Direction.MINIMIZE, "ms", (p, ctx) -> {
            double total = 0;
            for (SiteIntent site : ctx.spec().sites()) {
                double nearest = Double.MAX_VALUE;
                for (var metro : p.metros()) {
                    OptionalDouble ms = ctx.roundTripLatencyMs(metro, site);
                    if (ms.isPresent()) {
                        nearest = Math.min(nearest, ms.getAsDouble());
                    }
                }
                total += ctx.siteWeight(site) * nearest;
            }
            return total;
        });
    }

    /** 100 per metro: more metros cost more. */
    private static Objective cost() {
        return objective(Objective.COST, Direction.MINIMIZE, "USD/month", (p, ctx) -> 100.0 * p.size());
    }

    private interface Eval {
        double apply(MetroPortfolio portfolio, DesignContext context);
    }

    private static Objective objective(String id, Direction direction, String unit, Eval eval) {
        return new Objective() {
            public String id() { return id; }
            public Direction direction() { return direction; }
            public String unit() { return unit; }
            public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
                return ObjectiveValue.of(eval.apply(portfolio, context), id + " of " + portfolio.key());
            }
        };
    }

    private static Constraint excluding(String metro) {
        return new Constraint() {
            public String id() { return "exclude_" + metro; }
            public boolean admits(MetroFacts m, DesignContext c) { return !m.id().code().equals(metro); }
            public ConstraintVerdict check(MetroPortfolio p, DesignContext c) {
                return ConstraintVerdict.pass(id(), "not present");
            }
        };
    }

    private static SolveProblem problem(Map<String, Double> weights, int min, int max, Constraint... constraints) {
        FakeDesignContext ctx = context();
        return new SolveProblem(ctx, ctx.metros(), List.of(latency(), cost()), List.of(constraints), weights, min, max);
    }

    private static List<String> keys(List<DesignOption> options) {
        return options.stream().map(o -> o.portfolio().key()).collect(Collectors.toList());
    }

    @Test
    @DisplayName("examines every portfolio and returns the exact latency/cost frontier")
    void exactFrontier() {
        DesignSolution solution = new ExhaustiveSolver().solve(problem(Map.of("latency", 0.5, "cost", 0.5), 1, 2));

        assertEquals(4 + 6, solution.evaluated(), "C(4,1) + C(4,2)");
        assertEquals(10, solution.feasible());
        assertTrue(solution.exact());
        // Cheapest single metro with the best latency is CH (32 ms avg); the best pair is DC+SV (1 ms avg).
        // Every other option is beaten on both axes by one of those two.
        assertEquals(List.of("DC+SV", "CH"), keys(solution.frontier()).stream().sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList()));
        for (DesignOption option : solution.frontier()) {
            assertTrue(option.paretoOptimal());
        }
    }

    @Test
    @DisplayName("weights choose among the frontier; the recommendation moves with them")
    void weightsPickTheRecommendation() {
        DesignSolution latencyLed = new ExhaustiveSolver().solve(problem(Map.of("latency", 0.9, "cost", 0.1), 1, 2));
        DesignSolution costLed = new ExhaustiveSolver().solve(problem(Map.of("latency", 0.1, "cost", 0.9), 1, 2));
        assertEquals("DC+SV", latencyLed.recommended().portfolio().key());
        assertEquals("CH", costLed.recommended().portfolio().key());
        assertEquals(100.0, latencyLed.recommended().scores().get("latency"), 1e-9, "best latency seen scores 100");
        assertEquals(1.0, latencyLed.recommended().values().get("latency").value(), 1e-9);
    }

    @Test
    @DisplayName("a constraint's per-metro pruning removes the metro from every portfolio")
    void pruning() {
        DesignSolution solution = new ExhaustiveSolver().solve(
                problem(Map.of("latency", 1.0), 1, 2, excluding("DC")));
        assertEquals(3 + 3, solution.evaluated(), "C(3,1) + C(3,2) once DC is pruned");
        assertFalse(keys(solution.frontier()).stream().anyMatch(k -> k.contains("DC")));
        assertTrue(solution.notes().get(0).contains("ruled out"), solution.notes().toString());
    }

    @Test
    @DisplayName("no feasible portfolio is reported, with the constraint that rejected them")
    void infeasible() {
        Constraint never = new Constraint() {
            public String id() { return "never"; }
            public ConstraintVerdict check(MetroPortfolio p, DesignContext c) {
                return ConstraintVerdict.fail(id(), "nothing is allowed");
            }
        };
        DesignSolution solution = new ExhaustiveSolver().solve(problem(Map.of("latency", 1.0), 1, 2, never));
        assertFalse(solution.isFeasible());
        assertNull(solution.recommended());
        assertEquals(0, solution.feasible());
        assertTrue(String.join(" ", solution.notes()).contains("never=10"), solution.notes().toString());
    }

    @Test
    @DisplayName("above the portfolio ceiling the field is narrowed and the result says it is not exact")
    void narrowsAboveCeiling() {
        DesignSolution solution = new ExhaustiveSolver(5).solve(problem(Map.of("latency", 1.0), 1, 2));
        assertFalse(solution.exact());
        assertTrue(solution.evaluated() <= 5, "evaluated " + solution.evaluated());
        assertTrue(String.join(" ", solution.notes()).contains("narrowed"), solution.notes().toString());
        assertTrue(solution.isFeasible());
    }

    @Test
    @DisplayName("ties break deterministically: fewer metros, then key")
    void deterministicTies() {
        ToDoubleFunction<MetroPortfolio> flat = p -> 1.0;
        Objective constant = objective("flat", Direction.MINIMIZE, "x", (p, c) -> flat.applyAsDouble(p));
        FakeDesignContext ctx = context();
        SolveProblem same = new SolveProblem(ctx, ctx.metros(), List.of(constant), List.of(), Map.of("flat", 1.0), 1, 2);
        assertEquals("CH", new ExhaustiveSolver().solve(same).recommended().portfolio().key());
        assertEquals(ExhaustiveSolver.count(66, 1, 3), 66L + 2145L + 45760L);
    }
}
