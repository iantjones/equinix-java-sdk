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

package com.eqixiac.equinix.design.spi;

import java.util.List;

/**
 * What a {@link Solver} found.
 *
 * @param recommended  the feasible option with the highest weighted score, or {@code null} when no
 *                     portfolio satisfies every constraint
 * @param frontier     the Pareto-optimal options: each is a different, defensible trade-off; ordered
 *                     by weighted score, best first
 * @param evaluated    how many portfolios were examined
 * @param feasible     how many of them satisfied every constraint
 * @param exact        {@code true} when every portfolio in range was examined, so the frontier is
 *                     complete; {@code false} when the search was a heuristic over a subset
 * @param notes        what the reader should know about the search (why it was not exact, which
 *                     constraint removed the most options, ties broken and how)
 */
public record DesignSolution(DesignOption recommended, List<DesignOption> frontier, long evaluated, long feasible,
                             boolean exact, List<String> notes) {

    /** Makes the lists immutable and non-null. */
    public DesignSolution {
        frontier = frontier == null ? List.of() : List.copyOf(frontier);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /** @return {@code true} when at least one portfolio satisfied every constraint */
    public boolean isFeasible() {
        return recommended != null;
    }
}
