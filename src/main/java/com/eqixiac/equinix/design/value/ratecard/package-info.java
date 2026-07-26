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
 * Rate cards: pluggable sources of unit prices for the design-time cost, savings, and
 * TCO models. The central abstraction is
 * {@link com.eqixiac.equinix.design.value.ratecard.RateCard}, whose lookups (connection,
 * Cloud Router, cloud egress, colocation) return {@code Optional} — an empty result
 * means "this card cannot price that item", which is deliberately distinct from a zero
 * price, so callers fall back to another source instead of treating unknown as free.
 *
 * <p>Cards are composed into a precedence chain with
 * {@code RateCard.layered(...)} — the first card that can price an item wins. The
 * conventional ordering is most-trusted first:
 * {@link com.eqixiac.equinix.design.value.ratecard.CustomRateCard} (caller-supplied,
 * e.g. negotiated contract rates), then
 * {@link com.eqixiac.equinix.design.value.ratecard.EquinixRateCard} (live Fabric
 * Pricing API, term-aware with labelled substitutions), then
 * {@link com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard} (bundled, dated
 * indicative figures). {@code RateCard.standardChain(fabric)} builds the default
 * live-then-reference chain the value models use when no card is supplied. Every
 * {@link com.eqixiac.equinix.design.value.ratecard.PriceQuote} and
 * {@link com.eqixiac.equinix.design.value.ratecard.EgressRate} carries its
 * {@link com.eqixiac.equinix.design.value.ratecard.PriceSource} provenance and an
 * optional note flagging substitutions and extrapolations.</p>
 *
 * <p>Two invariants hold across all cards: an unpriceable item yields empty, never a
 * fabricated $0; and quotes in different currencies are never combined — no FX rate is
 * ever invented ({@code PriceQuote.plus} throws on a currency mismatch, and the engines
 * route cross-card sums through {@code CurrencyReconciler}).</p>
 *
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 * @see com.eqixiac.equinix.design.value.ratecard.PriceSource
 * @see com.eqixiac.equinix.design.value.ratecard.provider
 */
package com.eqixiac.equinix.design.value.ratecard;
