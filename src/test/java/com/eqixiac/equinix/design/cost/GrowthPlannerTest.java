package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Growth projection and term decisions with hand-computed expectations. Scenario: one 400 Mbps workload to AWS
 * in DC, 1000 GB/month egress, 50% annual growth, 24-month horizon, caller term prices.
 */
class GrowthPlannerTest {

    private static final MetroCode DC = MetroCode.DC;

    private static RateCard card() {
        return CustomRateCard.builder().currency("USD")
                .connectionRate(ConnectionType.IP_VC, 500, DC, Term.MONTH_1, new BigDecimal("100"))
                .connectionRate(ConnectionType.IP_VC, 500, DC, Term.MONTH_12, new BigDecimal("80"))
                .connectionRate(ConnectionType.IP_VC, 500, DC, Term.MONTH_24, new BigDecimal("75"))
                .connectionRate(ConnectionType.IP_VC, 500, DC, Term.MONTH_36, new BigDecimal("60"))
                .connectionRate(ConnectionType.IP_VC, 1000, DC, Term.MONTH_1, new BigDecimal("150"))
                .connectionRate(ConnectionType.IP_VC, 1000, DC, Term.MONTH_12, new BigDecimal("120"))
                .connectionRate(ConnectionType.IP_VC, 1000, DC, Term.MONTH_24, new BigDecimal("110"))
                .connectionRate(ConnectionType.IP_VC, 1000, DC, Term.MONTH_36, new BigDecimal("90"))
                .cloudRouterRate("STANDARD", DC, Term.MONTH_1, new BigDecimal("1000"))
                .cloudRouterRate("STANDARD", DC, Term.MONTH_12, new BigDecimal("900"))
                .cloudRouterRate("STANDARD", DC, Term.MONTH_24, new BigDecimal("850"))
                .cloudRouterRate("STANDARD", DC, Term.MONTH_36, new BigDecimal("800"))
                .egressRate(CloudProviderType.AWS, EgressPath.PRIVATE, new BigDecimal("0.02"))
                .build();
    }

