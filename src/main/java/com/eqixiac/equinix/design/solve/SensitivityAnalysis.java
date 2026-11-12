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

import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sensitivity of a recommendation, computed exactly from a {@link SolveRun} by re-ranking the portfolios
 * the solver already evaluated with the solver's own {@link Ranking} rules; nothing is searched again.
 *
 * <ol>
 *   <li><b>Weight ranges.</b> For each objective, its share {@code t} of the total weight is varied from 0
 *       to 1 while the other weights keep their proportions. Normalised scores do not depend on weights, so
 *       each option's weighted score is linear in {@code t} and the range over which the recommendation
 *       stays best is found in closed form; the option that takes over just outside each end is named.</li>
 *   <li><b>Nearest alternative.</b> The other frontier option closest in normalised-score space, with the
 *       raw per-objective difference.</li>
 *   <li><b>Binding constraints.</b> For each constraint, the feasible set plus the portfolios that failed
 *       only that constraint is re-normalised and re-ranked: the constraint is binding when the
 *       recommendation changes.</li>
 * </ol>
 */
public final class SensitivityAnalysis {

    private static final double STEP = 1e-6;

    private SensitivityAnalysis() {
    }

    /**
     * @param run the solver's evidence
     * @return the report, or {@code null} when nothing was feasible
     */
    public static SensitivityReport analyse(SolveRun run) {
        DesignOption recommended = run.solution().recommended();
        if (recommended == null || run.feasible().isEmpty()) {
            return null;
        }
        List<double[]> rows = new ArrayList<>();
        List<MetroPortfolio> portfolios = new ArrayList<>();
        int r = -1;
        for (SolveRun.Evaluated e : run.feasible()) {
            if (e.portfolio().equals(recommended.portfolio())) {
                r = rows.size();
            }
            rows.add(e.values());
            portfolios.add(e.portfolio());
        }
        List<String> notes = new ArrayList<>();
        if (!run.solution().exact()) {
            notes.add("The search was narrowed, so sensitivity covers the narrowed field only.");
        }
        double[][] scores = Ranking.normalise(rows, run.directions(), run.scales());
        List<SensitivityReport.WeightRange> ranges = weightRanges(run, rows, portfolios, scores, r);
        SensitivityReport.NearestAlternative nearest = nearest(run, rows, scores, portfolios, r);
        List<SensitivityReport.BindingConstraint> binding = binding(run, recommended, notes);
        return new SensitivityReport(recommended.portfolio().key(), ranges, nearest, binding, notes);
    }

    private static List<SensitivityReport.WeightRange> weightRanges(SolveRun run, List<double[]> rows,
                                                                    List<MetroPortfolio> portfolios,
                                                                    double[][] scores, int r) {
        double[] w = normalised(run.weights());
        List<SensitivityReport.WeightRange> ranges = new ArrayList<>();
        if (w == null) {
            return ranges;
        }
        int n = w.length;
        for (int o = 0; o < n; o++) {
            double[] others = new double[n];
            double rest = 1 - w[o];
            for (int j = 0; j < n; j++) {
                if (j != o) {
                    others[j] = rest > 1e-12 ? w[j] / rest : 1.0 / Math.max(1, n - 1);
                }
            }
            double low = 0;
            double high = 1;
            double sr = score(scores[r], o);
            double rr = rest(scores[r], others);
            for (int k = 0; k < rows.size(); k++) {
                if (k == r) {
                    continue;
                }
                double a = (sr - score(scores[k], o)) - (rr - rest(scores[k], others));
                double b = rr - rest(scores[k], others);
                if (a > 1e-12) {
                    low = Math.max(low, -b / a);
                } else if (a < -1e-12) {
                    high = Math.min(high, -b / a);
                }
            }
            low = clamp(low);
            high = clamp(high);
            String below = low > 0 ? winnerAt(run, rows, portfolios, o, others, low - STEP) : null;
            String above = high < 1 ? winnerAt(run, rows, portfolios, o, others, high + STEP) : null;
            String key = portfolios.get(r).key();
            ranges.add(new SensitivityReport.WeightRange(run.objectiveIds().get(o), w[o], low, high,
                    key.equals(below) ? null : below, key.equals(above) ? null : above));
        }
        return ranges;
    }

