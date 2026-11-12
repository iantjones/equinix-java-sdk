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

import com.eqixiac.equinix.design.spi.DesignSolution;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ValueScale;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link DesignSolution} plus the evidence behind it, kept so the answer can be interrogated without
 * searching again: every feasible portfolio with its raw objective values, and every portfolio that
 * failed exactly one constraint (grouped by that constraint). {@link SensitivityAnalysis} re-ranks
 * these sets under other weights, or with one constraint removed, using the solver's own
 * {@link Ranking} rules, so its answers are exact for the portfolios searched.
 *
 * @param solution             what the solver returned
 * @param objectiveIds         objective ids, in the order of every values row
 * @param directions           objective directions, same order
 * @param weights              the weights used, same order (not necessarily summing to 1)
 * @param feasible             every feasible portfolio, in enumeration order
 * @param rejectedOnlyBy       per constraint id, the portfolios that failed that constraint and no other
 * @param relaxationComplete   {@code false} when portfolios containing a metro that a constraint prunes per
 *                             metro were not enumerated (the extended field exceeded the ceiling), so the
 *                             binding analysis of those constraints is incomplete
 * @param candidates           metros offered to the solver
 * @param elapsedMillis        wall-clock time of the search, ms
 */
public record SolveRun(DesignSolution solution, List<String> objectiveIds, Direction[] directions,
                       ValueScale[] scales, double[] weights,
                       List<Evaluated> feasible, Map<String, List<Evaluated>> rejectedOnlyBy,
                       boolean relaxationComplete, int candidates, long elapsedMillis) {

    /** Makes the collections immutable. */
    public SolveRun {
        objectiveIds = List.copyOf(objectiveIds);
        feasible = List.copyOf(feasible);
        Map<String, List<Evaluated>> copy = new LinkedHashMap<>();
        rejectedOnlyBy.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        rejectedOnlyBy = Collections.unmodifiableMap(copy);
    }

    /**
     * A portfolio and its raw objective values (NaN = unknown).
     *
     * @param portfolio the metros
     * @param values    one value per objective, in {@link SolveRun#objectiveIds()} order
     */
    public record Evaluated(MetroPortfolio portfolio, double[] values) {
    }
}
