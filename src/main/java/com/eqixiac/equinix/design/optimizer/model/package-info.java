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
 * Immutable value types for the Metro Optimization Engine — the request side (sites, provider
 * requirements, workload specs, constraints, scoring weights) and the result side (ranked
 * recommendations, scores, topology, latency matrix, risk assessment, cost estimates).
 *
 * <p>Request types are assembled by
 * {@link com.eqixiac.equinix.design.optimizer.MetroOptimizer.Builder} into an
 * {@link com.eqixiac.equinix.design.optimizer.model.OptimizationRequest}; the engine returns an
 * {@link com.eqixiac.equinix.design.optimizer.model.OptimizationResult} whose parts —
 * {@link com.eqixiac.equinix.design.optimizer.model.MetroRecommendation},
 * {@link com.eqixiac.equinix.design.optimizer.model.MetroScore},
 * {@link com.eqixiac.equinix.design.optimizer.model.DeploymentTopology},
 * {@link com.eqixiac.equinix.design.optimizer.model.LatencyMatrix},
 * {@link com.eqixiac.equinix.design.optimizer.model.RiskAssessment},
 * {@link com.eqixiac.equinix.design.optimizer.model.CostEstimate} — are all Lombok
 * {@code @Value} types (get-prefixed accessors, builders, no setters).</p>
 *
 * <p>Cost figures are stamped per metro with their own currency and
 * {@link com.eqixiac.equinix.design.value.ratecard.PriceSource}; cross-currency totals are never
 * fabricated (the aggregate total is {@code null} when metros span currencies).</p>
 *
 * @see com.eqixiac.equinix.design.optimizer.model.OptimizationResult
 * @see com.eqixiac.equinix.design.optimizer.MetroOptimizer
 */
package com.eqixiac.equinix.design.optimizer.model;