    private static DesignSpec spec(Double growth) {
        return DesignSpec.builder().name("growth")
                .site(SiteIntent.builder().label("HQ").metro("DC").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).monthlyEgressGb(1000d).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(400).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().requiredMetros(List.of("DC")).build())
                .commercial(CommercialTerms.builder().annualGrowthRate(growth).horizonMonths(24).build())
                .build();
    }

    private static GrowthPlanner planner(Double growth, RateCard card) {
        DesignSpec spec = spec(growth);
        FakeDesignContext ctx = new FakeDesignContext(spec).metro("DC", Region.AMER).latency("DC", "HQ", 0.5)
                .reachable("DC", "aws");
        return GrowthPlanner.forSpec(spec, MetroPortfolio.ofCodes("DC"), ctx, card, ReferenceRateCard.standard(),
                CurrencyConverters.bundledOnly());
    }

    private static GrowthPlan.ItemPlan item(GrowthPlan plan, String prefix) {
        return plan.items().stream().filter(i -> i.label().startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("projection: 400 Mbps x 1.5^((n-1)/12) crosses 500 Mbps in month 8 (506.7 Mbps)")
    void projection() {
        assertEquals(1.0, GrowthPlanner.multiplier(0.5, 1));
        assertEquals(Math.sqrt(1.5), GrowthPlanner.multiplier(0.5, 7), 1e-12);
        int[] tiers = GrowthPlanner.tiers(400, 0.5, 24);
        for (int m = 0; m < 7; m++) {
            assertEquals(500, tiers[m], "month " + (m + 1));
        }
        for (int m = 7; m < 24; m++) {
            assertEquals(1000, tiers[m], "month " + (m + 1));
        }
        GrowthPlan.ItemPlan connection = item(planner(0.5, card()).plan(), "Fabric IP_VC");
        assertEquals(8, connection.upgradeMonth());
        assertEquals("growth forces an upgrade from 500 to 1000 Mbps in month 8", connection.upgradeNote());
        assertEquals(-1, GrowthPlanner.tiers(90_000, 0.5, 24)[23], "above 100 Gbps there is no tier");
    }

    @Test
    @DisplayName("term options: totals over the horizon, commitment tail, break-even month, recommendation")
    void termDecision() {
        GrowthPlan plan = planner(0.5, card()).plan();
        GrowthPlan.ItemPlan connection = item(plan, "Fabric IP_VC");
        // months 1-7 at the 500 Mbps price, months 8-24 at 1000 Mbps
        assertOption(connection, Term.MONTH_1, "3250.00", 0, null);    // 7x100 + 17x150
        assertOption(connection, Term.MONTH_12, "2600.00", 0, 11);     // 7x80 + 17x120; obligation 1160 reached in month 11
        assertOption(connection, Term.MONTH_24, "2395.00", 0, 19);     // 7x75 + 17x110; on demand reaches 2395 in month 19
        assertOption(connection, Term.MONTH_36, "3030.00", 12, 23);    // 7x60 + 17x90 + 12 tail months x 90
        assertEquals(Term.MONTH_24, connection.recommended());

        GrowthPlan.ItemPlan router = item(plan, "Fabric Cloud Router");
        assertOption(router, Term.MONTH_1, "24000.00", 0, null);
        assertOption(router, Term.MONTH_12, "21600.00", 0, 11);         // 12 x 900 = 10800 <= 11 x 1000
        assertOption(router, Term.MONTH_24, "20400.00", 0, 21);         // 24 x 850 = 20400 <= 21 x 1000
        assertOption(router, Term.MONTH_36, "28800.00", 12, 29);        // 36 x 800 = 28800 <= 29 x 1000
        assertEquals(Term.MONTH_24, router.recommended());

        // The provider fee has no term axis: AWS hosted 500 Mbps 0.20 x 730 = 146.00 for 7 months,
        // 1 Gbps 0.33 x 730 = 240.90 for 17 months.
        GrowthPlan.ItemPlan fee = item(plan, "Provider hosted-connection fee");
        assertEquals(1, fee.options().size());
        assertOption(fee, Term.MONTH_1, "5117.30", 0, null);
        assertTrue(plan.toMarkdown().contains("growth forces an upgrade from 500 to 1000 Mbps in month 8"));
    }

    @Test
    @DisplayName("a reference figure ignores the term, so commitment options priced from it are unpriced")
    void referenceTermIsNotAPrice() {
        GrowthPlan plan = planner(0.5, ReferenceRateCard.standard()).plan();
        GrowthPlan.ItemPlan router = item(plan, "Fabric Cloud Router");
        GrowthPlan.TermOption m12 = router.options().stream().filter(o -> o.term() == Term.MONTH_12).findFirst()
                .orElseThrow();
        assertFalse(m12.priced());
        assertTrue(m12.unpricedReason().contains("only a REFERENCE figure, which ignores the term"), m12.unpricedReason());
        assertEquals(Term.MONTH_1, router.recommended());
    }

    @Test
    @DisplayName("deterministic cases: 0% growth has no upgrade; hand total 30,384.00 over 24 months")
    void deterministicCases() {
        ScenarioBand band = planner(0.5, card()).scenarios(0.0, 0.6, null);
        ScenarioBand.Case low = band.cases().get(0);
        // 24 x (100 connection + 1000 router + 146.00 provider fee + 20.00 egress) = 30,384.00
        assertEquals(0, new BigDecimal("30384.00").compareTo(low.totalOverHorizon()));
        assertTrue(band.cases().get(2).totalOverHorizon().compareTo(band.cases().get(1).totalOverHorizon()) > 0);
        assertNull(band.simulation());
        // At 80%/year month 24 needs 400 x 1.8^(23/12) = 1235 Mbps: the 2000 Mbps tier, which the card does not price.
        ScenarioBand.Case high = planner(0.5, card()).scenarios(0.0, 0.8, null).cases().get(2);
        assertNull(high.totalOverHorizon());
        assertTrue(high.unpriced().get(0).contains("2000 Mbps"), high.unpriced().toString());
        assertThrows(IllegalArgumentException.class, () -> planner(0.5, card()).scenarios(0.6, 0.8, null));
    }

    @Test
    @DisplayName("seeded Monte Carlo: same seed, same percentiles; distribution and method stated")
    void seededMonteCarlo() {
        GrowthPlanner.MonteCarlo mc = new GrowthPlanner.MonteCarlo(42L, 500, 0.8, 1.3);
        ScenarioBand a = planner(0.5, card()).scenarios(0.2, 0.6, mc);
        ScenarioBand b = planner(0.5, card()).scenarios(0.2, 0.6, mc);
        assertEquals(a.simulation(), b.simulation());
        ScenarioBand c = planner(0.5, card()).scenarios(0.2, 0.6, new GrowthPlanner.MonteCarlo(7L, 500, 0.8, 1.3));
        assertNotEquals(a.simulation().p50(), c.simulation().p50());
        ScenarioBand.Simulation s = a.simulation();
        assertTrue(s.p10().compareTo(s.p50()) <= 0 && s.p50().compareTo(s.p90()) <= 0);
        assertEquals("annual growth ~ Triangular(min 0.2, mode 0.5, max 0.6)", s.growthDistribution());
        assertTrue(s.egressDistribution().startsWith("egress volume factor ~ Triangular(min 0.8, mode 1.0, max 1.3)"));
        assertTrue(s.method().contains("SplittableRandom(seed)"));
        assertTrue(a.toMarkdown().contains("**Monte Carlo** (seed 42, 500 runs)"));
        assertThrows(IllegalArgumentException.class, () -> new GrowthPlanner.MonteCarlo(1L, 0, null, null));
    }

    @Test
    @DisplayName("percentile and triangular helpers")
    void helpers() {
        List<BigDecimal> sorted = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            sorted.add(BigDecimal.valueOf(i));
        }
        assertEquals(BigDecimal.valueOf(1), GrowthPlanner.percentile(sorted, 0.10));
        assertEquals(BigDecimal.valueOf(5), GrowthPlanner.percentile(sorted, 0.50));
        assertEquals(BigDecimal.valueOf(9), GrowthPlanner.percentile(sorted, 0.90));
        assertEquals(0.5, GrowthPlanner.triangular(0.5, 0, 0.5, 1), 1e-12);
        assertEquals(0.0, GrowthPlanner.triangular(0.0, 0, 0.5, 1), 1e-12);
    }

    private static void assertOption(GrowthPlan.ItemPlan item, Term term, String total, int tailMonths,
                                     Integer breakEven) {
        GrowthPlan.TermOption o = item.options().stream().filter(x -> x.term() == term).findFirst().orElseThrow();
        assertTrue(o.priced(), term + ": " + o.unpricedReason());
        assertEquals(new BigDecimal(total), o.totalOverHorizon(), item.label() + " " + term);
        assertEquals(tailMonths, o.tailMonths(), item.label() + " " + term);
        assertEquals(breakEven, o.breakEvenMonth(), item.label() + " " + term);
    }
}
