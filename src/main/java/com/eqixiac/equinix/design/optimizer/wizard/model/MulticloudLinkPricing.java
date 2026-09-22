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

package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The cost of one cloud-to-cloud flow over each of its two paths: the native multicloud link
 * (billed by the two cloud providers) and the Equinix path the same flow would use (two Fabric
 * virtual connections plus the cloud providers' own port fees), with the sustained rate at which
 * the two cost the same.
 *
 * <p><b>Beta.</b> Every figure is a design-time estimate, not a quote.</p>
 *
 * <table>
 *   <caption>Fields and units</caption>
 *   <tr><th>Field</th><th>Unit</th><th>{@code null} means</th></tr>
 *   <tr><td>{@code nativeQuote}</td><td>two-sided {@link MulticloudLinkQuote}; each side is a
 *       monthly recurring charge converted from the provider's hourly rate at 730 h per month</td>
 *       <td>no rate card in the chain holds data for the provider pair</td></tr>
 *   <tr><td>{@code nativeMonthly}, {@code nativeCurrency}</td><td>monthly, ISO 4217</td>
 *       <td>a side is unpriced, or the two sides are in different currencies</td></tr>
 *   <tr><td>{@code equinixFixedMonthly}, {@code equinixCurrency}</td><td>monthly, ISO 4217</td>
 *       <td>a component is unpriced, or the components span currencies</td></tr>
 *   <tr><td>{@code equinixPerGb}, {@code nativePerGb}</td><td>currency per decimal GB, the two
 *       clouds' rates on that path summed</td><td>a cloud's rate on that path is not published</td></tr>
 *   <tr><td>{@code breakEvenSustainedMbps}</td><td>Mbps, both directions summed, symmetric
 *       traffic</td><td>an input is {@code null}, the currencies differ, or no break-even exists</td></tr>
 * </table>
 *
 * <p>A {@code null} amount is an unpriced figure, never a zero cost; {@code getNotes()} names what
 * is missing. Amounts in different currencies are never summed and no FX rate is applied.</p>
 *
 * <h2>Equinix fixed cost</h2>
 * <p>{@code equinixFixedMonthly} is the sum of the components listed in
 * {@code getEquinixComponents()}: the Fabric virtual connection to each cloud, priced by the
 * wizard's rate card exactly as the plan's provider connections are, and each cloud's
 * interconnect-port reference figure from {@code ReferenceRateCard}. The metro's Cloud Router is
 * added only when the flow is its sole use (no other provider connection or backbone link at the
 * metro and no user site in the request); otherwise it is left out and a note says so, which
 * understates the Equinix fixed cost and overstates the break-even rate.</p>
 *
 * <h2>Break-even</h2>
 * <p>Computed by {@code MulticloudPathComparison.breakEvenSustainedMbps(nativeMonthly,
 * equinixFixedMonthly, equinixPerGb, nativePerGb)}; that method documents the formula. It is
 * computed only when the native fee, the Equinix fixed cost and the per-GB rates are all in one
 * currency ({@code perGbCurrency}). {@code nativeCheaperAboveBreakEven} states which path costs
 * less on each side of the rate: in the usual case the Equinix path costs less below it and the
 * native link above it.</p>
 */
@Value
@Builder(toBuilder = true)
public class MulticloudLinkPricing {

    /** The two-sided native link quote as resolved, or {@code null}. */
    MulticloudLinkQuote nativeQuote;

    /** Both providers' flat monthly fees summed, or {@code null}. */
    BigDecimal nativeMonthly;

    /** The ISO 4217 code of {@code nativeMonthly}, or {@code null}. */
    String nativeCurrency;

    /** The Equinix path's fixed monthly cost for the same flow, or {@code null}. */
    BigDecimal equinixFixedMonthly;

    /** The ISO 4217 code of {@code equinixFixedMonthly}, or {@code null}. */
    String equinixCurrency;

    /** One line per component of {@code equinixFixedMonthly}, with amount and provenance. */
    List<String> equinixComponents;

    /** The two clouds' private-egress rates per GB, summed; or {@code null}. */
    BigDecimal equinixPerGb;

    /** The two clouds' per-GB rates on the native link, summed; or {@code null}. */
    BigDecimal nativePerGb;

    /**
     * The ISO 4217 code of {@code equinixPerGb} and {@code nativePerGb}; {@code null} when either
     * is {@code null} or the four rates are not all in one known currency. A break-even needs it
     * to equal {@code nativeCurrency} and {@code equinixCurrency}.
     */
    String perGbCurrency;

    /** The break-even sustained rate in Mbps, both directions summed; or {@code null}. */
    BigDecimal breakEvenSustainedMbps;

    /**
     * The sense of {@code breakEvenSustainedMbps}: {@code true} when the native link costs less
     * above it and the Equinix path below it (native fee above the Equinix fixed cost, Equinix
     * per-GB rate above the native per-GB rate); {@code false} when the two cross the other way
     * and the Equinix path costs less above it. Meaningful only when
     * {@code breakEvenSustainedMbps} is not {@code null}; {@code false} otherwise.
     */
    boolean nativeCheaperAboveBreakEven;

    /** Provenance, assumptions and the reason for each {@code null} figure, in reading order. */
    List<String> notes;

    /**
     * {@code getBreakEvenSustainedMbps()} as an {@link Optional}.
     *
     * @return the break-even rate in Mbps, or empty when it could not be computed
     */
    public Optional<BigDecimal> breakEvenSustainedMbps() {
        return Optional.ofNullable(breakEvenSustainedMbps);
    }

    /**
     * Whether both providers' sides of the native link are priced in one currency.
     *
     * @return {@code true} when {@code getNativeMonthly()} is not {@code null}
     */
    public boolean isNativePriced() {
        return nativeMonthly != null;
    }
}
