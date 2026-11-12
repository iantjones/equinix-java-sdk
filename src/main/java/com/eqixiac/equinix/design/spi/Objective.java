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
 * One measurable quality of a portfolio, in its own unit. Implementations must be pure functions
 * of their arguments (no I/O, no clock): everything they need is on the {@link DesignContext}.
 */
public interface Objective {

    /** Site-weighted round-trip latency from the sites to their nearest metro in the portfolio, in ms. */
    String LATENCY = "latency";
    /** Total monthly cost of the portfolio in the spec's currency. */
    String COST = "cost";
    /** Share of the required clouds and service providers reachable from the portfolio, 0 to 100. */
    String PROVIDER_COVERAGE = "provider_coverage";
    /** How well the portfolio survives losing a metro or a region, 0 to 100. */
    String RESILIENCE = "resilience";
    /** Share of the stated compliance and sovereignty zones the portfolio satisfies, 0 to 100. */
    String COMPLIANCE = "compliance";

    /**
     * @return the id weights refer to (for example {@link #LATENCY}); stable, lower_snake_case
     */
    String id();

    /**
     * @return whether smaller or larger values are better
     */
    Direction direction();

    /**
     * @return the unit of {@link ObjectiveValue#value()}, for display (for example {@code ms}, {@code USD/month}, {@code score})
     */
    String unit();

    /**
     * Measures a portfolio.
     *
     * @param portfolio the metros of one candidate design
     * @param context   the facts of this run
     * @return the measurement, or {@link ObjectiveValue#unknown(String)} when it cannot be made
     */
    ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context);

    /**
     * How this objective's values become 0..100 scores for the weighted ranking. The default is the
     * spread of the options being compared ({@link ValueScale#range()}); an objective that knows what a
     * good value is should return a fixed scale instead, so a small difference stays small.
     *
     * @param context the facts of this run
     * @return the scale
     */
    default ValueScale scale(DesignContext context) {
        return ValueScale.range();
    }
}
