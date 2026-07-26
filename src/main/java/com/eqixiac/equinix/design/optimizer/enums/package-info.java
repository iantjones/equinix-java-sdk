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
 * Enumerations that parameterize the Metro Optimization Engine:
 * {@link com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy} (scoring weight
 * presets), {@link com.eqixiac.equinix.design.optimizer.enums.WorkloadType} (workload archetypes
 * with built-in profiles), {@link com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity}
 * (default per-workload placement ceilings),
 * {@link com.eqixiac.equinix.design.optimizer.enums.SiteRole} (site importance multipliers),
 * {@link com.eqixiac.equinix.design.optimizer.enums.RedundancyTier} (metro-count floors and
 * region-diversity selection), {@link com.eqixiac.equinix.design.optimizer.enums.ComplianceZone}
 * (region-based data-sovereignty zones with deployment-level AND semantics),
 * {@link com.eqixiac.equinix.design.optimizer.enums.ScoreCategory} (the five scoring
 * dimensions), and {@link com.eqixiac.equinix.design.optimizer.enums.RiskSeverity} (risk-finding
 * levels).
 *
 * @see com.eqixiac.equinix.design.optimizer.MetroOptimizer
 */
package com.eqixiac.equinix.design.optimizer.enums;
