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
 * One feasible design with everything measured about it.
 *
 * @param portfolio      the metros
 * @param values         each objective's measurement, by objective id, in the objective's own unit
 * @param scores         each objective normalised to 0..100 across the feasible options of this run
 *                       (100 = best seen), by objective id; an unknown measurement has no entry
 * @param weightedScore  the weights applied to {@code scores}, 0..100: the single figure that picks
 *                       the recommended option
 * @param cost           the priced lines behind the cost objective
 * @param verdicts       every constraint check, all satisfied
 * @param paretoOptimal  {@code true} when no other feasible option is at least as good on every
 *                       objective and better on one
 */
public record DesignOption(MetroPortfolio portfolio, Map<String, ObjectiveValue> values, Map<String, Double> scores,
                           double weightedScore, PortfolioCost cost, List<ConstraintVerdict> verdicts,
                           boolean paretoOptimal) {

    /** Makes the collections immutable and insertion-ordered. */
    public DesignOption {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
        verdicts = verdicts == null ? List.of() : List.copyOf(verdicts);
    }
}
