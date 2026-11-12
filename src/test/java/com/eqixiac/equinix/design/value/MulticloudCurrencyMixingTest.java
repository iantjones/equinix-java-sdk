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
        return TcoCalculator.builder(null).defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none())
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(card);
    }

    private static SavingsCalculator.Builder savings(RateCard card) {
        return SavingsCalculator.builder(null).defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none())
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(card);
    }

    // MulticloudLinkQuote

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

    // TCO

    // Savings

    @Test
    void savingsAllUsdStillPricesEveryPath() {
        MulticloudPathComparison c = savings(ReferenceRateCard.standard()).calculate().getMulticloudComparison();

        assertNotNull(c.getInternetMonthlyCost());
        assertNotNull(c.getEquinixMonthlyCost());
        assertNotNull(c.getNativeMonthlyCost());
        assertNotNull(c.getBreakEvenSustainedMbps());
    }
}
