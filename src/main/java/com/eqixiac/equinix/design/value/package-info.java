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
 * <h2>Cloud-to-cloud comparisons</h2>
 * <p><b>Beta.</b> Both entry points accept a peer cloud ({@code toCloud(...)}). The models then
 * price the traffic between two clouds over three paths: the public internet, Equinix Fabric
 * (private egress both ways, two virtual connections, both CSP ports), and the providers' native
 * multicloud link, which has no Equinix component. The native link is priced by
 * {@code RateCard.multicloudLink(...)} as a two-sided
 * {@link com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote}: one flat fee per
 * provider, converted from the published hourly rate at
 * {@code MulticloudLinkQuote.HOURS_PER_MONTH} = 730 h/month.</p>
 *
 * <table>
 *   <caption>Rules the cloud-to-cloud models follow</caption>
 *   <tr><th>Rule</th><th>Mechanism</th></tr>
 *   <tr><td>Every path is priced on the same two-way traffic and a complete component list</td>
 *       <td>a peer cloud switches every archetype to its two-sided form together; on-prem,
 *       whose inputs carry no cloud egress, leaves the default set and is reported unpriced when
 *       requested; a missing peer-side component marks the archetype partially priced</td></tr>
 *   <tr><td>Only published native-link prices are bundled</td><td>each bundled figure records
 *       its source URL and retrieval date, repeated in the quote note; a size, tier, region or
 *       provider without a published figure is an empty side with a reason</td></tr>
 *   <tr><td>The AWS free tier is never assumed</td><td>applied only on
 *       {@code useAwsFreeTier(true)}</td></tr>
 *   <tr><td>The AWS path tier is never derived</td><td>an input, default 1; AWS publishes no
 *       path-to-tier table</td></tr>
 *   <tr><td>No cross-currency sums</td><td>the two sides of a link and the components of each
 *       path run through {@code CurrencyReconciler}</td></tr>
 * </table>
 *
 * <p>The savings estimate also reports the sustained rate at which the flat-fee native link and
 * the per-GB Equinix path cost the same
 * ({@code MulticloudPathComparison.breakEvenSustainedMbps(...)} documents the formula). Without
 * a peer cloud, both models behave and render exactly as before.</p>
 *
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 * @see com.eqixiac.equinix.design.value.CurrencyReconciler
 */
package com.eqixiac.equinix.design.value;
