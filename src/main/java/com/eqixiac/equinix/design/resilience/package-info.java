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
 * Failure analysis of a design. {@link com.eqixiac.equinix.design.resilience.DesignGraphBuilder} turns a
 * {@code DesignSpec}, a portfolio, its {@code PlacementPlan} and the run's {@code DesignContext} into an explicit,
 * immutable {@link com.eqixiac.equinix.design.resilience.DesignGraph} (sites, metros, Cloud Routers, cloud
 * on-ramps, clouds, workloads, optional peers; access, backbone, primary and standby connections with capacity and
 * round-trip latency). {@link com.eqixiac.equinix.design.resilience.FailureSimulator} simulates
 * {@link com.eqixiac.equinix.design.resilience.FailureScenario}s on it (enumerated by
 * {@link com.eqixiac.equinix.design.resilience.ScenarioCatalog}: every single failure, bounded pairs) and
 * {@link com.eqixiac.equinix.design.resilience.ResilienceAnalyzer} assembles a
 * {@link com.eqixiac.equinix.design.resilience.ResilienceReport}: what still works per scenario (paths, latency,
 * capacity versus demand), blast-radius ranking, single points of failure, correlated risks, N-1 and N-2 verdicts,
 * availability estimates under stated assumptions and re-simulated remediation.
 *
 * <p>{@link com.eqixiac.equinix.design.resilience.ResiliencePlugin} plugs the simulation into the Metro Optimizer:
 * the {@code simulated_resilience} objective, an opt-in {@code min_survival} constraint and a failover-aware
 * {@code geo_zones_failover} constraint.</p>
 */
package com.eqixiac.equinix.design.resilience;
