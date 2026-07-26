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

package api.equinix.javasdk.design.value;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import api.equinix.javasdk.design.value.ratecard.CustomRateCard;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.tco.CostBreakdown;
import api.equinix.javasdk.design.value.tco.DeploymentArchetype;
import api.equinix.javasdk.design.value.tco.TcoCalculator;
import api.equinix.javasdk.design.value.tco.TcoComparison;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the 3-archetype {@link TcoCalculator} using the bundled reference card
 * (null Fabric). Scenario: 100 TB/mo egress from AWS, 10 Gbps, metro DC.
 *
 * <p>Expected (reference 2026-06):
 * public-internet = 0.09 × 100,000 = 9000; on-prem = transit 7500 + hw 300 +
 * xconn 250 + power 975 = 9025; Equinix = private egress 2000 + VC 350 +
 * DX port 1642.50 + cross-connect 300 = 4292.50.</p>
 */
class TcoComparisonTest {

    private TcoComparison run() {
        return TcoCalculator.builder(null) // default reference rate card (Fabric unused)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .compare();
    }

    @Test
    void comparesAllThreeArchetypes() {
        TcoComparison tco = run();
        assertEquals(3, tco.getBreakdowns().size());
        assertTrue(tco.getBreakdowns().stream().allMatch(CostBreakdown::isPriced));

        assertEquals(0, new BigDecimal("9000").compareTo(
                tco.breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow().getMonthlyTotal()));
        assertEquals(0, new BigDecimal("9025").compareTo(
                tco.breakdown(DeploymentArchetype.ON_PREM).orElseThrow().getMonthlyTotal()));
        assertEquals(0, new BigDecimal("4292.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
    }

    @Test
    void recommendsEquinixAndComputesSavings() {
        TcoComparison tco = run();
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
        assertEquals(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, tco.getBaseline());
        // 9000 − 4292.50 = 4707.50/mo; × 12 = 56490
        assertEquals(0, new BigDecimal("4707.50").compareTo(tco.getMonthlySavingsVsBaseline()));
        assertEquals(0, new BigDecimal("56490.00").compareTo(tco.getAnnualSavingsVsBaseline()));
        assertTrue(tco.toMarkdown().contains("recommended"));
    }

    @Test
    void honoursArchetypeSubsetAndOnPremOverride() {
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.EQUINIX_INTERCONNECT)
                .onPremTransitPerMbpsMonth(new BigDecimal("2.00")) // ignored: on-prem not requested
                .compare();

        assertEquals(2, tco.getBreakdowns().size());
        assertTrue(tco.breakdown(DeploymentArchetype.ON_PREM).isEmpty());
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
    }

    @Test
    void includesCloudRouterInInterconnectArchetype() {
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .includeCloudRouter("STANDARD")
                .compare();

        // Equinix monthly gains the reference FCR price (1200): 4292.50 + 1200 = 5492.50.
        assertEquals(0, new BigDecimal("5492.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended(), "still cheapest vs 9000 baseline");
    }

    @Test
    void unpriceableProviderYieldsUnavailableArchetypesAndNullSavings() {
        // ORACLE_CLOUD has no egress rate in the bundled reference card.
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.ORACLE_CLOUD)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .compare();

        CostBreakdown internet = tco.breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow();
        assertFalse(internet.isPriced());
        assertEquals(0, BigDecimal.ZERO.compareTo(internet.getMonthlyTotal()), "unpriced => zero, surfaced as unavailable");
        assertFalse(tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().isPriced(),
                "no private egress rate => unpriced");
        assertNull(tco.getMonthlySavingsVsBaseline(), "baseline unpriced => savings null, not bogus");
        assertNull(tco.getAnnualSavingsVsBaseline());
        // On-prem prices purely from reference midpoints (provider-independent), so it remains the pick.
        assertEquals(DeploymentArchetype.ON_PREM, tco.getRecommended());
        assertTrue(tco.toMarkdown().contains("_unavailable_"));
    }

    @Test
    void additionalLineItemsFoldedIntoEveryPricedArchetype() {
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .additionalLineItem("Compute", new BigDecimal("5000"))
                .compare();

        // Each priced archetype gains the $5000 line: internet 9000->14000, Equinix 4292.50->9292.50.
        assertEquals(0, new BigDecimal("14000").compareTo(
                tco.breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow().getMonthlyTotal()));
        assertEquals(0, new BigDecimal("9292.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
        assertTrue(tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow()
                .getLineItems().containsKey("Compute"));
        // Applied uniformly, so the recommendation is unchanged.
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
    }

    @Test
    void colocationCrossConnectFromRateCardOverridesReference() {
        // A caller-supplied cross-connect ($100) layered over the reference card replaces the
        // reference's $300 cross-connect in the Equinix archetype: 4292.50 - 300 + 100 = 4092.50.
        CustomRateCard colo = CustomRateCard.builder()
                .colocationRate(ColocationItem.CROSS_CONNECT, new BigDecimal("100.00"))
                .build();
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(RateCard.layered(colo, ReferenceRateCard.standard()))
                .compare();

        assertEquals(0, new BigDecimal("4092.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
    }

    @Test
    void termFlowsThroughToTheEquinixConnectionRate() {
        // Distinct 12- and 36-month rates on a custom card layered over the reference card
        // (which supplies egress + the other Equinix line items): term() must pick the rate.
        CustomRateCard termed = CustomRateCard.builder()
                .connectionRate(api.equinix.javasdk.fabric.enums.ConnectionType.EVPL_VC, 10_000, MetroCode.DC,
                        api.equinix.javasdk.design.value.ratecard.Term.MONTH_12, new BigDecimal("2000"))
                .connectionRate(api.equinix.javasdk.fabric.enums.ConnectionType.EVPL_VC, 10_000, MetroCode.DC,
                        api.equinix.javasdk.design.value.ratecard.Term.MONTH_36, new BigDecimal("1500"))
                .build();
        RateCard layered = RateCard.layered(termed, ReferenceRateCard.standard());

        TcoComparison longTerm = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .term(api.equinix.javasdk.design.value.ratecard.Term.MONTH_36)
                .rateCard(layered)
                .compare();
        assertEquals(0, new BigDecimal("1500").compareTo(
                        longTerm.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow()
                                .getLineItems().get("Equinix Fabric connection")),
                "term(MONTH_36) must resolve the 36-month connection rate");

        TcoComparison defaultTerm = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(layered)
                .compare();
        assertEquals(0, new BigDecimal("2000").compareTo(
                        defaultTerm.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow()
                                .getLineItems().get("Equinix Fabric connection")),
                "the default term is MONTH_12");
    }

    @Test
    void recommendedBreakdownReturnsTheRecommendedArchetypesLine() {
        TcoComparison tco = run();

        CostBreakdown recommended = tco.recommendedBreakdown().orElseThrow();
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, recommended.getArchetype());
        assertSame(tco.breakdown(tco.getRecommended()).orElseThrow(), recommended,
                "recommendedBreakdown() must resolve to the same breakdown as breakdown(recommended)");
        assertEquals(0, new BigDecimal("4292.50").compareTo(recommended.getMonthlyTotal()));
    }

    @Test
    void recommendedBreakdownIsEmptyWhenNoArchetypeRecommended() {
        // A model with a null recommendation (e.g. nothing priceable) has no recommended breakdown.
        TcoComparison tco = TcoComparison.builder()
                .breakdowns(java.util.List.of())
                .recommended(null)
                .currency("USD")
                .build();

        assertTrue(tco.recommendedBreakdown().isEmpty());
    }

    // ── Cabinet / cross-connect quantity levers (D3) ──

    @Test
    void cabinetAndCrossConnectCountsMultiplyColocationQuotes() {
        // Per-unit colocation quotes must scale by the configured counts, the way POWER_PER_KW
        // scales by kW: 2 cross-connects @ 100 = 200, 3 cabinets @ 500 = 1500.
        CustomRateCard colo = CustomRateCard.builder()
                .colocationRate(ColocationItem.CROSS_CONNECT, new BigDecimal("100.00"))
                .colocationRate(ColocationItem.CABINET, new BigDecimal("500.00"))
                .build();
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .cabinets(3).crossConnects(2)
                .rateCard(RateCard.layered(colo, ReferenceRateCard.standard()))
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertTrue(equinix.isPriced());
        assertEquals(0, new BigDecimal("200").compareTo(
                        equinix.getLineItems().get("Equinix cross-connect (2x @ 100.00/mo)")),
                "2 cross-connects @ 100: " + equinix.getLineItems().keySet());
        assertEquals(0, new BigDecimal("1500").compareTo(
                        equinix.getLineItems().get("Colocation cabinet (3x @ 500.00/mo)")),
                "3 cabinets @ 500: " + equinix.getLineItems().keySet());
        // Reference baseline without its 300 cross-connect (replaced by the colo quote):
        // 2000 egress + 350 VC + 1642.50 DX port = 3992.50; + 200 + 1500 = 5692.50.
        assertEquals(0, new BigDecimal("5692.50").compareTo(equinix.getMonthlyTotal()));
    }

    @Test
    void crossConnectCountScalesTheReferenceCrossConnectFigure() {
        // With no colocation rate, the reference cross-connect (300) is per unit too: 2x = 600.
        TcoComparison two = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .crossConnects(2)
                .compare();
        CostBreakdown equinix = two.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertEquals(0, new BigDecimal("600").compareTo(
                        equinix.getLineItems().get("Equinix cross-connect (2x @ 300.00/mo)")),
                "the reference per-unit figure must scale: " + equinix.getLineItems().keySet());
        assertEquals(0, new BigDecimal("4592.50").compareTo(equinix.getMonthlyTotal()), "4292.50 + one more 300");

        // Zero omits cross-connects entirely: 4292.50 − 300 = 3992.50.
        TcoComparison none = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .crossConnects(0)
                .compare();
        CostBreakdown noXconn = none.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertEquals(0, new BigDecimal("3992.50").compareTo(noXconn.getMonthlyTotal()));
        assertFalse(noXconn.getLineItems().keySet().stream().anyMatch(k -> k.startsWith("Equinix cross-connect")),
                "crossConnects(0) must omit the line entirely: " + noXconn.getLineItems().keySet());
    }

    // ── Term-total recommendation and savings (D4) ──

    @Test
    void breakdownsCarryTotalOverTermAndComparisonCarriesTermSavings() {
        TcoComparison tco = run(); // default term MONTH_12, no NRC anywhere in the reference figures
        assertEquals(Term.MONTH_12, tco.getTerm());
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertEquals(0, new BigDecimal("51510.00").compareTo(equinix.getTotalOverTerm()), "4292.50 × 12");
        // With zero setup everywhere, the term saving equals the annual saving.
        assertEquals(0, new BigDecimal("56490.00").compareTo(tco.getSavingsOverTermVsBaseline()));
        assertTrue(tco.toMarkdown().contains("Total over term"));
        assertTrue(tco.toMarkdown().contains("**Term:** 12 months"));
    }

    @Test
    void recommendationUsesTotalCostOverTheTermNotJustMrc() {
        // Equinix MRC (4442.50) undercuts the internet baseline (4500), but a 10000 setup charge
        // over a 1-month term makes it far more expensive overall — the recommendation must flip.
        CustomRateCard card = CustomRateCard.builder()
                .currency("USD")
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("1500"), new BigDecimal("10000"))
                .build();
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(50, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .term(Term.MONTH_1)
                .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.EQUINIX_INTERCONNECT)
                .rateCard(card)
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        // 1000 private egress + 1500 VC + 1642.50 reference DX port + 300 reference cross-connect.
        assertEquals(0, new BigDecimal("4442.50").compareTo(equinix.getMonthlyTotal()));
        assertEquals(0, new BigDecimal("14442.50").compareTo(equinix.getTotalOverTerm()), "4442.50 × 1 + 10000");
        assertEquals(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, tco.getRecommended(),
                "an MRC-only comparison would wrongly recommend Equinix here");
        assertEquals(0, BigDecimal.ZERO.compareTo(tco.getSavingsOverTermVsBaseline()),
                "recommended == baseline, so the term saving is zero");
        assertTrue(tco.toMarkdown().contains("**Term:** 1 month"));
    }

    // ── Requested-but-unpriceable Cloud Router (D5) ──

    @Test
    void requestedButUnpriceableCloudRouterMarksArchetypePartiallyPriced() {
        // The card prices egress + connection but has no Cloud Router rate: a requested router
        // must not silently vanish from the total while the archetype stays "priced".
        CustomRateCard noRouter = CustomRateCard.builder()
                .currency("USD")
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("2000"))
                .build();
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .includeCloudRouter("STANDARD")
                .rateCard(noRouter)
                .compare();

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertFalse(equinix.isPriced(), "a requested but unpriceable router leaves the archetype partially priced");
        assertNotNull(equinix.getNote());
        assertTrue(equinix.getNote().contains("Cloud Router (STANDARD)"),
                "the note names the unpriced component: " + equinix.getNote());
        assertNotEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended(),
                "a partially priced archetype is never recommended");
    }

    // ── Builder validation (D7) ──

    @Test
    void builderFailsFastOnInvalidInputs() {
        TcoCalculator.Builder b = TcoCalculator.builder(null);
        assertThrows(IllegalArgumentException.class, () -> b.egress(-1, DataUnit.TERABYTE));
        assertThrows(IllegalArgumentException.class, () -> b.egress(Double.NaN, DataUnit.TERABYTE));
        assertThrows(IllegalArgumentException.class, () -> b.egress(5, null));
        assertThrows(IllegalArgumentException.class, () -> b.bandwidthMbps(0));
        assertThrows(IllegalArgumentException.class, () -> b.bandwidthMbps(-100));
        assertThrows(IllegalArgumentException.class, () -> b.powerKw(-0.5));
        assertThrows(IllegalArgumentException.class, () -> b.cabinets(-1));
        assertThrows(IllegalArgumentException.class, () -> b.crossConnects(-1));
        assertThrows(IllegalArgumentException.class, () -> b.fromCloud(null));
        assertThrows(IllegalArgumentException.class, () -> b.viaMetro(null));
        assertThrows(IllegalArgumentException.class, () -> b.connectionType(null));
        assertThrows(IllegalArgumentException.class, () -> b.term(null));
        assertThrows(IllegalArgumentException.class, () -> b.includeCloudRouter(" "));
        assertThrows(IllegalArgumentException.class, () -> b.onPremTransitPerMbpsMonth(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> b.onPremHardwareMonthly(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> b.onPremCrossConnectMonthly(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> b.onPremPowerPerKwMonth(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, b::archetypes);
        assertThrows(IllegalArgumentException.class,
                () -> b.archetypes(DeploymentArchetype.ON_PREM, null));
        assertThrows(IllegalArgumentException.class, () -> b.additionalLineItem(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> b.additionalLineItem(" ", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> b.additionalLineItem("Compute", null));
    }
}
