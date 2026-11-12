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
 * Prices a portfolio. The one place cost comes from: the cost objective, the budget constraint,
 * the reports and the deployment plan all read the same {@link PortfolioCost}, so they cannot
 * disagree. Implementations may call rate cards, and should cache: a solver prices many
 * portfolios that share metros.
 */
@FunctionalInterface
public interface CostModel {

    /**
     * @param portfolio the candidate design
     * @param context   the facts of this run
     * @return every priced line with its source and range, and what could not be priced
     */
    PortfolioCost price(MetroPortfolio portfolio, DesignContext context);

    /**
     * Prices ahead of time what {@link #price} will need for these portfolios, so later calls answer from memory.
     * A solver calls it once per run with every portfolio it is about to price (never one a constraint has
     * already rejected). It must not change any result: an implementation may batch or parallelise independent
     * lookups, but {@link #price} must return what it would have returned without it. The default does nothing.
     *
     * @param portfolios the portfolios about to be priced, in a deterministic order
     * @param context    the facts of this run
     */
    default void prepare(java.util.Collection<MetroPortfolio> portfolios, DesignContext context) {
    }
}
