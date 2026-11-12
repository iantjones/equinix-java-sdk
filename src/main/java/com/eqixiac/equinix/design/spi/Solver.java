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

/**
 * Searches the portfolios a {@link SolveProblem} allows and returns the trade-off frontier and the
 * recommended option. A solver must be deterministic: the same problem gives the same solution,
 * with ties broken by {@link MetroPortfolio#key()}.
 */
@FunctionalInterface
public interface Solver {

    /**
     * @param problem the candidates, objectives, constraints, weights and size limits
     * @return the solution; never {@code null}, and {@link DesignSolution#isFeasible()} is false when
     *         nothing satisfies the constraints
     */
    DesignSolution solve(SolveProblem problem);
}