    private static String winnerAt(SolveRun run, List<double[]> rows, List<MetroPortfolio> portfolios, int o,
                                   double[] others, double t) {
        double share = clamp(t);
        double[] weights = new double[others.length];
        for (int j = 0; j < weights.length; j++) {
            weights[j] = j == o ? share : (1 - share) * others[j];
        }
        int best = Ranking.best(rows, run.directions(), run.scales(), weights, portfolios);
        return best < 0 ? null : portfolios.get(best).key();
    }

    private static SensitivityReport.NearestAlternative nearest(SolveRun run, List<double[]> rows, double[][] scores,
                                                                List<MetroPortfolio> portfolios, int r) {
        Set<MetroPortfolio> frontier = new LinkedHashSet<>();
        for (DesignOption option : run.solution().frontier()) {
            frontier.add(option.portfolio());
        }
        int bestIndex = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int k = 0; k < rows.size(); k++) {
            if (k == r || !frontier.contains(portfolios.get(k))) {
                continue;
            }
            double sum = 0;
            for (int o = 0; o < scores[k].length; o++) {
                double d = score(scores[k], o) - score(scores[r], o);
                sum += d * d;
            }
            double distance = Math.sqrt(sum);
            if (distance < bestDistance || (distance == bestDistance
                    && portfolios.get(k).key().compareTo(portfolios.get(bestIndex).key()) < 0)) {
                bestDistance = distance;
                bestIndex = k;
            }
        }
        if (bestIndex < 0) {
            return null;
        }
        Map<String, Double> deltas = new LinkedHashMap<>();
        for (int o = 0; o < run.objectiveIds().size(); o++) {
            double a = rows.get(bestIndex)[o];
            double b = rows.get(r)[o];
            if (!Double.isNaN(a) && !Double.isNaN(b)) {
                deltas.put(run.objectiveIds().get(o), a - b);
            }
        }
        return new SensitivityReport.NearestAlternative(portfolios.get(bestIndex).key(), bestDistance, deltas);
    }

    private static List<SensitivityReport.BindingConstraint> binding(SolveRun run, DesignOption recommended,
                                                                     List<String> notes) {
        List<SensitivityReport.BindingConstraint> result = new ArrayList<>();
        if (!run.relaxationComplete()) {
            notes.add("Portfolios containing metros pruned per metro were not all enumerated, so a constraint "
                    + "reported as not binding might still bind through those metros.");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (ConstraintVerdict verdict : recommended.verdicts()) {
            ids.add(verdict.constraintId());
        }
        ids.addAll(run.rejectedOnlyBy().keySet());
        for (String id : ids) {
            List<SolveRun.Evaluated> alone = run.rejectedOnlyBy().getOrDefault(id, List.of());
            if (alone.isEmpty()) {
                result.add(new SensitivityReport.BindingConstraint(id, false, 0, null,
                        "rejects no portfolio on its own"));
                continue;
            }
            List<double[]> rows = new ArrayList<>();
            List<MetroPortfolio> portfolios = new ArrayList<>();
            for (SolveRun.Evaluated e : run.feasible()) {
                rows.add(e.values());
                portfolios.add(e.portfolio());
            }
            for (SolveRun.Evaluated e : alone) {
                rows.add(e.values());
                portfolios.add(e.portfolio());
            }
            int best = Ranking.best(rows, run.directions(), run.scales(), run.weights(), portfolios);
            String key = portfolios.get(best).key();
            boolean binds = !portfolios.get(best).equals(recommended.portfolio());
            result.add(new SensitivityReport.BindingConstraint(id, binds, alone.size(), binds ? key : null,
                    binds ? String.format(Locale.ROOT, "binding: without it %s would be recommended (%d portfolio(s) "
                            + "rejected by it alone)", key, alone.size())
                            : String.format(Locale.ROOT, "not binding: it rejects %d portfolio(s) on its own but none "
                            + "would outrank the recommendation", alone.size())));
        }
        return result;
    }

    private static double[] normalised(double[] weights) {
        double total = 0;
        for (double w : weights) {
            total += Math.max(0, w);
        }
        if (total <= 0) {
            return null;
        }
        double[] w = new double[weights.length];
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.max(0, weights[i]) / total;
        }
        return w;
    }

    private static double score(double[] row, int o) {
        return Double.isNaN(row[o]) ? 0 : row[o];
    }

    private static double rest(double[] row, double[] others) {
        double sum = 0;
        for (int j = 0; j < others.length; j++) {
            sum += others[j] * score(row, j);
        }
        return sum;
    }

    private static double clamp(double t) {
        return Math.max(0, Math.min(1, t));
    }
}
