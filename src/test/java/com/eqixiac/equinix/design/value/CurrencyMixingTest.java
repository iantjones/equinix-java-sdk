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
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanValueRealization;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The systemic currency-mixing (C9) guard across the value-realization cost models: when the
 * components of a total are priced in different currencies - which the real default chain genuinely
 * produces (USD cloud-egress reference figures alongside a live EUR Fabric connection for an EMEA
 * metro) - the models must refuse to emit a single (wrong) number and instead mark the total
 * mixed/unpriced with an explicit reason, while all-one-currency inputs still price normally.
 *
 * <p>Mixed currencies are constructed deterministically by layering two single-currency
 * {@link CustomRateCard}s (no network, no FX rate anywhere in the SDK).</p>
 */
class CurrencyMixingTest {

    /** A USD card that prices only cloud egress (AWS internet $0.09/GB, private $0.02/GB). */
    private static CustomRateCard usdEgressCard() {
        return CustomRateCard.builder()
                .currency("USD")
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .build();
    }

    /** A EUR card that prices only the Equinix connection (10 Gbps @ €2000/mo). */
    private static CustomRateCard eurConnectionCard() {
        return CustomRateCard.builder()
                .currency("EUR")
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000"))
                .build();
    }

    // TCO

    @Test
    void tcoAllUsdStillPricesTheEquinixArchetypeNormally() {
        // Same shape, but connection is USD too: the archetype prices as before (2000 egress + 2000
        // connection = 4000, no reference figures on a pure custom card).
        RateCard allUsd = RateCard.layered(
                CustomRateCard.builder().currency("USD")
                        .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000")).build(),
                usdEgressCard());

        TcoComparison tco = TcoCalculator.builder(null).defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none())
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.EQUINIX_INTERCONNECT)
                .rateCard(allUsd)
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertTrue(equinix.isPriced(), "all-USD components price normally");
        assertEquals(0, new BigDecimal("2000").compareTo(equinix.getLineItems().get("Equinix Fabric connection")),
                "the custom USD connection price");
        // 2000 private egress + 2000 connection + 1810.40 reference AWS hosted connection + 300 reference cross-connect
        // (the bundled USD reference figures fold in because the reconciled currency is USD).
        assertEquals(0, new BigDecimal("6110.40").compareTo(equinix.getMonthlyTotal()));
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended(), "cheaper than 9000 internet");
    }

    @Test
    void tcoConsumesPowerPerKwColocationPrimitive() {
        // POWER_PER_KW (previously never consumed) is priced per kW/mo; at the default 5 kW draw a
        // €.../$150 per-kW rate must appear as a 'Colocation power' line = 150 × 5 = 750.
        CustomRateCard colo = CustomRateCard.builder()
                .colocationRate(ColocationItem.POWER_PER_KW, new BigDecimal("150.00"))
                .build();
        TcoComparison tco = TcoCalculator.builder(null).defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none())
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(RateCard.layered(colo, ReferenceRateCard.standard()))
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertTrue(equinix.isPriced());
        assertTrue(equinix.getLineItems().containsKey("Colocation power (5.0 kW)"),
                "POWER_PER_KW must be consumed: " + equinix.getLineItems().keySet());
        assertEquals(0, new BigDecimal("750").compareTo(equinix.getLineItems().get("Colocation power (5.0 kW)")),
                "150/kW × 5 kW = 750");
        // Reference baseline (4460.40) + the 750 power line = 5210.40.
        assertEquals(0, new BigDecimal("5210.40").compareTo(equinix.getMonthlyTotal()));
    }

    // Savings

    @Test
    void savingsComputesNetAndBreakEvenWhenAllOneCurrency() {
        RateCard allUsd = RateCard.layered(
                CustomRateCard.builder().currency("USD")
                        .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000")).build(),
                usdEgressCard());

        SavingsEstimate s = SavingsCalculator.builder(null).defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none())
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(allUsd)
                .calculate();

        assertTrue(s.isComplete());
        assertEquals(0, new BigDecimal("-310.40").compareTo(s.getNetMonthlySavings()), "3500 - 2000 - 1810.40 AWS 10G hosted connection (2.48 USD/h x 730 h)");
        assertNotNull(s.getBreakEvenGbPerMonth(), "break-even is computable within one currency");
        assertEquals("USD", s.getCurrency());
    }

    // Plan value realization

    private static DeploymentPlan usdPlan(String monthly, String setup) {
        return DeploymentPlan.builder()
                .pricing(PlanPricing.builder()
                        .monthlyTotal(new BigDecimal(monthly))
                        .setupTotal(new BigDecimal(setup))
                        .currency("USD")
                        .build())
                .fabric(null)
                .build();
    }

    @Test
    void planValueOmitsNetWhenEgressCurrencyDiffersFromPlanCurrency() {
        // Plan interconnect cost is USD; the egress rate card quotes EUR.
        CustomRateCard eurEgress = CustomRateCard.builder()
                .currency("EUR")
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .build();

        PlanValueRealization vr = usdPlan("700", "1000").valueRealization()
                .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)
                .rateCard(eurEgress)
                .assess();

        assertNull(vr.getTotalMonthlyEgressSavings(), "EUR egress vs USD plan: no cross-currency total");
        assertNull(vr.getNetMonthlySavings());
        assertNull(vr.getAnnualNetSavings());
        assertNull(vr.getFirstYearNetSavings());
        assertEquals(1, vr.getPerProvider().size());
        assertTrue(vr.getPerProvider().get(0).isPriced());
        assertEquals("EUR", vr.getPerProvider().get(0).getCurrency(), "the provider row keeps its own currency");
        assertEquals(0, new BigDecimal("700").compareTo(vr.getPlanMonthlyCost()));
        assertTrue(vr.getDisclaimer().contains("EUR") && vr.getDisclaimer().contains("USD"),
                "the disclaimer names both currencies: " + vr.getDisclaimer());
        assertNotNull(vr.toMarkdown());
    }

    @Test
    void planValueOmitsTotalWhenProvidersSpanCurrencies() {
        // AWS priced in USD, GOOGLE_CLOUD priced in EUR - the per-provider savings cannot be summed.
        RateCard mixedProviders = RateCard.layered(
                CustomRateCard.builder().currency("USD")
                        .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                        .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                        .build(),
                CustomRateCard.builder().currency("EUR")
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.INTERNET, new BigDecimal("0.12"))
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.PRIVATE, new BigDecimal("0.02"))
                        .build());

        PlanValueRealization vr = usdPlan("1000", "0").valueRealization()
                .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)
                .egress(CloudProviderType.GOOGLE_CLOUD, 10, DataUnit.TERABYTE)
                .rateCard(mixedProviders)
                .assess();

        assertNull(vr.getTotalMonthlyEgressSavings(), "USD + EUR provider savings cannot be summed");
        assertNull(vr.getNetMonthlySavings());
        assertEquals(2, vr.getPerProvider().size());
        assertTrue(vr.getPerProvider().stream().anyMatch(p -> "USD".equals(p.getCurrency())));
        assertTrue(vr.getPerProvider().stream().anyMatch(p -> "EUR".equals(p.getCurrency())));
        assertTrue(vr.getDisclaimer().contains("multiple currencies"), vr.getDisclaimer());
    }

    // CurrencyReconciler unit behaviour

    @Test
    void reconcilerRefusesToTotalMixedCurrenciesButSumsWithinOne() {
        CurrencyReconciler mixed = CurrencyReconciler.create()
                .add("USD", new BigDecimal("100"), BigDecimal.ZERO)
                .add("EUR", new BigDecimal("200"), BigDecimal.ZERO);
        assertTrue(mixed.isMixed());
        assertTrue(mixed.monthlyTotal().isEmpty(), "no total across currencies");
        assertNull(mixed.soleCurrency());
        assertEquals(2, mixed.monthlySubtotals().size());
        assertTrue(mixed.describeCurrencies().contains("USD") && mixed.describeCurrencies().contains("EUR"));

        CurrencyReconciler oneCurrency = CurrencyReconciler.create()
                .add("USD", new BigDecimal("100"), new BigDecimal("10"))
                .add("USD", new BigDecimal("50"), new BigDecimal("5"));
        assertFalse(oneCurrency.isMixed());
        assertEquals(0, new BigDecimal("150").compareTo(oneCurrency.monthlyTotal().orElseThrow()));
        assertEquals(0, new BigDecimal("15").compareTo(oneCurrency.setupTotal().orElseThrow()));
        assertEquals("USD", oneCurrency.soleCurrency());
    }

    @Test
    void reconcilerKnownDifferentTreatsUnknownAsNotProvablyDifferent() {
        assertTrue(CurrencyReconciler.knownDifferent("USD", "EUR"));
        assertFalse(CurrencyReconciler.knownDifferent("USD", "USD"));
        assertFalse(CurrencyReconciler.knownDifferent(null, "USD"), "an absent currency is not a mismatch");
        assertFalse(CurrencyReconciler.knownDifferent("USD", null));
    }
}
