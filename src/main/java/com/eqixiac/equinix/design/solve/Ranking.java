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

import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ValueScale;

import java.util.Comparator;
import java.util.List;

/**
 * The scoring rules shared by {@link ExhaustiveSolver} and {@link SensitivityAnalysis}, so a re-ranking
 * under other weights or over a relaxed set uses exactly the arithmetic the solver used.
 *
 * <ul>
 *   <li>Normalisation: per objective, 0..100 (100 = best) on the objective's own {@link ValueScale}:
 *       a fixed scale where the objective declares one (a percentage, a ratio to the best cost, an
 *       anchored latency), otherwise the spread of the set being ranked; an unknown value has no
 *       score (NaN).</li>
 *   <li>Weighted score: {@code sum(weight x score) / sum(weight)} over the objectives with a positive
 *       weight; an unknown score contributes nothing and its weight is not redistributed, so missing
 *       data never raises a score.</li>
 *   <li>Order: higher weighted score; then Pareto-optimal before dominated (so the recommendation is
 *       never a dominated option that merely ties); then fewer metros; then portfolio key.</li>
 * </ul>
 */
public final class Ranking {

    private Ranking() {
    }

    /**
     * @param rows       raw values, one row per option, NaN = unknown
     * @param directions per objective
     * @return normalised scores, same shape, NaN where the value is unknown
     */
    public static double[][] normalise(List<double[]> rows, Direction[] directions) {
        return normalise(rows, directions, null);
    }

    /**
     * @param rows       raw values, one row per option, NaN = unknown
     * @param directions per objective
     * @param scales     per objective; {@code null} (or a {@code null} entry) means the spread of the set
     * @return normalised scores, same shape, NaN where the value is unknown
     */
    public static double[][] normalise(List<double[]> rows, Direction[] directions, ValueScale[] scales) {
        int objectives = directions.length;
        double[] best = new double[objectives];
        double[] worst = new double[objectives];
        for (int o = 0; o < objectives; o++) {
            best[o] = Double.NaN;
            worst[o] = Double.NaN;
            for (double[] row : rows) {
                double v = row[o];
                if (Double.isNaN(v)) {
                    continue;
                }
                if (Double.isNaN(best[o]) || directions[o].compare(v, best[o]) < 0) {
                    best[o] = v;
                }
                if (Double.isNaN(worst[o]) || directions[o].compare(v, worst[o]) > 0) {
                    worst[o] = v;
                }
            }
        }
        double[][] scores = new double[rows.size()][objectives];
        for (int i = 0; i < rows.size(); i++) {
            for (int o = 0; o < objectives; o++) {
                double v = rows.get(i)[o];
                if (Double.isNaN(v)) {
                    scores[i][o] = Double.NaN;
                } else {
                    ValueScale scale = scales == null || scales[o] == null ? ValueScale.range() : scales[o];
                    scores[i][o] = scale.score(v, best[o], worst[o], directions[o]);
                }
            }
        }
        return scores;
    }

    /**
     * @param scores  normalised scores
     * @param weights per objective, non-negative
     * @return the weighted score of each option (0 for all when every weight is 0)
     */
    public static double[] weighted(double[][] scores, double[] weights) {
        double total = 0;
        for (double w : weights) {
            total += Math.max(0, w);
        }
        double[] result = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double sum = 0;
            for (int o = 0; o < weights.length; o++) {
                if (!Double.isNaN(scores[i][o]) && weights[o] > 0) {
                    sum += weights[o] * scores[i][o];
                }
            }
            result[i] = total == 0 ? 0 : sum / total;
        }
        return result;
    }

    /**
     * @param weighted   weighted scores
     * @param pareto     whether each option is Pareto-optimal in the set
     * @param portfolios the options' portfolios
     * @return the ranking order as a comparator over option indexes, best first
     */
    public static Comparator<Integer> order(double[] weighted, boolean[] pareto, List<MetroPortfolio> portfolios) {
        return Comparator.comparingDouble((Integer i) -> -weighted[i])
                .thenComparing(i -> !pareto[i])
                .thenComparingInt(i -> portfolios.get(i).size())
                .thenComparing(i -> portfolios.get(i).key());
    }

    /**
     * @param rows       raw values
     * @param directions per objective
     * @param weights    per objective
     * @param portfolios the options
     * @return the index of the option the rules recommend, or -1 for an empty set
     */
    public static int best(List<double[]> rows, Direction[] directions, double[] weights,
                           List<MetroPortfolio> portfolios) {
        return best(rows, directions, null, weights, portfolios);
    }

    /**
     * @param rows       raw values
     * @param directions per objective
     * @param scales     per objective; {@code null} means the spread of the set
     * @param weights    per objective
     * @param portfolios the options
     * @return the index of the option the rules recommend, or -1 for an empty set
     */
    public static int best(List<double[]> rows, Direction[] directions, ValueScale[] scales, double[] weights,
                           List<MetroPortfolio> portfolios) {
        if (rows.isEmpty()) {
            return -1;
        }
        double[] weightedScores = weighted(normalise(rows, directions, scales), weights);
        boolean[] pareto = paretoFlags(rows, directions);
        Comparator<Integer> order = order(weightedScores, pareto, portfolios);
        int best = 0;
        for (int i = 1; i < rows.size(); i++) {
            if (order.compare(i, best) < 0) {
                best = i;
            }
        }
        return best;
    }

    /**
     * @param rows       raw values
     * @param directions per objective
     * @return for each row, whether no other row dominates it
     */
    public static boolean[] paretoFlags(List<double[]> rows, Direction[] directions) {
        boolean[] flags = new boolean[rows.size()];
        for (int index : Pareto.frontier(rows, directions)) {
            flags[index] = true;
        }
        return flags;
    }
}
