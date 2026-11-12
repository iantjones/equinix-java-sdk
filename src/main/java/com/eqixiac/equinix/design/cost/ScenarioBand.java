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

package com.eqixiac.equinix.design.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cost over the horizon under a range of growth, from {@link GrowthPlanner#scenarios}: deterministic
 * low/base/high cases and, when requested, a seeded Monte Carlo with its distributions stated.
 *
 * @param specFingerprint the spec's fingerprint
 * @param currency        the currency of every amount
 * @param horizonMonths   the horizon
 * @param cases           the low, base and high cases
 * @param simulation      the Monte Carlo summary, or {@code null} when none was requested
 * @param unpriced        items that could not be priced, with the reason
 * @param assumptions     the modelling assumptions
 */
public record ScenarioBand(String specFingerprint, String currency, int horizonMonths, List<Case> cases,
                           Simulation simulation, List<String> unpriced, List<String> assumptions) {

    /** Copies the lists. */
    public ScenarioBand {
        cases = List.copyOf(cases);
        unpriced = List.copyOf(unpriced);
        assumptions = List.copyOf(assumptions);
    }

    /**
     * One deterministic case.
     *
     * @param name             low, base or high
     * @param annualGrowth     the growth rate (fraction)
     * @param egressFactor     the egress-volume factor (1 = the spec's volume)
     * @param totalOverHorizon the on-demand total, or {@code null} when a month is unpriced
     * @param finalMonthly     the last month's total, or {@code null}
     * @param unpriced         what could not be priced in this case
     */
    public record Case(String name, double annualGrowth, double egressFactor, BigDecimal totalOverHorizon,
                       BigDecimal finalMonthly, List<String> unpriced) {

        /** Copies the list. */
        public Case {
            unpriced = List.copyOf(unpriced);
        }
    }

    /**
     * A seeded Monte Carlo summary: re-running with the same seed and inputs gives the same figures.
     *
     * @param seed               the seed
     * @param iterations         runs
     * @param growthDistribution the stated growth distribution
     * @param egressDistribution the stated egress-volume distribution
     * @param method             the generator and percentile method
     * @param p10                10th percentile of the total over the horizon
     * @param p50                median
     * @param p90                90th percentile
     * @param unpricedRuns       runs whose total could not be priced (excluded from the percentiles)
     */
    public record Simulation(long seed, int iterations, String growthDistribution, String egressDistribution,
                             String method, BigDecimal p10, BigDecimal p50, BigDecimal p90, int unpricedRuns) {
    }

    /** @return a Markdown report of the cases and the simulation */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("## Cost scenarios over ").append(horizonMonths).append(" months\n\n");
        sb.append("| Case | Growth/year | Egress factor | Total | Final month |\n|---|---:|---:|---:|---:|\n");
        for (Case c : cases) {
            sb.append(String.format(java.util.Locale.ROOT, "| %s | %.1f%% | %.2f | %s | %s |%n", c.name(),
                    c.annualGrowth() * 100, c.egressFactor(), money(c.totalOverHorizon()), money(c.finalMonthly())));
        }
        if (simulation != null) {
            Simulation s = simulation;
            sb.append("\n**Monte Carlo** (seed ").append(s.seed()).append(", ").append(s.iterations())
                    .append(" runs): p10 ").append(money(s.p10())).append(", p50 ").append(money(s.p50()))
                    .append(", p90 ").append(money(s.p90())).append("\n\n");
            sb.append("- ").append(s.growthDistribution()).append("\n- ").append(s.egressDistribution())
                    .append("\n- ").append(s.method()).append("\n");
            if (s.unpricedRuns() > 0) {
                sb.append("- ").append(s.unpricedRuns()).append(" run(s) could not be priced and are excluded\n");
            }
        }
        for (String u : unpriced) {
            sb.append("- Unpriced: ").append(u).append("\n");
        }
        for (String a : assumptions) {
            sb.append("- Assumption: ").append(a).append("\n");
        }
        return sb.toString();
    }

    private String money(BigDecimal v) {
        return v == null ? "unpriced" : currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
