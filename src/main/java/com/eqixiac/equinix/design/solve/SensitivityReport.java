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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How firm the recommendation is. Built by {@link SensitivityAnalysis} from the solver's own evidence,
 * without searching again.
 *
 * @param recommended   the recommended portfolio key
 * @param weightRanges  per weighted objective, the weight range that keeps the recommendation
 * @param nearest       the closest other frontier option, or {@code null} when the frontier has one option
 * @param constraints   per constraint, whether removing it would change the recommendation
 * @param notes         caveats (incomplete relaxation, narrowed search)
 */
public record SensitivityReport(String recommended, List<WeightRange> weightRanges, NearestAlternative nearest,
                                List<BindingConstraint> constraints, List<String> notes) {

    /** Makes the lists immutable. */
    public SensitivityReport {
        weightRanges = weightRanges == null ? List.of() : List.copyOf(weightRanges);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * The range of one objective's share of the total weight over which the recommendation stays the same,
     * the other weights keeping their proportions to each other.
     *
     * @param objective      the objective id
     * @param weight         its current share of the total weight, 0..1
     * @param low            the smallest share that keeps the recommendation
     * @param high           the largest share that keeps the recommendation
     * @param belowTakeover  the portfolio recommended just below {@code low}, or {@code null} when {@code low} is 0
     * @param aboveTakeover  the portfolio recommended just above {@code high}, or {@code null} when {@code high} is 1
     */
    public record WeightRange(String objective, double weight, double low, double high, String belowTakeover,
                              String aboveTakeover) {
    }

    /**
     * The frontier option nearest the recommendation in normalised-score space.
     *
     * @param portfolio  its key
     * @param distance   Euclidean distance over the objectives' 0..100 scores
     * @param deltas     per objective, its raw value minus the recommendation's (in the objective's unit);
     *                   absent when either value is unknown
     */
    public record NearestAlternative(String portfolio, double distance, Map<String, Double> deltas) {
        /** Makes the map immutable. */
        public NearestAlternative {
            deltas = deltas == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(deltas));
        }
    }

    /**
     * Whether a constraint decides the answer.
     *
     * @param constraint      the constraint id
     * @param binding         {@code true} when removing it changes the recommendation
     * @param rejectedAlone   how many portfolios it alone rejected
     * @param alternative     the portfolio recommended without it, when binding
     * @param explanation     a sentence for the reader
     */
    public record BindingConstraint(String constraint, boolean binding, int rejectedAlone, String alternative,
                                    String explanation) {
    }
}
