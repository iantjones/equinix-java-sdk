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
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The currency-mixing guard applied to the two-sided native link and to the two-sided form of the
 * other archetypes. The two sides of a native link are billed by different companies, so a
 * negotiated EUR side next to a USD list-price side is a realistic input. No FX rate exists
 * anywhere in the SDK: a cross-currency sum is withheld and the per-currency subtotals are
 * reported instead. Mixed currencies are built by layering single-currency
 * {@link CustomRateCard}s over the USD reference card.
 */
class MulticloudCurrencyMixingTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    /** A EUR card that prices only the Google side of a 10 Gbps native link (EUR 12,000/month). */
    private static CustomRateCard eurGoogleSide() {
        return CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000"))
                .build();
    }

    private static TcoCalculator.Builder tco(RateCard card) {
        return TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(card);
    }

    private static SavingsCalculator.Builder savings(RateCard card) {
        return SavingsCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(card);
    }

    // ── MulticloudLinkQuote ──

    @Test
    void quoteWithholdsTheCombinedFigureWhenTheSidesAreInDifferentCurrencies() {
        MulticloudLinkQuote quote = RateCard.layered(eurGoogleSide(), ReferenceRateCard.standard())
                .multicloudLink(com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest.builder()
                        .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                        .bandwidthMbps(10_000).build())
                .orElseThrow();

        assertTrue(quote.isFullyPriced(), "both sides priced: USD 9000.90 (reference) and EUR 12000 (custom)");
        assertTrue(quote.isMixedCurrency());
        assertTrue(quote.combinedMonthly().isEmpty(), "USD + EUR is never summed");
        assertTrue(quote.combinedSetup().isEmpty());
        assertTrue(quote.combinedCurrency().isEmpty());
        assertEquals("USD 9000.90, EUR 12000.00", quote.monthlySubtotalsByCurrency());
    }

    @Test
    void aZeroPricedSideDoesNotCreateACurrencyMix() {
        // A USD free-tier side (zero) next to a negotiated EUR side: zero is zero in any currency.
        MulticloudLinkQuote quote = MulticloudLinkQuote.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(500).pathTier(1)
                .sideA(PriceQuote.zero(USD, PriceSource.REFERENCE))
                .sideZ(PriceQuote.monthly(new BigDecimal("1100"), EUR, PriceSource.CUSTOM))
                .build();

        assertFalse(quote.isMixedCurrency());
        assertEquals(0, new BigDecimal("1100").compareTo(quote.combinedMonthly().orElseThrow()));
        assertEquals("EUR", quote.combinedCurrency().orElseThrow());
    }

    @Test
    void aSideWithNoCurrencyWithholdsTheCombinedFigure() {
        MulticloudLinkQuote quote = MulticloudLinkQuote.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(10_000).pathTier(1)
                .sideA(PriceQuote.monthly(new BigDecimal("9000.90"), null, PriceSource.CUSTOM))
                .sideZ(PriceQuote.monthly(new BigDecimal("13870.00"), USD, PriceSource.REFERENCE))
                .build();

        assertTrue(quote.combinedMonthly().isEmpty(),
                "an unreconcilable side must not produce a one-sided figure presented as the link price");
    }

    // ── TCO ──

    @Test
    void tcoNativeArchetypeIsUnpricedWhenItsTwoSidesAreInDifferentCurrencies() {
        TcoComparison tco = tco(RateCard.layered(eurGoogleSide(), ReferenceRateCard.standard())).compare();
        CostBreakdown nativeLink = tco.breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow();

        assertFalse(nativeLink.isPriced());
        assertEquals(0, BigDecimal.ZERO.compareTo(nativeLink.getMonthlyTotal()), "no fabricated cross-currency total");
        assertTrue(nativeLink.getNote().contains("USD") && nativeLink.getNote().contains("EUR"), nativeLink.getNote());
        assertTrue(nativeLink.getNote().contains("9000.90") && nativeLink.getNote().contains("12000.00"),
                "per-currency subtotals are reported: " + nativeLink.getNote());
        assertNotEquals(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT, tco.getRecommended());
        assertTrue(tco.toMarkdown().contains("| Native cloud-to-cloud interconnect | _unavailable_ |"));
        // The two line items stay visible for inspection.
        assertEquals(0, new BigDecimal("12000").compareTo(
                nativeLink.getLineItems().get("Native link, GOOGLE_CLOUD side (10000 Mbps)")));
    }

    @Test
    void tcoNativeArchetypePricesInEurWhenBothSidesAreEurDespiteTheUsdZeroPerGbRate() {
        // Both flat fees negotiated in EUR; the per-GB rates still come from the USD reference
        // card and are zero. A zero USD amount must not flip an all-EUR link to "mixed".
        CustomRateCard eurBothSides = CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000"))
                .build();
        CostBreakdown nativeLink = tco(RateCard.layered(eurBothSides, ReferenceRateCard.standard())).compare()
                .breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow();

        assertTrue(nativeLink.isPriced(), String.valueOf(nativeLink.getNote()));
        assertEquals("EUR", nativeLink.getCurrency());
        assertEquals(0, new BigDecimal("20000").compareTo(nativeLink.getMonthlyTotal()));
        assertTrue(nativeLink.getLineItems().containsKey("Data transfer over the native link (none charged)"));
    }

    @Test
    void tcoTwoSidedEquinixArchetypeIsUnpricedWhenConnectionsAndEgressDiffer() {
        RateCard eurConnections = RateCard.layered(
                CustomRateCard.builder().currency("EUR")
                        .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000")).build(),
                ReferenceRateCard.standard());
        TcoComparison tco = tco(eurConnections).compare();
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();

        assertFalse(equinix.isPriced());
        assertEquals(0, BigDecimal.ZERO.compareTo(equinix.getMonthlyTotal()));
        assertTrue(equinix.getNote().contains("EUR 4000.00"), "two EUR 2000 connections: " + equinix.getNote());
        assertTrue(equinix.getNote().contains("USD 3860.00"), "USD egress both ways: " + equinix.getNote());
        assertTrue(tco.breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow().isPriced(),
                "the all-USD native archetype is unaffected");
    }

    @Test
    void tcoTwoSidedInternetArchetypeIsUnpricedWhenTheTwoCloudsEgressRatesDiffer() {
        RateCard eurGoogleEgress = RateCard.layered(
                CustomRateCard.builder().currency("EUR")
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.INTERNET, new BigDecimal("0.10")).build(),
                ReferenceRateCard.standard());
        CostBreakdown internet = tco(eurGoogleEgress).compare()
                .breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow();

        assertFalse(internet.isPriced());
        assertTrue(internet.getNote().contains("USD 9000.00") && internet.getNote().contains("EUR 10000.00"),
                internet.getNote());
    }

    // ── Savings ──

    @Test
    void savingsWithholdsTheNativePathAndTheBreakEvenWhenTheNativeSidesDiffer() {
        MulticloudPathComparison c = savings(RateCard.layered(eurGoogleSide(), ReferenceRateCard.standard()))
                .calculate().getMulticloudComparison();

        assertNull(c.getNativeMonthlyCost(), "USD + EUR native sides are not summed");
        assertNull(c.getBreakEvenSustainedMbps(), "the break-even subtracts fixed costs, so it needs one currency");
        assertNotNull(c.getEquinixMonthlyCost());
        assertEquals("USD", c.getCurrency());
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("Native multicloud path unpriced")
                && n.contains("EUR 12000.00") && n.contains("USD 9000.90")), c.getNotes().toString());
    }

    @Test
    void savingsWithholdsAnAllEurNativePathFromAUsdComparison() {
        CustomRateCard eurBothSides = CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000"))
                .build();
        MulticloudPathComparison c = savings(RateCard.layered(eurBothSides, ReferenceRateCard.standard()))
                .calculate().getMulticloudComparison();

        assertEquals("USD", c.getCurrency(), "the internet and Equinix paths are USD");
        assertNull(c.getNativeMonthlyCost(), "an EUR path is not ranked against USD paths");
        assertNull(c.getBreakEvenSustainedMbps());
        assertEquals(EgressPath.PRIVATE, c.getLowestCostPath());
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("Native multicloud path withheld")
                && n.contains("EUR 20000.00")), c.getNotes().toString());
    }

    @Test
    void savingsAllUsdStillPricesEveryPath() {
        MulticloudPathComparison c = savings(ReferenceRateCard.standard()).calculate().getMulticloudComparison();

        assertNotNull(c.getInternetMonthlyCost());
        assertNotNull(c.getEquinixMonthlyCost());
        assertNotNull(c.getNativeMonthlyCost());
        assertNotNull(c.getBreakEvenSustainedMbps());
    }
}
