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
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.SolveProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitivityAnalysisTest {

    /** An objective read from a table keyed by portfolio key. */
    private static Objective table(String id, Direction direction, Map<String, Double> values) {
        return new Objective() {
            public String id() {
                return id;
            }

            public Direction direction() {
                return direction;
            }

            public String unit() {
                return "u";
            }

            public ObjectiveValue evaluate(MetroPortfolio p, DesignContext c) {
                Double v = values.get(p.key());
                return v == null ? ObjectiveValue.unknown("n/a") : ObjectiveValue.of(v, id + "=" + v);
            }
        };
    }

    private static FakeDesignContext ctx() {
        return new FakeDesignContext(DesignSpec.builder().name("s")
                .workload(WorkloadIntent.builder().label("w").build()).build())
                .metro("AA", Region.AMER).metro("BB", Region.AMER).metro("CC", Region.EMEA);
    }

    // Singles only. cost (min): AA 10, BB 20, CC 30. latency (min): AA 30, BB 10, CC 20.
    private static final Map<String, Double> COST = Map.of("AA", 10d, "BB", 20d, "CC", 30d);
    private static final Map<String, Double> LATENCY = Map.of("AA", 30d, "BB", 10d, "CC", 20d);

    private static SolveRun solve(double costWeight, double latencyWeight, List<Constraint> constraints) {
        FakeDesignContext ctx = ctx();
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("cost", costWeight);
        weights.put("latency", latencyWeight);
        return new ExhaustiveSolver().solveDetailed(new SolveProblem(ctx, ctx.metros(),
                List.of(table("cost", Direction.MINIMIZE, COST), table("latency", Direction.MINIMIZE, LATENCY)),
                constraints, weights, 1, 1));
    }

    @Test
    @DisplayName("weight ranges are exact: re-ranking the evaluated set reproduces a fresh solve at every weight")
    void weightRanges() {
        SolveRun run = solve(0.8, 0.2, List.of());
        assertEquals("AA", run.solution().recommended().portfolio().key());
        SensitivityReport report = SensitivityAnalysis.analyse(run);
        SensitivityReport.WeightRange cost = report.weightRanges().get(0);
        assertEquals("cost", cost.objective());
        // Scores: AA cost 100 lat 0; BB cost 50 lat 100; CC cost 0 lat 50.
        // AA beats BB while 100t >= 50t + 100(1-t), i.e. t >= 2/3.
        assertEquals(2.0 / 3, cost.low(), 1e-9);
        assertEquals(1.0, cost.high(), 1e-9);
        assertEquals("BB", cost.belowTakeover());
        assertNull(cost.aboveTakeover());
        // Cross-check against independent solves either side of the boundary.
        assertEquals("BB", solve(0.66, 0.34, List.of()).solution().recommended().portfolio().key());
        assertEquals("AA", solve(0.67, 0.33, List.of()).solution().recommended().portfolio().key());

        SensitivityReport.WeightRange latency = report.weightRanges().get(1);
        assertEquals(0.0, latency.low(), 1e-9);
        assertEquals(1.0 / 3, latency.high(), 1e-9);
        assertEquals("BB", latency.aboveTakeover());
    }

    @Test
    @DisplayName("the nearest frontier alternative and its raw deltas")
    void nearest() {
        SensitivityReport report = SensitivityAnalysis.analyse(solve(0.8, 0.2, List.of()));
        assertEquals("BB", report.nearest().portfolio());
        assertEquals(10.0, report.nearest().deltas().get("cost"), 1e-9);
        assertEquals(-20.0, report.nearest().deltas().get("latency"), 1e-9);
    }

    @Test
    @DisplayName("a constraint is binding when removing it changes the answer; pruning constraints are analysed too")
    void binding() {
        Constraint noAA = new Constraint() {
            public String id() {
                return "no_aa";
            }

            public boolean admits(MetroFacts m, DesignContext c) {
                return !m.id().code().equals("AA");
            }

            public ConstraintVerdict check(MetroPortfolio p, DesignContext c) {
                return p.key().contains("AA") ? ConstraintVerdict.fail("no_aa", "AA") : ConstraintVerdict.pass("no_aa", "ok");
            }
        };
        Constraint noCC = new Constraint() {
            public String id() {
                return "no_cc";
            }

            public ConstraintVerdict check(MetroPortfolio p, DesignContext c) {
                return p.key().contains("CC") ? ConstraintVerdict.fail("no_cc", "CC") : ConstraintVerdict.pass("no_cc", "ok");
            }
        };
        SolveRun run = solve(0.9, 0.1, List.of(noAA, noCC));
        assertEquals("BB", run.solution().recommended().portfolio().key());
        assertEquals(2, run.solution().evaluated(), "AA was pruned before search; BB and CC were examined");
        SensitivityReport report = SensitivityAnalysis.analyse(run);
        SensitivityReport.BindingConstraint aa = report.constraints().stream()
                .filter(b -> b.constraint().equals("no_aa")).findFirst().orElseThrow();
        assertTrue(aa.binding());
        assertEquals("AA", aa.alternative());
        SensitivityReport.BindingConstraint cc = report.constraints().stream()
                .filter(b -> b.constraint().equals("no_cc")).findFirst().orElseThrow();
        assertFalse(cc.binding(), cc.explanation());
        assertEquals(1, cc.rejectedAlone());
    }

    @Test
    @DisplayName("a dominated option never wins a tie: the recommendation is Pareto-optimal")
    void tieBreakPrefersParetoOptimal() {
        // Weighted on cost only. AA and BB tie on cost; BB is better on the zero-weight latency, so AA is
        // dominated. Key order alone would pick AA.
        Map<String, Double> cost = Map.of("AA", 10d, "BB", 10d);
        Map<String, Double> latency = Map.of("AA", 20d, "BB", 5d);
        FakeDesignContext ctx = new FakeDesignContext(DesignSpec.builder().name("t")
                .workload(WorkloadIntent.builder().label("w").build()).build())
                .metro("AA", Region.AMER).metro("BB", Region.AMER);
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("cost", 1.0);
        weights.put("latency", 0.0);
        SolveRun run = new ExhaustiveSolver().solveDetailed(new SolveProblem(ctx, ctx.metros(),
                List.of(table("cost", Direction.MINIMIZE, cost), table("latency", Direction.MINIMIZE, latency)),
                List.of(), weights, 1, 1));
        assertEquals("BB", run.solution().recommended().portfolio().key());
        assertTrue(run.solution().recommended().paretoOptimal());
    }
}
