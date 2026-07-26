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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the bundled {@link ReferenceRateCard} and the out-of-the-box savings
 * calculation it enables (no caller-supplied rate card).
 */
class ReferenceRateCardTest {

    private static final ReferenceRateCard CARD = ReferenceRateCard.standard();

    @Test
    void loadsEgressRatesPerProviderAndPath() {
        assertEquals(0, new BigDecimal("0.09").compareTo(
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.02").compareTo(
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.PRIVATE, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.087").compareTo(
                CARD.egress(CloudProviderType.AZURE, null, EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(0, new BigDecimal("0.0186").compareTo(
                CARD.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.PRIVATE, Term.MONTH_12).orElseThrow().getPricePerGb()));
        assertEquals(PriceSource.REFERENCE,
                CARD.egress(CloudProviderType.AWS, "us", EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getSource());
    }

    @Test
    void gcpEgressFiguresAreConvertedFromPerGibListPrices() {
        // Google publishes its egress list prices per GiB ($0.12 / $0.02); EgressRate.costFor()
        // multiplies by SI-decimal GB, so storing the raw per-GiB figures in the per-GB field
        // overstated GCP costs ~7.4%. The bundled table carries the converted per-GB values
        // (0.12 / 1.073741824 ≈ 0.1118, 0.02 / 1.073741824 ≈ 0.0186), matching the convention of
        // the live GCP billing-catalog adapter, and the notes flag the conversion.
        var internet = CARD.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.INTERNET, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("0.1118").compareTo(internet.getPricePerGb()),
                "the raw $0.12/GiB list price must be stored converted to per-GB");
        assertTrue(internet.getNote().contains("converted from per-GiB list price"),
                "the note must flag the GiB->GB conversion: " + internet.getNote());

        var privatePath = CARD.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.PRIVATE, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("0.0186").compareTo(privatePath.getPricePerGb()),
                "the raw $0.02/GiB list price must be stored converted to per-GB");
        assertTrue(privatePath.getNote().contains("converted from per-GiB list price"));

        // AWS and Azure quote their list prices in decimal GB already — no conversion applied.
        assertEquals(0, new BigDecimal("0.09").compareTo(
                CARD.egress(CloudProviderType.AWS, null, EgressPath.INTERNET, Term.MONTH_12).orElseThrow().getPricePerGb()));
    }

    @Test
    void connectionPricingUsesNearestBandwidthTier() {
        assertEquals(0, new BigDecimal("75").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        assertEquals(0, new BigDecimal("350").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 10_000, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        // 5000 Mbps has no exact tier -> ceiling to the 10G tier (350).
        assertEquals(0, new BigDecimal("350").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 5_000, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        // 50 Mbps is below the smallest tier -> ceiling up to the 100 Mbps tier (75).
        assertEquals(0, new BigDecimal("75").compareTo(
                CARD.connection(ConnectionType.EVPL_VC, 50, MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
    }

    @Test
    void connectionAboveTopTierIsExtrapolatedAndTaggedNotSilentlyUnderpriced() {
        // 40000 Mbps exceeds the largest tabulated tier (10000 Mbps @ 350). It must NOT be
        // silently priced at the 10G flat rate (the old floor-back bug). It is extrapolated
        // linearly from the top tier's per-Mbps rate: 350 * 40000 / 10000 = 1400, and the
        // quote's note clearly flags it as an extrapolation rather than a tabulated figure.
        PriceQuote aboveTop = CARD.connection(ConnectionType.EVPL_VC, 40_000, MetroCode.DC, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("1400").compareTo(aboveTop.getMonthlyRecurring()),
                "above-top-tier bandwidth must not be silently under-priced at the 10G flat rate");
        assertNotEquals(0, new BigDecimal("350").compareTo(aboveTop.getMonthlyRecurring()),
                "must not return the top tier's flat price for a request above every tier");
        assertTrue(aboveTop.getNote().toLowerCase().contains("extrapolat"),
                "an above-top-tier price must be clearly tagged as extrapolated");
        assertEquals(PriceSource.REFERENCE, aboveTop.getSource());
    }

    @Test
    void cloudRouterFallsBackToStandard() {
        PriceQuote exact = CARD.cloudRouter("STANDARD", null, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("1200").compareTo(exact.getMonthlyRecurring()));
        assertFalse(exact.getNote().contains("substituted"),
                "an exact package match carries no substitution label: " + exact.getNote());

        // An unlisted package still falls back to the STANDARD figure, but the substitution must
        // be named in the note — never silently passed off as a genuine package-specific figure.
        PriceQuote substituted = CARD.cloudRouter("NONEXISTENT", null, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("1200").compareTo(substituted.getMonthlyRecurring()),
                "unknown package falls back to STANDARD");
        assertTrue(substituted.getNote().contains("STANDARD substituted for NONEXISTENT"),
                "the fallback must label the substitution: " + substituted.getNote());
    }

    @Test
    void exposesOnPremAndCspPortReferenceFigures() {
        assertEquals(0, new BigDecimal("0.75").compareTo(CARD.onPrem("transitPerMbpsMonth").orElseThrow()));
        assertEquals(0, new BigDecimal("195").compareTo(CARD.onPrem("powerPerKwMonth").orElseThrow()));
        assertEquals(0, new BigDecimal("300").compareTo(CARD.equinixCrossConnectMonthly().orElseThrow()));
        assertEquals(0, new BigDecimal("1642.50").compareTo(
                CARD.cspInterconnectPortMonthly(CloudProviderType.AWS, 10_000).orElseThrow()));
        assertNotNull(CARD.disclaimer());
        assertEquals("2026-06", CARD.asOf());
    }

    @Test
    void cspPortAboveTopTierIsExtrapolatedAndTaggedNotSilentlyFloored() {
        // 40 Gbps exceeds AWS's largest tabulated Direct Connect tier (10 Gbps @ 1642.50). It
        // must NOT floor back to the 10G flat price (the old bug: a 100G requirement priced as a
        // single 10G circuit). Mirroring connection(), it is extrapolated linearly from the top
        // tier's per-Mbps rate: 1642.50 × 40000 / 10000 = 6570.00.
        assertEquals(0, new BigDecimal("6570.00").compareTo(
                CARD.cspInterconnectPortMonthly(CloudProviderType.AWS, 40_000).orElseThrow()),
                "above-top-tier bandwidth must extrapolate, not floor to the 10G flat price");

        // The quote variant tags the extrapolation so it is never mistaken for a tabulated rate.
        PriceQuote quote = CARD.cspInterconnectPortMonthlyQuote(CloudProviderType.AWS, 40_000).orElseThrow();
        assertEquals(0, new BigDecimal("6570.00").compareTo(quote.getMonthlyRecurring()));
        assertTrue(quote.getNote().toLowerCase().contains("extrapolat"),
                "an above-top-tier CSP port price must be clearly tagged as extrapolated");
        assertEquals(PriceSource.REFERENCE, quote.getSource());

        // At or below a tabulated tier the figure is the tabulated one, untagged.
        PriceQuote tabulated = CARD.cspInterconnectPortMonthlyQuote(CloudProviderType.AWS, 10_000).orElseThrow();
        assertEquals(0, new BigDecimal("1642.50").compareTo(tabulated.getMonthlyRecurring()));
        assertFalse(tabulated.getNote().toLowerCase().contains("extrapolat"));
    }

    @Test
    void savingsCalculatorWorksOutOfTheBoxViaReferenceCard() {
        // No rate card and a null Fabric: the live card yields nothing (caught), the
        // reference card supplies both egress rates and a fallback connection price.
        SavingsEstimate s = SavingsCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .calculate();

        assertTrue(s.isComplete(), "egress + connection both resolved from the reference card");
        assertEquals(0, new BigDecimal("4500").compareTo(s.getInternetEgressMonthlyCost()));
        assertEquals(0, new BigDecimal("1000").compareTo(s.getPrivateEgressMonthlyCost()));
        assertEquals(0, new BigDecimal("3500").compareTo(s.getMonthlyEgressSavings()));
        assertEquals(0, new BigDecimal("350").compareTo(s.getEquinixMonthlyCost()), "reference 10G VC");
        assertEquals(0, new BigDecimal("3150").compareTo(s.getNetMonthlySavings()), "3500 − 350");
    }
}
