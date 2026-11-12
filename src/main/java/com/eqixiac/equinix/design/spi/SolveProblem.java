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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a {@link Solver} needs.
 *
 * @param context      the facts of this run
 * @param candidates   the metros to form portfolios from
 * @param objectives   what to measure; each id appears once
 * @param constraints  the hard limits
 * @param weights      relative weight per objective id (need not sum to 1; missing ids weigh 0)
 * @param minMetros    fewest metros in a portfolio, at least 1
 * @param maxMetros    most metros in a portfolio, at least {@code minMetros}
 */
public record SolveProblem(DesignContext context, List<MetroFacts> candidates, List<Objective> objectives,
                           List<Constraint> constraints, Map<String, Double> weights, int minMetros, int maxMetros) {

    /** Validates the sizes and makes the collections immutable. */
    public SolveProblem {
        if (context == null) {
            throw new IllegalArgumentException("A problem needs a context");
        }
        if (minMetros < 1 || maxMetros < minMetros) {
            throw new IllegalArgumentException("Need 1 <= minMetros <= maxMetros, got " + minMetros + ".." + maxMetros);
        }
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        weights = weights == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }
}
