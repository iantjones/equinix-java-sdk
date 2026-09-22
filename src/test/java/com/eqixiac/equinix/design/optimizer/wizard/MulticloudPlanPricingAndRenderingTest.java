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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.MulticloudLinkPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Native-link pricing on the plan, its separation from the Equinix totals, repricing, and the
 * summary and Markdown rendering.
 *
 * <p>Expected figures for AWS {@code us-east-1} and Google Cloud {@code us-east4} at 10000 Mbps,
 * path tier 1, from the bundled reference data (retrieved 2026-09-21): AWS 12.33 USD/h x 730 h =
 * 9000.90; Google Cloud North America 19.00 USD/h x 730 h = 13870.00; native total 22870.90. The
 * Equinix path under {@code flatRateCard()} is two connections at 500, the reference AWS 10G port
 * 1642.50, the reference Google Cloud 10G port 1676.00, and the Cloud Router at 300 when the flow
 * is its only use: 4618.50. Per-GB: AWS 0.02 + Google Cloud 0.0186 private; 0 + 0 native.</p>
 */
@DisplayName("Native multicloud links: pricing, repricing and rendering")
class MulticloudPlanPricingAndRenderingTest {

    private static DeploymentWizard.Builder wizard(OptimizationResult result) {
        return DeploymentWizard.builder(null, result)
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard());
    }

    private static DeploymentPlan comparePlan() {
        return wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000)).plan();
    }

    // ── Pricing ──

    @Test
    @DisplayName("the link carries the two-sided native quote, the Equinix-path cost for the same flow and the break-even")
    void linkPricing() {
        MulticloudLinkPricing pricing = comparePlan().getMulticloudLinks().get(0).getPricing();

        assertEquals(new BigDecimal("22870.90"), pricing.getNativeMonthly());
        assertEquals("USD", pricing.getNativeCurrency());
        assertEquals(new BigDecimal("9000.90"),
                pricing.getNativeQuote().side(CloudProviderType.AWS).orElseThrow().getMonthlyRecurring());
        assertEquals(new BigDecimal("13870.00"),
                pricing.getNativeQuote().side(CloudProviderType.GOOGLE_CLOUD).orElseThrow().getMonthlyRecurring());
        assertTrue(pricing.isNativePriced());

        assertEquals(new BigDecimal("4618.50"), pricing.getEquinixFixedMonthly());
        assertEquals("USD", pricing.getEquinixCurrency());
        assertEquals(5, pricing.getEquinixComponents().size(), "two connections, two cloud ports, one Cloud Router");
        assertEquals(new BigDecimal("0.0386"), pricing.getEquinixPerGb());
        assertEquals(0, BigDecimal.ZERO.compareTo(pricing.getNativePerGb()));

        BigDecimal expected = MulticloudPathComparison.breakEvenSustainedMbps(new BigDecimal("22870.90"),
                new BigDecimal("4618.50"), new BigDecimal("0.0386"), BigDecimal.ZERO).orElseThrow();
        assertEquals(expected, pricing.getBreakEvenSustainedMbps(), "the value layer's formula, not a second one");
        assertEquals(new BigDecimal("2878.9"), pricing.getBreakEvenSustainedMbps());
    }

    @Test
    @DisplayName("the recommendation states both fixed costs, the break-even rate and which path costs less on each side of it")
    void recommendationStatesBreakEven() {
        String recommendation = comparePlan().getMulticloudLinks().get(0).getRecommendation();

        assertTrue(recommendation.contains("native USD 22870.90"), recommendation);
        assertTrue(recommendation.contains("Equinix path USD 4618.50 plus 0.0386 USD per GB"), recommendation);
        assertTrue(recommendation.contains("Break-even at 2878.9 Mbps sustained, both directions summed "
                + "(1439.5 Mbps each way"), recommendation);
        assertTrue(recommendation.contains("Below that rate the Equinix path costs less per month; above it the "
                + "native link does."), recommendation);
        assertTrue(recommendation.contains("14% sustained utilization of a 10000 Mbps link"), recommendation);
    }

    @Test
    @DisplayName("the Cloud Router is left out of the Equinix-path cost when a user site also uses it, and a note says so")
    void sharedRouterIsNotAttributed() {
        OptimizationResult withSite = MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-1",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4", 10_000,
                List.of(MulticloudWizardFixtures.site("HQ", MulticloudWizardFixtures.DC)), List.of(), List.of());

        MulticloudLinkPricing pricing = wizard(withSite).plan().getMulticloudLinks().get(0).getPricing();

        assertEquals(new BigDecimal("4318.50"), pricing.getEquinixFixedMonthly(), "4618.50 less the 300 Cloud Router");
        assertEquals(4, pricing.getEquinixComponents().size());
        assertTrue(pricing.getNotes().stream().anyMatch(n -> n.startsWith("Cloud Router FCR-DC is not in the Equinix "
                + "path cost: the request declares user sites") && n.contains("overstates the break-even rate")),
                () -> String.valueOf(pricing.getNotes()));
    }

    @Test
    @DisplayName("an unpublished size leaves the side unpriced: no native figure, no break-even, and the link is listed as unpriced")
    void unpricedSideIsNotZero() {
        // 1000 Mbps: Google publishes 3.50 USD/h; AWS publishes no 1000 Mbps rate.
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.awsGcpAtDc(1000)).plan();
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);

        assertNull(link.getPricing().getNativeMonthly());
        assertNull(link.getPricing().getNativeCurrency());
        assertFalse(link.getPricing().isNativePriced());
        assertTrue(link.getPricing().getNativeQuote().side(CloudProviderType.AWS).isEmpty());
        assertEquals(new BigDecimal("2555.00"),
                link.getPricing().getNativeQuote().side(CloudProviderType.GOOGLE_CLOUD).orElseThrow().getMonthlyRecurring());
        assertNull(link.getPricing().getBreakEvenSustainedMbps());
        assertTrue(link.getPricing().getNotes().stream().anyMatch(n -> n.startsWith("Native link not fully priced: AWS side")));
        assertTrue(link.getRecommendation().startsWith("Cost comparison not possible"), link.getRecommendation());

        PlanPricing pricing = plan.getPricing();
        assertNull(pricing.getNativeAlternativeMonthlyCost(), "withheld, not zero");
        assertEquals(List.of("aws-gcp-DC"), pricing.getUnpricedMulticloudLinks());
        assertTrue(pricing.getPerMulticloudLinkCost().isEmpty());
        assertTrue(plan.toMarkdown().contains("| aws-gcp-DC | AWS us-east-1 <-> GOOGLE_CLOUD us-east4 | GA (as of "
                + "2026-09-21) | 1000 Mbps | unpriced |"));
    }

    @Test
    @DisplayName("a missing reference port figure withholds the Equinix-path cost instead of under-stating it")
    void missingPortFigureWithholdsEquinixCost() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-1",
                CloudProviderType.ORACLE_CLOUD, "OCI", "us-ashburn-1", 3000)).plan();
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);

        assertEquals(5000, link.getCoveringTierMbps(), "3000 Mbps rounds up to Oracle's 5G option");
        assertTrue(link.isRoundedUp());
        assertNull(link.getPricing().getEquinixFixedMonthly());
        assertTrue(link.getPricing().getNotes().contains(
                "Equinix path cost withheld: no reference interconnect-port figure for ORACLE_CLOUD."));
        assertNull(link.getPricing().getNativeMonthly(), "neither side publishes a 5000 Mbps price");
    }

    // ── Separation from the Equinix totals ──

    @Test
    @DisplayName("PlanPricing reports alternatives separately: the Equinix totals equal those of EQUINIX_ONLY")
    void alternativesAreReportedSeparately() {
        PlanPricing compare = comparePlan().getPricing();
        PlanPricing equinixOnly = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan().getPricing();

        assertEquals(new BigDecimal("22870.90"), compare.getNativeAlternativeMonthlyCost());
        assertEquals("USD", compare.getNativeAlternativeCurrency());
        assertEquals(Map.of("aws-gcp-DC", new BigDecimal("22870.90")), compare.getPerMulticloudLinkCost());
        assertTrue(compare.getUnpricedMulticloudLinks().isEmpty());
        assertNull(compare.getNativeReplacementMonthlyCost(), "no link replaces anything under COMPARE");
        assertNotNull(compare.getNativeMulticloudDisclaimer());

        assertEquals(equinixOnly.getMonthlyTotal(), compare.getMonthlyTotal());
        assertEquals(equinixOnly.getSetupTotal(), compare.getSetupTotal());
        assertEquals(equinixOnly.getMonthlyByCurrency(), compare.getMonthlyByCurrency());
        assertEquals(equinixOnly.getProviderConnectionMonthlyCost(), compare.getProviderConnectionMonthlyCost());
        assertEquals(equinixOnly.getPerConnectionCost(), compare.getPerConnectionCost());
        assertEquals(equinixOnly.getDisclaimer(), compare.getDisclaimer());

        assertNull(equinixOnly.getNativeAlternativeMonthlyCost());
        assertNull(equinixOnly.getPerMulticloudLinkCost());
        assertNull(equinixOnly.getUnpricedMulticloudLinks());
        assertNull(equinixOnly.getNativeMulticloudDisclaimer());
    }

    @Test
    @DisplayName("a REPLACEMENT link is reported under nativeReplacement*, and still not added to monthlyTotal")
    void replacementIsReportedSeparately() {
        PlanPricing pricing = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE).plan().getPricing();

        assertEquals(new BigDecimal("22870.90"), pricing.getNativeReplacementMonthlyCost());
        assertEquals("USD", pricing.getNativeReplacementCurrency());
        assertNull(pricing.getNativeAlternativeMonthlyCost());
        assertEquals(0, new BigDecimal("300").compareTo(pricing.getMonthlyTotal()),
                "the Equinix total is the remaining Cloud Router only");
    }

    // ── Currency honesty ──

    @Test
    @DisplayName("sides in different currencies are never summed: the native figure is withheld and the note names both subtotals")
    void mixedCurrencySidesAreNotSummed() {
        RateCard awsInUsd = CustomRateCard.builder().currency("USD")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("9000.90")).build();
        RateCard googleInEur = CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000.00")).build();

        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(RateCard.layered(awsInUsd, googleInEur, MulticloudWizardFixtures.flatRateCard()))
                .plan();
        MulticloudLinkPricing pricing = plan.getMulticloudLinks().get(0).getPricing();

        assertNull(pricing.getNativeMonthly());
        assertNull(pricing.getBreakEvenSustainedMbps());
        assertTrue(pricing.getNotes().stream().anyMatch(n -> n.contains("different currencies")
                && n.contains("USD 9000.90") && n.contains("EUR 12000.00") && n.contains("no FX rate is applied")),
                () -> String.valueOf(pricing.getNotes()));
        assertNull(plan.getPricing().getNativeAlternativeMonthlyCost());
        assertEquals(List.of("aws-gcp-DC"), plan.getPricing().getUnpricedMulticloudLinks());

        String markdown = plan.toMarkdown();
        assertTrue(markdown.contains("Native link, AWS side: $9000.90 per month (CUSTOM)"), markdown);
        assertTrue(markdown.contains("Native link, GOOGLE_CLOUD side: €12000.00 per month (CUSTOM)"), markdown);
    }

    @Test
    @DisplayName("a native link priced in EUR renders with the euro symbol next to a USD Equinix path, and no break-even is formed")
    void eurNativeLinkRendersInItsOwnCurrency() {
        RateCard eur = CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000.00"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000.00")).build();

        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(RateCard.layered(eur, MulticloudWizardFixtures.flatRateCard()))
                .plan();
        MulticloudLinkPricing pricing = plan.getMulticloudLinks().get(0).getPricing();

        assertEquals(new BigDecimal("20000.00"), pricing.getNativeMonthly());
        assertEquals("EUR", pricing.getNativeCurrency());
        assertEquals("USD", pricing.getEquinixCurrency());
        assertNull(pricing.getBreakEvenSustainedMbps(), "EUR and USD are not compared");
        assertTrue(plan.toMarkdown().contains("| €20000.00 | $4618.50 | n/a | ALTERNATIVE |"), plan.toMarkdown());
        assertEquals("EUR", plan.getPricing().getNativeAlternativeCurrency());
        assertEquals("USD", plan.getPricing().getCurrency(), "the Equinix total keeps its own currency");
    }

    // ── reprice ──

    @Test
    @DisplayName("reprice() refreshes the native-link pricing and recommendation against the builder's rate card")
    void repriceRefreshesNativeLinkPricing() {
        DeploymentPlan plan = comparePlan();

        RateCard negotiated = CustomRateCard.builder().currency("USD")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("7000.00"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("11000.00")).build();
        DeploymentWizard.Builder repricer = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .rateCard(RateCard.layered(negotiated, MulticloudWizardFixtures.flatRateCard()));

        DeploymentPlan repriced = repricer.reprice(plan);

        PlannedMulticloudInterconnect link = repriced.getMulticloudLinks().get(0);
        assertEquals(new BigDecimal("18000.00"), link.getPricing().getNativeMonthly());
        assertTrue(link.getRecommendation().contains("native USD 18000.00"), link.getRecommendation());
        assertEquals(new BigDecimal("18000.00"), repriced.getPricing().getNativeAlternativeMonthlyCost());
        assertEquals(plan.getPricing().getMonthlyTotal(), repriced.getPricing().getMonthlyTotal());
        // What repricing does not re-plan.
        assertEquals(plan.getMulticloudLinks().get(0).getEnvironment(), link.getEnvironment());
        assertEquals(plan.getMulticloudLinks().get(0).getRole(), link.getRole());
        assertEquals(plan.getMulticloudLinks().get(0).getReasoning(), link.getReasoning());
        assertEquals(new BigDecimal("22870.90"), plan.getMulticloudLinks().get(0).getPricing().getNativeMonthly(),
                "the input plan is not mutated");
    }

    @Test
    @DisplayName("reprice() with the planning configuration reproduces the plan's figures")
    void repriceIsIdempotent() {
        DeploymentWizard.Builder builder = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000));
        DeploymentPlan plan = builder.plan();

        DeploymentPlan repriced = builder.reprice(plan);

        assertEquals(plan.getMulticloudLinks(), repriced.getMulticloudLinks());
        assertEquals(plan.getPricing(), repriced.getPricing());
    }

    @Test
    @DisplayName("reprice() of a REPLACEMENT plan still prices the omitted Equinix connections for the comparison")
    void repriceKeepsOmittedLegs() {
        DeploymentWizard.Builder builder = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);
        DeploymentPlan plan = builder.plan();

        DeploymentPlan repriced = builder.reprice(plan);

        MulticloudLinkPricing pricing = repriced.getMulticloudLinks().get(0).getPricing();
        assertEquals(new BigDecimal("4618.50"), pricing.getEquinixFixedMonthly());
        assertTrue(pricing.getEquinixComponents().get(0).contains("FCR-DC-to-aws, omitted from the plan"));
        assertEquals(new BigDecimal("22870.90"), repriced.getPricing().getNativeReplacementMonthlyCost());
    }

    @Test
    @DisplayName("per-GB rates in another currency withhold the break-even and the recommendation says so, never that one path wins at every volume")
    void perGbRatesInAnotherCurrencyGiveNoConclusion() {
        // Regression: with EUR per-GB rates the break-even was withheld, but recommend() then fell
        // through to "the Equinix path's per-GB rate does not exceed the native link's ... costs
        // less at every volume" while 0.04 EUR > 0 and no comparison is possible.
        RateCard eurPerGb = CustomRateCard.builder().currency("EUR")
                .egressRate(CloudProviderType.AWS, com.eqixiac.equinix.design.value.ratecard.EgressPath.PRIVATE,
                        new BigDecimal("0.02"))
                .egressRate(CloudProviderType.GOOGLE_CLOUD, com.eqixiac.equinix.design.value.ratecard.EgressPath.PRIVATE,
                        new BigDecimal("0.02"))
                .egressRate(CloudProviderType.AWS,
                        com.eqixiac.equinix.design.value.ratecard.EgressPath.MULTICLOUD_INTERCONNECT, BigDecimal.ZERO)
                .egressRate(CloudProviderType.GOOGLE_CLOUD,
                        com.eqixiac.equinix.design.value.ratecard.EgressPath.MULTICLOUD_INTERCONNECT, BigDecimal.ZERO)
                .build();
        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(eurPerGb)
                .plan();
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        MulticloudLinkPricing pricing = link.getPricing();

        assertEquals(new BigDecimal("22870.90"), pricing.getNativeMonthly());
        assertNotNull(pricing.getEquinixFixedMonthly());
        assertEquals("USD", pricing.getEquinixCurrency());
        assertEquals(new BigDecimal("0.04"), pricing.getEquinixPerGb());
        assertEquals("EUR", pricing.getPerGbCurrency());
        assertNull(pricing.getBreakEvenSustainedMbps());
        assertTrue(pricing.getNotes().stream().anyMatch(n -> n.startsWith("Break-even not computed")
                && n.contains("per-GB rates (EUR)")), () -> String.valueOf(pricing.getNotes()));
        assertTrue(link.getRecommendation().contains("The fixed costs (USD, USD) and the per-GB rates (EUR) are not in "
                + "one currency; no comparison is made"), link.getRecommendation());
        assertFalse(link.getRecommendation().contains("at every volume"), link.getRecommendation());
        assertTrue(plan.toMarkdown().contains("no comparison is made and no FX rate is applied"), plan.toMarkdown());
    }

    @Test
    @DisplayName("a reversed crossing (native fee lower, native per-GB higher) is a break-even with the Equinix path cheaper above it")
    void reversedCrossingIsStatedWithItsSense() {
        // Native 2,000 flat + 0.10 USD/GB; Equinix 4,618.50 flat + 0.0386 USD/GB. The lines cross at
        // (4,618.50 - 2,000) / (0.10 - 0.0386) = 42,646.6 GB each way = 259.6 Mbps summed; above
        // it the Equinix path costs less. The previous text asserted that the native link costs
        // the same or less at every volume.
        RateCard reversed = RateCard.layered(CustomRateCard.builder().currency("USD")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("1000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("1000"))
                .egressRate(CloudProviderType.AWS,
                        com.eqixiac.equinix.design.value.ratecard.EgressPath.MULTICLOUD_INTERCONNECT, new BigDecimal("0.05"))
                .egressRate(CloudProviderType.GOOGLE_CLOUD,
                        com.eqixiac.equinix.design.value.ratecard.EgressPath.MULTICLOUD_INTERCONNECT, new BigDecimal("0.05"))
                .build(), MulticloudWizardFixtures.flatRateCard());
        PlannedMulticloudInterconnect link = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com").rateCard(reversed).plan().getMulticloudLinks().get(0);
        MulticloudLinkPricing pricing = link.getPricing();

        assertEquals(new BigDecimal("2000.00"), pricing.getNativeMonthly());
        assertEquals(new BigDecimal("4618.50"), pricing.getEquinixFixedMonthly());
        assertEquals(new BigDecimal("259.6"), pricing.getBreakEvenSustainedMbps());
        assertFalse(pricing.isNativeCheaperAboveBreakEven());
        assertTrue(link.getRecommendation().contains("Below that rate the native link costs less per month; above it "
                + "the Equinix path does."), link.getRecommendation());
        assertFalse(link.getRecommendation().contains("at every volume"), link.getRecommendation());

        // The usual case keeps its sense, and a true no-crossing case states both differences.
        assertTrue(comparePlan().getMulticloudLinks().get(0).getPricing().isNativeCheaperAboveBreakEven());
        RateCard cheapNative = RateCard.layered(CustomRateCard.builder().currency("USD")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("1000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("1000"))
                .build(), MulticloudWizardFixtures.flatRateCard());
        String dominated = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com").rateCard(cheapNative).plan().getMulticloudLinks().get(0)
                .getRecommendation();
        assertTrue(dominated.contains("does not exceed the Equinix path's fixed cost")
                && dominated.contains("its per-GB rate does not exceed the Equinix path's")
                && dominated.contains("costs the same or less at every volume"), dominated);
    }

    @Test
    @DisplayName("plan() and reprice() with a gateway and no rate card fetch the live price catalogue once, not once per pricing step")
    void oneLiveRateCardInstancePricesLinksAndPlan() {
        // Regression: MulticloudLinkPlanner.price() resolved its own card, so a wizard with a
        // gateway and no explicit card built two live EquinixRateCards, fetched the catalogue
        // twice per plan() (and per reprice()), and could price one connection from two fetch
        // outcomes. A successful EquinixRateCard fetch is one prices().list(...) call per priced
        // product type (VIRTUAL_CONNECTION_PRODUCT and CLOUD_ROUTER_PRODUCT), cached per instance.
        com.eqixiac.equinix.FabricGateway fabric = org.mockito.Mockito.mock(com.eqixiac.equinix.FabricGateway.class);
        com.eqixiac.equinix.fabric.client.Prices prices = org.mockito.Mockito.mock(com.eqixiac.equinix.fabric.client.Prices.class);
        org.mockito.Mockito.when(fabric.prices()).thenReturn(prices);
        // null: a genuine empty result, which the card treats as a complete fetch and caches.
        org.mockito.Mockito.when(prices.list(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeploymentWizard.Builder builder = DeploymentWizard.builder(fabric, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com");
        DeploymentPlan plan = builder.plan();

        org.mockito.Mockito.verify(prices, org.mockito.Mockito.times(2)).list(org.mockito.ArgumentMatchers.any());
        // The empty live catalogue makes every Equinix figure the heuristic, on the link and on the plan alike.
        assertTrue(plan.getMulticloudLinks().get(0).getPricing().getEquinixComponents().get(0)
                .contains("USD 5000.00 per month, ESTIMATE"), () -> plan.getMulticloudLinks().get(0).getPricing()
                .getEquinixComponents().toString());
        assertEquals(0, new BigDecimal("5000").compareTo(plan.getPricing().getPerConnectionCost().get("FCR-DC-to-aws")));

        builder.reprice(plan);
        org.mockito.Mockito.verify(prices, org.mockito.Mockito.times(4)).list(org.mockito.ArgumentMatchers.any());
    }

    // ── valueRealization() on a plan that depends on a native link ──

    @Test
    @DisplayName("valueRealization() nets a REPLACEMENT link's fee and prices native-only clouds at the link's per-GB rate")
    void valueRealizationNetsTheNativeLinkFee() {
        // Regression: the net used pricing.getMonthlyTotal() only (USD 300, the remaining router),
        // so a plan paying 22,870.90/month for the link it depends on reported a HIGHER net
        // (16,020) than the EQUINIX_ONLY plan (15,020), and credited Fabric PRIVATE rates for
        // clouds the plan no longer reaches through Fabric.
        DeploymentPlan replacement = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE).plan();
        assertTrue(replacement.getProviderConnections().isEmpty());
        assertEquals(new BigDecimal("22870.90"), replacement.getPricing().getNativeReplacementMonthlyCost());

        com.eqixiac.equinix.design.optimizer.wizard.model.PlanValueRealization vr = replacement.valueRealization()
                .egressTerabytes(CloudProviderType.AWS, 100)
                .egressTerabytes(CloudProviderType.GOOGLE_CLOUD, 100)
                .assess();

        assertEquals(new BigDecimal("22870.90"), vr.getNativeLinkMonthlyCost());
        assertEquals("USD", vr.getNativeLinkCurrency());
        assertFalse(vr.isNativeLinkUnpriced());
        // Both clouds are reached only through the link: egress priced at the link's per-GB rate (0).
        for (var row : vr.getPerProvider()) {
            assertEquals(com.eqixiac.equinix.design.value.ratecard.EgressPath.MULTICLOUD_INTERCONNECT, row.getPath());
            assertTrue(row.isPriced());
            assertEquals(0, BigDecimal.ZERO.compareTo(row.getPrivateMonthlyCost()));
        }
        // Savings 9,000 (AWS 0.09/GB) + 11,180 (Google Cloud 0.1118/GB) = 20,180; net = 20,180 - 300 - 22,870.90.
        assertEquals(0, new BigDecimal("20180").compareTo(vr.getTotalMonthlyEgressSavings()));
        assertEquals(0, new BigDecimal("300").compareTo(vr.getPlanMonthlyCost()));
        assertEquals(0, new BigDecimal("-2990.90").compareTo(vr.getNetMonthlySavings()));
        assertEquals(0, new BigDecimal("-35890.80").compareTo(vr.getAnnualNetSavings()));
        assertTrue(vr.getDisclaimer().contains("depends on native multicloud link(s) [aws-gcp-DC]"), vr.getDisclaimer());
        String markdown = vr.toMarkdown();
        assertTrue(markdown.contains("| AWS (native link) | 100000 |"), markdown);
        assertTrue(markdown.contains("Native multicloud link fees the plan depends on (billed by the cloud providers): "
                + "−USD 22870.90/mo"), markdown);

        // The EQUINIX_ONLY plan of the same workload: Fabric PRIVATE rates, no native fee, net 15,020.
        com.eqixiac.equinix.design.optimizer.wizard.model.PlanValueRealization equinixOnly =
                wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                        .cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan().valueRealization()
                        .egressTerabytes(CloudProviderType.AWS, 100)
                        .egressTerabytes(CloudProviderType.GOOGLE_CLOUD, 100)
                        .assess();
        assertNull(equinixOnly.getNativeLinkMonthlyCost());
        assertEquals(com.eqixiac.equinix.design.value.ratecard.EgressPath.PRIVATE, equinixOnly.getPerProvider().get(0).getPath());
        assertEquals(0, new BigDecimal("15020").compareTo(equinixOnly.getNetMonthlySavings()));
        assertTrue(equinixOnly.getNetMonthlySavings().compareTo(vr.getNetMonthlySavings()) > 0,
                "the deployment that pays the link fee never reports the higher net");

        // COMPARE: the link is an ALTERNATIVE, the Fabric connections stay, nothing changes.
        com.eqixiac.equinix.design.optimizer.wizard.model.PlanValueRealization compare = comparePlan().valueRealization()
                .egressTerabytes(CloudProviderType.AWS, 100)
                .egressTerabytes(CloudProviderType.GOOGLE_CLOUD, 100)
                .assess();
        assertNull(compare.getNativeLinkMonthlyCost());
        assertEquals(equinixOnly.getNetMonthlySavings(), compare.getNetMonthlySavings());
    }

    @Test
    @DisplayName("valueRealization() withholds the net when a REPLACEMENT link's fee is unpriced, never treating it as zero")
    void valueRealizationWithholdsTheNetForAnUnpricedReplacementLink() {
        // A REPLACEMENT plan whose native fee cannot be formed: the two sides are in different currencies.
        RateCard awsInUsd = CustomRateCard.builder().currency("USD")
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("9000.90")).build();
        RateCard googleInEur = CustomRateCard.builder().currency("EUR")
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000.00")).build();
        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(RateCard.layered(awsInUsd, googleInEur, MulticloudWizardFixtures.flatRateCard()))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE)
                .plan();
        assertTrue(plan.getMulticloudLinks().get(0).replacesEquinixConnections(),
                "the replacement rule runs before pricing, so the link is a REPLACEMENT whatever its price");
        assertNull(plan.getPricing().getNativeReplacementMonthlyCost());

        com.eqixiac.equinix.design.optimizer.wizard.model.PlanValueRealization vr = plan.valueRealization()
                .egressTerabytes(CloudProviderType.AWS, 100)
                .assess();

        assertTrue(vr.isNativeLinkUnpriced());
        assertNull(vr.getNativeLinkMonthlyCost());
        assertNull(vr.getTotalMonthlyEgressSavings());
        assertNull(vr.getNetMonthlySavings());
        assertNull(vr.getAnnualNetSavings());
        assertNull(vr.getFirstYearNetSavings());
        assertTrue(vr.getPerProvider().get(0).isPriced(), "the per-provider row stays valid");
        assertTrue(vr.getDisclaimer().contains("unpriced (aws-gcp-DC)")
                && vr.getDisclaimer().contains("rather than computed as if that fee were zero"), vr.getDisclaimer());
        assertTrue(vr.toMarkdown().contains("Native multicloud link fees the plan depends on: n/a (unpriced"), vr.toMarkdown());
        assertTrue(vr.toMarkdown().contains("**Net monthly saving:** n/a"), vr.toMarkdown());
    }

    @Test
    @DisplayName("multicloudPathTier is an input: tier 4 prices the AWS side at the published 51.78 USD/h")
    void pathTierIsAnInput() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000)).multicloudPathTier(4).plan();

        assertEquals(new BigDecimal("37799.40"), plan.getMulticloudLinks().get(0).getPricing()
                .getNativeQuote().side(CloudProviderType.AWS).orElseThrow().getMonthlyRecurring());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000)).multicloudPathTier(6));
    }

    // ── Rendering ──

    @Test
    @DisplayName("toMarkdown() renders a 'Native multicloud alternatives' section with the table row, recommendation, sources and steps")
    void markdownSection() {
        String markdown = comparePlan().toMarkdown();

        assertTrue(markdown.contains("## Native multicloud alternatives\n"), markdown);
        assertTrue(markdown.contains("| aws-gcp-DC | AWS us-east-1 <-> GOOGLE_CLOUD us-east4 | GA (as of 2026-09-21) "
                + "| 10000 Mbps | $22870.90 | $4618.50 | 2878.9 Mbps | ALTERNATIVE |"), markdown);
        assertTrue(markdown.contains("- **aws-gcp-DC**: Fixed monthly cost: native USD 22870.90"), markdown);
        assertTrue(markdown.contains("  - Workloads: Replication"), markdown);
        assertTrue(markdown.contains("  - Native link, AWS side: $9000.90 per month (REFERENCE)"), markdown);
        assertTrue(markdown.contains("https://aws.amazon.com/interconnect/multicloud/pricing/"), markdown);
        assertTrue(markdown.contains("  - Environment source: https://docs.aws.amazon.com/interconnect/latest/userguide/"
                + "region-availability.html"), markdown);
        assertTrue(markdown.contains("Create-then-accept steps (performed by the customer, outside Fabric)"), markdown);
        assertTrue(markdown.contains("--activation-key"), markdown);
        assertTrue(markdown.indexOf("## Native multicloud alternatives") < markdown.indexOf("## Cost Estimate"));
        assertTrue(markdown.contains("_Native multicloud link charges are billed by the cloud providers and are not "
                + "included in the table above; see Native multicloud alternatives._"), markdown);
        assertTrue(markdown.contains("| **Total Monthly** | **$1300** |"), "the Equinix total is unchanged");
        assertTrue(markdown.contains("`plan.execute()` does not create the 1 native multicloud link(s)"), markdown);
    }

    @Test
    @DisplayName("totalResourceCount() excludes native links, and the summary and Markdown say so next to the figure")
    void resourceCountExcludesLinks() {
        DeploymentPlan plan = comparePlan();

        assertEquals(7, plan.totalResourceCount(), "1 Cloud Router + 2 connections + 4 routing protocols");
        assertTrue(plan.toSummary().contains("Total resources: 7. Native multicloud link(s): 1 (not provisioned by "
                + "this SDK; excluded from the resource count and from the cost below)."), plan.toSummary());
        assertTrue(plan.toMarkdown().contains("**Total Resources:** 7 (excludes 1 native multicloud link(s), which "
                + "this SDK does not create)"), plan.toMarkdown());
    }

    @Test
    @DisplayName("an UNAVAILABLE entry renders as 'none in catalog' and unpriced")
    void unavailableEntryRenders() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-2",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-central1", 10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_ONLY).plan();

        assertTrue(plan.toMarkdown().contains("| aws-gcp-DC | AWS us-east-2 <-> GOOGLE_CLOUD us-central1 | none in "
                + "catalog | 10000 Mbps (no covering size) | unpriced | unpriced | n/a | UNAVAILABLE |"), plan.toMarkdown());
        assertTrue(plan.toMarkdown().contains("> **VALIDATION ERRORS**"));
    }

    @Test
    @DisplayName("a plan without links renders no native section, note or count")
    void noLinksNoRendering() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan();

        assertFalse(plan.toMarkdown().toLowerCase(java.util.Locale.ROOT).contains("multicloud"));
        assertFalse(plan.toSummary().toLowerCase(java.util.Locale.ROOT).contains("multicloud"));
    }
}
