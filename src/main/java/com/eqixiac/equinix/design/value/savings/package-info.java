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
 * Cloud-egress savings estimation: how much routing a monthly data volume over an
 * Equinix private interconnect saves versus egressing it to the public internet, net of
 * the Equinix interconnect cost. The entry point is {@code fabric.savingsCalculator()}
 * (or {@code SavingsCalculator.builder(fabric)}), a fluent
 * {@link com.eqixiac.equinix.design.value.savings.SavingsCalculator.Builder} terminated
 * by {@code calculate()}, which yields a
 * {@link com.eqixiac.equinix.design.value.savings.SavingsEstimate} with the net
 * monthly/annual savings, break-even volume, and setup-payback figures. Volumes are
 * expressed via {@link com.eqixiac.equinix.design.value.savings.DataUnit} (decimal/SI —
 * 1&nbsp;TB = 1000&nbsp;GB).
 *
 * <p>Rates come from the layered rate-card architecture in
 * {@link com.eqixiac.equinix.design.value.ratecard}: by default live Equinix Fabric
 * pricing backed by bundled reference figures (which supply the cloud-egress rates), so
 * the calculator works with zero configuration; supply a custom or provider-API card to
 * override. The estimate is honest about what it could not price: the
 * {@code egressPriced}/{@code equinixPriced}/{@code complete} flags and the disclaimer
 * name any missing component, and — per the layer's currency-honesty rule — figures in
 * differing currencies are never subtracted or summed: a cross-currency interconnect
 * cost is excluded (and quoted in the disclaimer) rather than mislabelled, with the
 * derived net figures omitted instead of fabricated.</p>
 *
 * @see com.eqixiac.equinix.design.value.savings.SavingsCalculator
 * @see com.eqixiac.equinix.design.value.savings.SavingsEstimate
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 */
package com.eqixiac.equinix.design.value.savings;
