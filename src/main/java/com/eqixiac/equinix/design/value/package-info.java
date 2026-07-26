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
 * The value-realization layer of the design API: design-time cost modelling that puts a
 * money figure on an interconnect decision. It comprises the rate-card abstraction
 * ({@link com.eqixiac.equinix.design.value.ratecard}), live cloud-provider pricing
 * adapters ({@link com.eqixiac.equinix.design.value.ratecard.provider}), the egress
 * savings calculator ({@link com.eqixiac.equinix.design.value.savings}), and the TCO
 * comparison ({@link com.eqixiac.equinix.design.value.tco}).
 *
 * <p>Prices flow through a <em>layered</em> rate-card architecture: caller-supplied
 * rates ({@code CustomRateCard}) take precedence over live Equinix Fabric pricing
 * ({@code EquinixRateCard}), which takes precedence over bundled, dated reference
 * figures ({@code ReferenceRateCard}); optional provider-API adapters slot in for live
 * cloud egress rates. Every figure is tagged with its
 * {@link com.eqixiac.equinix.design.value.ratecard.PriceSource} so provenance survives
 * aggregation, and a card that cannot price an item returns empty — never a phantom
 * zero — so the next layer gets consulted.</p>
 *
 * <p>The layer's one hard rule is currency honesty, centralized in
 * {@link com.eqixiac.equinix.design.value.CurrencyReconciler}: amounts are only added,
 * subtracted, or compared when they share a currency, and no FX rate is ever fabricated.
 * When components disagree, the engines report the figure as unpriced or partial with
 * per-currency subtotals and a reason, rather than emitting a false single-currency
 * number. Entry points: {@code fabric.savingsCalculator()} and
 * {@code fabric.tcoComparison()} (also available on {@code Design.over(fabric)}).</p>
 *
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 * @see com.eqixiac.equinix.design.value.CurrencyReconciler
 */
package com.eqixiac.equinix.design.value;
