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

/**
 * Extension points of the design engines. Everything an engine decides with is one of these small
 * interfaces, so a new objective, limit, pricing source or search strategy is added by implementing
 * one, never by editing an engine.
 *
 * <ul>
 *   <li>{@link com.eqixiac.equinix.design.spi.DesignContext}: the read-only facts of one run (the
 *       {@code DesignSpec}, the metro catalogue, latency, cloud reachability, the cost model).
 *       Built once, so the other plug-ins are pure functions of it and can be unit-tested without
 *       a network.</li>
 *   <li>{@link com.eqixiac.equinix.design.spi.CandidateGenerator}: which metros are worth considering.</li>
 *   <li>{@link com.eqixiac.equinix.design.spi.Constraint}: a hard limit on a metro or a portfolio.</li>
 *   <li>{@link com.eqixiac.equinix.design.spi.Objective}: one measurable quality of a portfolio
 *       (latency, cost, resilience...), in its own unit, with a direction.</li>
 *   <li>{@link com.eqixiac.equinix.design.spi.CostModel}: prices a portfolio line by line, with the
 *       source and an uncertainty range for every figure.</li>
 *   <li>{@link com.eqixiac.equinix.design.spi.Solver}: searches portfolios and returns the
 *       trade-off frontier plus the option the weights recommend.</li>
 * </ul>
 *
 * <p>A <em>portfolio</em> ({@link com.eqixiac.equinix.design.spi.MetroPortfolio}) is the set of
 * metros a design deploys in. Objectives are evaluated on portfolios, not on single metros, because
 * the qualities that matter (worst-case latency, total cost, surviving a metro loss) are properties
 * of the set.</p>
 *
 * <p>Third-party plug-ins are discovered through {@link java.util.ServiceLoader}: see
 * {@link com.eqixiac.equinix.design.spi.DesignPlugin}.</p>
 */
package com.eqixiac.equinix.design.spi;
