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
 * Design-time total-cost-of-ownership comparison across three
 * {@link com.eqixiac.equinix.design.value.tco.DeploymentArchetype}s — public cloud over
 * the internet, on-premises, and Equinix-interconnected. The entry point is
 * {@code fabric.tcoComparison()} (or
 * {@code TcoCalculator.builder(fabric)}), a fluent
 * {@link com.eqixiac.equinix.design.value.tco.TcoCalculator.Builder} terminated by
 * {@code compare()}, which yields a
 * {@link com.eqixiac.equinix.design.value.tco.TcoComparison} of per-archetype
 * {@link com.eqixiac.equinix.design.value.tco.CostBreakdown}s. Archetypes are ranked by
 * total cost over the commitment term ({@code MRC × months + setup}), so one-time
 * charges are never ignored in the recommendation.
 *
 * <p>Prices come from the layered rate-card architecture in
 * {@link com.eqixiac.equinix.design.value.ratecard}: by default live Equinix Fabric
 * pricing backed by bundled reference figures, replaceable with a caller-supplied card.
 * Each breakdown records which components priced ({@code isPriced()}), carries the
 * currency its own components reconciled to, and — per the layer's currency-honesty
 * rule — figures in differing currencies are never summed and no FX rate is fabricated:
 * a mixed-currency archetype is reported unpriced with per-currency subtotals, and the
 * baseline-versus-recommended saving is left null when the two sides' currencies
 * differ.</p>
 *
 * @see com.eqixiac.equinix.design.value.tco.TcoCalculator
 * @see com.eqixiac.equinix.design.value.tco.TcoComparison
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 */
package com.eqixiac.equinix.design.value.tco;
