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
 * A hard limit. A portfolio that fails any constraint is never offered, whatever its objectives
 * say. Implementations must be pure functions of their arguments.
 */
public interface Constraint {

    /**
     * @return a stable id, lower_snake_case (for example {@code max_latency})
     */
    String id();

    /**
     * Cheap per-metro pruning before portfolios are formed. Return {@code false} only when no
     * portfolio containing this metro could ever satisfy the constraint (an excluded metro, a
     * metro outside every allowed zone). The default admits every metro.
     *
     * @param metro   a candidate metro
     * @param context the facts of this run
     * @return whether the metro may appear in a portfolio
     */
    default boolean admits(MetroFacts metro, DesignContext context) {
        return true;
    }

    /**
     * Checks a whole portfolio.
     *
     * @param portfolio the candidate design
     * @param context   the facts of this run
     * @return the verdict with its reason
     */
    ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context);

    /**
     * Whether {@link #check} prices the portfolio through {@link DesignContext#costModel()}. A solver uses it to
     * price ahead, in one batch, the portfolios that reach a pricing constraint ({@link CostModel#prepare}); the
     * verdicts are the same either way. The default is {@code false}: a pricing constraint that says so is only
     * slower, never wrong.
     *
     * @return {@code true} when checking needs prices
     */
    default boolean usesCost() {
        return false;
    }
}
