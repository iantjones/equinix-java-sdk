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

package com.eqixiac.equinix.design.optimizer.model;

/**
 * How the search went.
 *
 * @param candidates     metros in the catalogue offered to the solver
 * @param minMetros      fewest metros a design could have
 * @param maxMetros      most metros a design could have
 * @param evaluated      portfolios examined (within the admitted field)
 * @param feasible       portfolios that satisfied every constraint
 * @param frontier       Pareto-optimal options
 * @param exact          {@code true} when every portfolio in range was examined
 * @param elapsedMillis  wall-clock time of the search, ms
 */
public record SolveStatistics(int candidates, int minMetros, int maxMetros, long evaluated, long feasible, int frontier,
                              boolean exact, long elapsedMillis) {
}
