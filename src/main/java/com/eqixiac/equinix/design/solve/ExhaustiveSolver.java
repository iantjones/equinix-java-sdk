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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.DesignSolution;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.SolveProblem;
import com.eqixiac.equinix.design.spi.ValueScale;
import com.eqixiac.equinix.design.spi.Solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Examines every portfolio in the allowed size range and returns the exact trade-off frontier.
 *
 * <p>Equinix has on the order of 70 metros, so a design of up to three metros is at most about
 * 50,000 portfolios and one of four about 800,000: small enough to enumerate. When the count would
 * exceed {@code maxPortfolios} the solver narrows the candidate field to the metros that score best
 * alone until the count fits, says so in {@link DesignSolution#notes()} and reports
 * {@link DesignSolution#exact()} as {@code false}.</p>
 *
 * <p>Scoring: each objective is scored 0..100 (100 = best) on the scale it declares
 * ({@link com.eqixiac.equinix.design.spi.Objective#scale}): fixed scales for the built-in objectives,
 * the spread of the feasible portfolios for an objective that declares none. The weighted score is the
 * weight-averaged normalised score over the objectives that could be measured for that portfolio; an
 * objective that is unknown for a portfolio contributes nothing and its weight is not redistributed,
 * so missing data never raises a score. Ties are broken by Pareto-optimality (a dominated option never wins
 * a tie), then fewer metros, then portfolio key; see {@link Ranking}. {@link #solveDetailed(SolveProblem)}
 * also keeps the evidence {@link SensitivityAnalysis} re-ranks.</p>
 */
public final class ExhaustiveSolver implements Solver {

    /** The default ceiling on portfolios examined in one run. */
    public static final long DEFAULT_MAX_PORTFOLIOS = 2_000_000L;

    private final long maxPortfolios;

    /** Creates a solver with {@link #DEFAULT_MAX_PORTFOLIOS}. */
    public ExhaustiveSolver() {
        this(DEFAULT_MAX_PORTFOLIOS);
    }

    /**
     * @param maxPortfolios the most portfolios to examine before narrowing the candidate field; at least 1
     */
    public ExhaustiveSolver(long maxPortfolios) {
        if (maxPortfolios < 1) {
            throw new IllegalArgumentException("maxPortfolios must be at least 1");
        }
        this.maxPortfolios = maxPortfolios;
    }

    @Override
    public DesignSolution solve(SolveProblem problem) {
        return solveDetailed(problem).solution();
    }

    /**
     * Solves and keeps the evidence {@link SensitivityAnalysis} needs: the feasible set with raw values
     * and, per constraint, the portfolios that failed only that constraint. To find those for a
     * constraint that prunes metros ({@link Constraint#admits}), metros pruned by exactly one constraint
     * are also enumerated when the widened field still fits the portfolio ceiling; such portfolios never
     * count as feasible and are not included in {@link DesignSolution#evaluated()}.
     *
     * @param problem the problem
     * @return the solution and its evidence
     */
    public SolveRun solveDetailed(SolveProblem problem) {
        long started = System.nanoTime();
        DesignContext context = problem.context();
        List<Objective> objectives = problem.objectives();
        List<Constraint> constraints = problem.constraints();
        List<String> notes = new ArrayList<>();
        Direction[] directions = new Direction[objectives.size()];
        ValueScale[] scales = new ValueScale[objectives.size()];
        double[] weights = new double[objectives.size()];
        List<String> ids = new ArrayList<>();
        for (int o = 0; o < objectives.size(); o++) {
            directions[o] = objectives.get(o).direction();
            scales[o] = objectives.get(o).scale(context);
            Double weight = problem.weights().get(objectives.get(o).id());
            weights[o] = weight == null ? 0 : Math.max(0, weight);
            ids.add(objectives.get(o).id());
        }

        // Per-metro pruning, remembering which constraint pruned each metro.
        List<MetroFacts> field = new ArrayList<>();
        List<MetroFacts> extended = new ArrayList<>();
        Map<String, String> prunedBy = new LinkedHashMap<>();
        for (MetroFacts metro : problem.candidates()) {
            List<String> refused = new ArrayList<>();
            for (Constraint constraint : constraints) {
                if (!constraint.admits(metro, context)) {
                    refused.add(constraint.id());
                }
            }
            if (refused.isEmpty()) {
                field.add(metro);
                extended.add(metro);
            } else if (refused.size() == 1) {
                extended.add(metro);
                prunedBy.put(metro.id().code(), refused.get(0));
            }
        }
        Comparator<MetroFacts> byCode = Comparator.comparing(m -> m.id().code());
        field.sort(byCode);
        extended.sort(byCode);
        if (field.size() < problem.candidates().size()) {
            notes.add((problem.candidates().size() - field.size()) + " of " + problem.candidates().size()
                    + " candidate metros were ruled out by a constraint before search.");
        }

        int maxSize = Math.min(problem.maxMetros(), field.size());
        if (field.isEmpty() || maxSize < problem.minMetros()) {
            notes.add("No portfolio of " + problem.minMetros() + " to " + problem.maxMetros()
                    + " metros can be formed from " + field.size() + " admissible metros.");
            return new SolveRun(new DesignSolution(null, List.of(), 0, 0, true, notes), ids, directions, scales, weights,
                    List.of(), Map.of(), false, problem.candidates().size(), elapsed(started));
        }

        boolean exact = true;
        if (count(field.size(), problem.minMetros(), maxSize) > maxPortfolios) {
            int before = field.size();
            field = narrow(field, problem, maxSize);
            exact = false;
            notes.add("The full search would examine more than " + maxPortfolios + " portfolios, so the field was "
                    + "narrowed from " + before + " to the " + field.size() + " metros that score best alone. "
                    + "The frontier is complete for that field only.");
            maxSize = Math.min(maxSize, field.size());
        }
        boolean relaxationComplete = exact && prunedBy.isEmpty();
        List<MetroFacts> searchField = field;
        if (exact && !prunedBy.isEmpty()) {
            int extendedMax = Math.min(problem.maxMetros(), extended.size());
            if (count(extended.size(), problem.minMetros(), extendedMax) <= maxPortfolios) {
                searchField = extended;
                maxSize = extendedMax;
                relaxationComplete = true;
            } else {
                notes.add("Binding analysis of per-metro constraints is partial: widening the search to the metros "
                        + "they prune would exceed " + maxPortfolios + " portfolios.");
            }
        }

        // Two passes, same answer as one: the first enumerates and applies every constraint that does not price,
        // the second prices in one batch (CostModel.prepare) every portfolio that will be priced, then applies the
        // pricing constraints and measures the objectives. A portfolio already rejected by two constraints is
        // never priced. Enumeration order, and so every list and tie, is unchanged.
        List<Pending> pending = new ArrayList<>();
        long evaluated = 0;
        long seq = 0;
        Map<String, long[]> rejectedSeen = new HashMap<>();
        for (int size = problem.minMetros(); size <= maxSize; size++) {
            int[] pick = new int[size];
            for (int i = 0; i < size; i++) {
                pick[i] = i;
            }
            while (true) {
                MetroPortfolio portfolio = portfolio(searchField, pick);
                String admissionFailure = null;
                boolean tooMany = false;
                for (MetroId metro : portfolio.metros()) {
                    String by = prunedBy.get(metro.code());
                    if (by != null) {
                        if (admissionFailure != null && !admissionFailure.equals(by)) {
                            tooMany = true;
                        }
                        admissionFailure = by;
                    }
                }
                boolean inBaseField = admissionFailure == null;
                if (inBaseField) {
                    evaluated++;
                }
                if (!tooMany) {
                    Pending p = new Pending(seq++, portfolio, inBaseField, admissionFailure);
                    boolean finished = p.check(constraints, context, false);
                    if (finished && p.failures.size() > 1) {
                        p.recordRejection(rejectedSeen);
                    } else {
                        pending.add(p);
                    }
                }
                if (!advance(pick, searchField.size())) {
                    break;
                }
            }
        }
        if (context.costModel() != null && !pending.isEmpty()) {
            List<MetroPortfolio> toPrice = new ArrayList<>(pending.size());
            for (Pending p : pending) {
                toPrice.add(p.portfolio);
            }
            try {
                context.costModel().prepare(toPrice, context);
            } catch (RuntimeException e) {
                // Pricing ahead is an optimisation only: price() answers each portfolio on demand regardless.
            }
        }
        List<SolveRun.Evaluated> feasible = new ArrayList<>();
        Map<String, List<SolveRun.Evaluated>> rejectedOnlyBy = new LinkedHashMap<>();
        for (Pending p : pending) {
            p.check(constraints, context, true);
            p.recordRejection(rejectedSeen);
            if (p.failures.isEmpty()) {
                feasible.add(new SolveRun.Evaluated(p.portfolio, values(objectives, p.portfolio, context)));
            } else if (p.failures.size() == 1) {
                rejectedOnlyBy.computeIfAbsent(p.failures.get(0), k -> new ArrayList<>())
                        .add(new SolveRun.Evaluated(p.portfolio, values(objectives, p.portfolio, context)));
            }
        }
        // Rejections by the first constraint that failed, in the order each constraint first rejected a portfolio.
        Map<String, Long> rejectedBy = new LinkedHashMap<>();
        rejectedSeen.entrySet().stream().sorted(Comparator.comparingLong(e -> e.getValue()[0]))
                .forEach(e -> rejectedBy.put(e.getKey(), e.getValue()[1]));
        if (feasible.isEmpty()) {
            notes.add("None of the " + evaluated + " portfolios examined satisfies every constraint. Rejections by "
                    + "constraint: " + rejectedBy + ".");
            return new SolveRun(new DesignSolution(null, List.of(), evaluated, 0, exact, notes), ids, directions, scales,
                    weights, feasible, rejectedOnlyBy, relaxationComplete, problem.candidates().size(),
                    elapsed(started));
        }
        if (!rejectedBy.isEmpty()) {
            notes.add("Portfolios rejected, by the first constraint that failed: " + rejectedBy + ".");
        }
        if (weightTotal(weights) == 0) {
            notes.add("No objective carries a positive weight, so options are ordered by size and key only.");
        }

        List<double[]> rows = new ArrayList<>();
        List<MetroPortfolio> portfolios = new ArrayList<>();
        for (SolveRun.Evaluated e : feasible) {
            rows.add(e.values());
            portfolios.add(e.portfolio());
        }
        double[][] scores = Ranking.normalise(rows, directions, scales);
        double[] weighted = Ranking.weighted(scores, weights);
        boolean[] pareto = Ranking.paretoFlags(rows, directions);
        Comparator<Integer> order = Ranking.order(weighted, pareto, portfolios);
        List<Integer> frontierIndexes = new ArrayList<>();
        int recommendedIndex = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (pareto[i]) {
                frontierIndexes.add(i);
            }
            if (order.compare(i, recommendedIndex) < 0) {
                recommendedIndex = i;
            }
        }
        frontierIndexes.sort(order);
        List<DesignOption> frontier = new ArrayList<>();
        DesignOption recommended = null;
        for (int index : frontierIndexes) {
            DesignOption option = option(portfolios.get(index), scores[index], weighted[index], true, problem);
            frontier.add(option);
            if (index == recommendedIndex) {
                recommended = option;
            }
        }
        if (recommended == null) {
            recommended = option(portfolios.get(recommendedIndex), scores[recommendedIndex],
                    weighted[recommendedIndex], false, problem);
        }
        return new SolveRun(new DesignSolution(recommended, frontier, evaluated, feasible.size(), exact, notes), ids,
                directions, scales, weights, feasible, rejectedOnlyBy, relaxationComplete, problem.candidates().size(),
                elapsed(started));
    }

    private static double[] values(List<Objective> objectives, MetroPortfolio portfolio, DesignContext context) {
        double[] row = new double[objectives.size()];
        for (int o = 0; o < row.length; o++) {
            row[o] = objectives.get(o).evaluate(portfolio, context).value();
        }
        return row;
    }

    private static double weightTotal(double[] weights) {
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        return total;
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private static DesignOption option(MetroPortfolio portfolio, double[] scoreRow, double weightedScore,
                                       boolean paretoOptimal, SolveProblem problem) {
        Map<String, ObjectiveValue> values = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();
        List<Objective> objectives = problem.objectives();
        for (int o = 0; o < objectives.size(); o++) {
            Objective objective = objectives.get(o);
            values.put(objective.id(), objective.evaluate(portfolio, problem.context()));
            if (!Double.isNaN(scoreRow[o])) {
                scores.put(objective.id(), scoreRow[o]);
            }
        }
        List<ConstraintVerdict> verdicts = new ArrayList<>();
        for (Constraint constraint : problem.constraints()) {
            verdicts.add(constraint.check(portfolio, problem.context()));
        }
        return new DesignOption(portfolio, values, scores, weightedScore,
                problem.context().costModel() == null ? null
                        : problem.context().costModel().price(portfolio, problem.context()),
                verdicts, paretoOptimal);
    }

    /** 0..100, 100 = best; NaN stays NaN; a single distinct value scores 100. */
    private static double normalise(double value, double best, double worst) {
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        if (best == worst) {
            return 100.0;
        }
        return 100.0 * (value - worst) / (best - worst);
    }

    private static MetroPortfolio portfolio(List<MetroFacts> field, int[] pick) {
        List<MetroId> ids = new ArrayList<>(pick.length);
        for (int index : pick) {
            ids.add(field.get(index).id());
        }
        return new MetroPortfolio(ids);
    }

    /** Advances {@code pick} to the next combination in lexicographic order; false when exhausted. */
    private static boolean advance(int[] pick, int n) {
        int k = pick.length;
        int i = k - 1;
        while (i >= 0 && pick[i] == n - k + i) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        pick[i]++;
        for (int j = i + 1; j < k; j++) {
            pick[j] = pick[j - 1] + 1;
        }
        return true;
    }

    /** Number of portfolios of size {@code min..max} from {@code n} metros, saturating at Long.MAX_VALUE. */
    static long count(int n, int min, int max) {
        long total = 0;
        for (int k = min; k <= max; k++) {
            long c = 1;
            for (int i = 1; i <= k; i++) {
                long next = c * (n - k + i);
                if (next / (n - k + i) != c) {
                    return Long.MAX_VALUE;
                }
                c = next / i;
            }
            total += c;
            if (total < 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /**
     * Keeps the largest prefix of the field, ordered by single-metro weighted score, whose portfolio
     * count fits the ceiling. Metros that cannot stand alone as a feasible portfolio are scored on
     * their objectives anyway, so a metro that is only useful in combination is not lost outright.
     */
    private List<MetroFacts> narrow(List<MetroFacts> field, SolveProblem problem, int maxSize) {
        List<Objective> objectives = problem.objectives();
        double[][] rows = new double[field.size()][objectives.size()];
        for (int m = 0; m < field.size(); m++) {
            MetroPortfolio single = new MetroPortfolio(List.of(field.get(m).id()));
            for (int o = 0; o < objectives.size(); o++) {
                rows[m][o] = objectives.get(o).evaluate(single, problem.context()).value();
            }
        }
        double[] score = new double[field.size()];
        for (int o = 0; o < objectives.size(); o++) {
            Direction direction = objectives.get(o).direction();
            double best = Double.NaN;
            double worst = Double.NaN;
            for (double[] row : rows) {
                if (Double.isNaN(row[o])) {
                    continue;
                }
                if (Double.isNaN(best) || direction.compare(row[o], best) < 0) {
                    best = row[o];
                }
                if (Double.isNaN(worst) || direction.compare(row[o], worst) > 0) {
                    worst = row[o];
                }
            }
            Double weight = problem.weights().get(objectives.get(o).id());
            for (int m = 0; m < field.size(); m++) {
                double s = Double.isNaN(rows[m][o]) ? Double.NaN
                        : objectives.get(o).scale(problem.context()).score(rows[m][o], best, worst, direction);
                if (!Double.isNaN(s) && weight != null) {
                    score[m] += Math.max(0, weight) * s;
                }
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int m = 0; m < field.size(); m++) {
            order.add(m);
        }
        order.sort(Comparator.comparingDouble((Integer m) -> -score[m]).thenComparing(m -> field.get(m).id().code()));
        int keep = field.size();
        while (keep > problem.minMetros() && count(keep, problem.minMetros(), Math.min(maxSize, keep)) > maxPortfolios) {
            keep--;
        }
        List<MetroFacts> narrowed = new ArrayList<>();
        for (int i = 0; i < keep; i++) {
            narrowed.add(field.get(order.get(i)));
        }
        narrowed.sort(Comparator.comparing(m -> m.id().code()));
        return narrowed;
    }

    /** A portfolio part-way through its constraint checks; {@code next} is the next constraint to check. */
    private static final class Pending {
        private final long seq;
        private final MetroPortfolio portfolio;
        private final boolean inBaseField;
        private final String admissionFailure;
        private final List<String> failures = new ArrayList<>(2);
        private int next;

        Pending(long seq, MetroPortfolio portfolio, boolean inBaseField, String admissionFailure) {
            this.seq = seq;
            this.portfolio = portfolio;
            this.inBaseField = inBaseField;
            this.admissionFailure = admissionFailure;
            if (admissionFailure != null) {
                failures.add(admissionFailure);
            }
        }

        /**
         * Checks constraints in order, stopping once two have failed (the portfolio is then neither feasible nor
         * evidence for a single constraint). Without {@code allowCost} it stops before the first constraint that
         * prices; it resumes there on the next call.
         *
         * @return {@code true} when no constraint is left to check
         */
        boolean check(List<Constraint> constraints, DesignContext context, boolean allowCost) {
            while (next < constraints.size()) {
                if (failures.size() > 1) {
                    return true;
                }
                Constraint constraint = constraints.get(next);
                if (constraint.id().equals(admissionFailure)) {
                    next++;
                    continue;
                }
                if (!allowCost && constraint.usesCost()) {
                    return false;
                }
                if (!constraint.check(portfolio, context).satisfied()) {
                    failures.add(constraint.id());
                }
                next++;
            }
            return true;
        }

        void recordRejection(Map<String, long[]> seen) {
            if (inBaseField && !failures.isEmpty()) {
                long[] entry = seen.computeIfAbsent(failures.get(0), k -> new long[]{seq, 0});
                entry[0] = Math.min(entry[0], seq);
                entry[1]++;
            }
        }
    }
}
