package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eqixiac.equinix.design.optimizer.OptimizerFixtures.link;
import static com.eqixiac.equinix.design.optimizer.OptimizerFixtures.metro;
import static com.eqixiac.equinix.design.optimizer.OptimizerFixtures.profile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-currency designs are priced and ranked in one currency. Reproduces offline the live case of
 * 2026-11-11: a London + New York spec lost to Istanbul + New York because the Fabric price search quotes
 * London in GBP (Cloud Router STANDARD GBP 1115, IP_VC 1 Gbps GBP 150; observed 2026-11-12) and Istanbul and
 * New York in USD (1500 and 200), so London designs had an unknown cost. Every run here uses the bundled
 * reference rates or pinned rates: no test reaches the internet.
 */
class CostComparabilityOptimizerTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency SGD = Currency.getInstance("SGD");

    /** The live shape: GBP in London, SGD in Singapore, XAF (no source has it) in Johannesburg, USD elsewhere. */
    static final class LiveLikeCard implements RateCard {
        private static PriceQuote quote(MetroCode metro, String gbp, String sgd, String usd) {
            if (metro == MetroCode.LD) {
                return PriceQuote.monthly(new BigDecimal(gbp), GBP, PriceSource.EQUINIX_LIVE);
            }
            if (metro == MetroCode.SG) {
                return PriceQuote.monthly(new BigDecimal(sgd), SGD, PriceSource.EQUINIX_LIVE);
            }
            if (metro == MetroCode.JN) {
                return PriceQuote.monthly(new BigDecimal(usd), Currency.getInstance("XAF"), PriceSource.EQUINIX_LIVE);
            }
            return PriceQuote.monthly(new BigDecimal(usd), USD, PriceSource.EQUINIX_LIVE);
        }

        @Override
        public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
            return Optional.of(quote(metro, "150", "260", "200"));
        }

        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            return Optional.of(quote(metro, "1115", "1950", "1500"));
        }

        @Override
        public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
            return Optional.of(EgressRate.of(new BigDecimal("0.02"), USD, PriceSource.REFERENCE));
        }

        @Override
        public PriceSource source() {
            return PriceSource.EQUINIX_LIVE;
        }
    }

    private static FabricGateway fabric() {
        return OptimizerFixtures.gateway(List.of(
                metro("NY", "New York", Region.AMER, 40.7128, -74.0060, List.of(GeoScopeType.CONUS),
                        link("LD", 70.1), link("IL", 120.0), link("SG", 230.0), link("JN", 220.0)),
                metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of(GeoScopeType.UK),
                        link("NY", 70.1), link("IL", 55.0), link("SG", 160.0), link("JN", 160.0)),
                metro("IL", "Istanbul", Region.EMEA, 41.0082, 28.9784, List.of(GeoScopeType.EU),
                        link("NY", 120.0), link("LD", 55.0), link("SG", 150.0)),
                metro("SG", "Singapore", Region.APAC, 1.3521, 103.8198, List.of(GeoScopeType.UNKNOWN),
                        link("NY", 230.0), link("LD", 160.0), link("IL", 150.0)),
                metro("JN", "Johannesburg", Region.EMEA, -26.2041, 28.0473, List.of(GeoScopeType.UNKNOWN),
                        link("NY", 220.0), link("LD", 160.0))),
                List.of(profile("sp-aws", "AWS Direct Connect", "NY:us-east-1", "LD:eu-west-2", "IL:eu-central-1",
                        "SG:ap-southeast-1", "JN:af-south-1")));
    }

    private static DesignSpec spec(String currency, CommercialTerms.ExchangeRates rates, String... excluded) {
        return DesignSpec.builder().name("london-newyork")
                .site(SiteIntent.builder().label("London office").metro("LD").headcount(200).build())
                .site(SiteIntent.builder().label("New York office").metro("NY").headcount(200).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).monthlyEgressGb(1000d).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().minMetros(2).maxMetros(2)
                        .excludedMetros(List.of(excluded)).build())
                .objectives(new DesignObjectives(null, Map.of(Objective.COST, 0.5, Objective.LATENCY, 0.5)))
                .commercial(CommercialTerms.builder().currency(currency).exchangeRates(rates)
                        .budgetMaxMonthly(new BigDecimal("100000")).build())
                .build();
    }

    private static OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric(), spec).rateCard(new LiveLikeCard())
                .defaultRates(CurrencyConverters.bundledOnly()).run();
    }

    private static CostLine line(DesignOption option, String prefix) {
        return option.cost().lines().stream().filter(l -> l.label().startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("GBP prices are converted at the bundled reference rate: London is costed, ranked normally and wins")
    void mixedCurrencyIsConvertedNotBlocked() {
        OptimizationResult result = run(spec("USD", null, "SG", "JN"));

        DesignOption rec = result.getRecommended();
        assertEquals("LD+NY", rec.portfolio().key());
        assertTrue(rec.values().get(Objective.COST).known(), rec.values().get(Objective.COST).explanation());
        assertEquals(Optional.of("USD"), rec.cost().currency());
        assertTrue(rec.cost().isComplete());
        assertFalse(result.getCostComparability().incomplete());
        assertTrue(result.getRiskAssessment().getFindings().stream()
                .noneMatch(f -> f.getSeverity() == RiskSeverity.CRITICAL || f.getSeverity() == RiskSeverity.HIGH
                        && f.getCategory().startsWith("COST")));

        // GBP -> USD through the euro: 1.1127 / 0.83618 = 1.33069435 (ECB, 2026-11-11). Router GBP 1115 = USD 1483.72.
        CostLine router = rec.cost().lines().stream()
                .filter(l -> l.label().startsWith("Fabric Cloud Router") && l.converted()).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1483.72"), router.monthly());
        assertEquals("GBP", router.conversion().originalCurrency());
        assertEquals(0, new BigDecimal("1115").compareTo(router.conversion().originalMonthly()));
        assertEquals(new BigDecimal("1.33069435"), router.conversion().rate());
        assertEquals(CurrencyConverter.Kind.REFERENCE, router.conversion().kind());
        assertEquals(PriceSource.ESTIMATE, router.source(), "a bundled-rate conversion is an estimate");
        // Bundled band 5%: 1115 x 1.33069435 = 1483.724; x 0.95 = 1409.54, x 1.05 = 1557.91.
        assertEquals(new BigDecimal("1409.54"), router.monthlyLow());
        assertEquals(new BigDecimal("1557.91"), router.monthlyHigh());
        assertTrue(router.note().contains("bundled reference table, as of 2026-11-11"), router.note());

        assertTrue(result.getRiskAssessment().getFindings().stream().anyMatch(f -> f.getSeverity() == RiskSeverity.INFO
                && f.getCategory().equals("COST_CURRENCY_CONVERTED") && f.getDescription().contains("converted to USD")));
    }

    @Test
    @DisplayName("a rate pinned in the spec overrides the bundled one and is not widened")
    void pinnedRateOverridesBundled() {
        CommercialTerms.ExchangeRates pinned = new CommercialTerms.ExchangeRates(Map.of("GBP", new BigDecimal("1.27")),
                "2026-11-12", "treasury sheet", null);
        OptimizationResult result = run(spec("USD", pinned, "SG", "JN"));
        CostLine router = result.getRecommended().cost().lines().stream()
                .filter(l -> l.label().startsWith("Fabric Cloud Router") && l.converted()).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1416.05"), router.monthly(), "1115 x 1.27");
        assertEquals(CurrencyConverter.Kind.PINNED, router.conversion().kind());
        assertEquals(router.monthly(), router.monthlyLow());
        assertEquals(router.monthly(), router.monthlyHigh());
        assertEquals(PriceSource.EQUINIX_LIVE, router.source());
        assertTrue(router.note().contains("pinned in spec (treasury sheet), as of 2026-11-12"), router.note());
    }

    @Test
    @DisplayName("a cross-region portfolio with USD, GBP and SGD prices gets a known total and ranks normally")
    void threeCurrencyPortfolio() {
        DesignSpec spec = spec("USD", null, "JN").toBuilder()
                .clearWorkloads()
                .workload(WorkloadIntent.builder().label("ny").bandwidthMbps(1000).clouds(List.of("aws")).pinnedMetro("NY").build())
                .workload(WorkloadIntent.builder().label("ld").bandwidthMbps(1000).clouds(List.of("aws")).pinnedMetro("LD").build())
                .workload(WorkloadIntent.builder().label("sg").bandwidthMbps(1000).clouds(List.of("aws")).pinnedMetro("SG").build())
                .constraints(DesignConstraints.builder().minMetros(3).maxMetros(3).excludedMetros(List.of("JN", "IL"))
                        .build()).build();
        OptimizationResult result = run(spec);
        assertFalse(result.getFrontier().isEmpty(), result.getSolverNotes().toString());
        for (DesignOption option : result.getFrontier()) {
            assertTrue(option.values().get(Objective.COST).known(), option.portfolio().key());
            assertEquals(Optional.of("USD"), option.cost().currency());
        }
        PortfolioCost cost = result.getFrontier().stream().filter(o -> o.portfolio().key().contains("SG"))
                .findFirst().orElseThrow().cost();
        assertTrue(cost.monthlyTotal().isPresent());
        assertTrue(cost.lines().stream().anyMatch(l -> l.converted() && l.conversion().originalCurrency().equals("SGD")));
    }

    @Test
    @DisplayName("a currency no source covers leaves the line unpriced with the reason; nothing else is unknown")
    void unknownCurrencyIsUnpriced() {
        DesignSpec spec = spec("USD", null, "SG", "IL", "LD");
        OptimizationResult result = run(spec);
        DesignOption jn = result.getFrontier().stream().filter(o -> o.portfolio().key().contains("JN")).findFirst()
                .orElse(result.getRecommended());
        assertTrue(jn.portfolio().key().contains("JN"));
        assertFalse(jn.cost().isComplete());
        assertTrue(jn.cost().unpriced().stream().anyMatch(u -> u.contains("no exchange rate XAF to USD in any source")),
                jn.cost().unpriced().toString());
        assertFalse(jn.values().get(Objective.COST).known());
    }

    @Test
    @DisplayName("pinned rates are part of the fingerprint, and the used rates can be pinned to reproduce a run")
    void fingerprintAndReproduction() {
        CommercialTerms.ExchangeRates a = new CommercialTerms.ExchangeRates(Map.of("GBP", new BigDecimal("1.27")),
                "2026-11-12", "sheet", null);
        CommercialTerms.ExchangeRates b = new CommercialTerms.ExchangeRates(Map.of("GBP", new BigDecimal("1.28")),
                "2026-11-12", "sheet", null);
        assertNotEquals(spec("USD", a).fingerprint(), spec("USD", b).fingerprint());
        assertNotEquals(spec("USD", null).fingerprint(), spec("USD", a).fingerprint());

        OptimizationResult first = run(spec("USD", null, "SG", "JN"));
        DesignSpec pinned = first.specWithUsedRatesPinned();
        assertEquals(new BigDecimal("1.33069435"), pinned.commercial().exchangeRates().rates().get("GBP"));
        OptimizationResult again = MetroOptimizer.forSpec(fabric(), pinned).rateCard(new LiveLikeCard())
                .defaultRates(CurrencyConverters.none()).run();
        assertEquals(first.getRecommended().cost().monthlyTotal(), again.getRecommended().cost().monthlyTotal(),
                "the pinned run reproduces the figures without any other rate source");
    }

    @Test
    @DisplayName("a non-USD target (EUR) works end to end: USD and GBP lines converted, budget compared in EUR")
    void euroTarget() {
        OptimizationResult result = run(spec("EUR", null, "SG", "JN"));
        DesignOption rec = result.getRecommended();
        assertEquals(Optional.of("EUR"), rec.cost().currency());
        assertTrue(rec.values().get(Objective.COST).known());
        // USD -> EUR: 1 / 1.1127 = 0.89871484; the New York router USD 1500 = EUR 1348.07.
        CostLine nyRouter = rec.cost().lines().stream()
                .filter(l -> l.label().equals("Fabric Cloud Router STANDARD in NY")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1348.07"), nyRouter.monthly());
        assertTrue(result.getConstraintVerdicts().stream().anyMatch(v -> v.constraintId().equals("budget")
                && v.reason().startsWith("EUR ")), result.getConstraintVerdicts().toString());
    }
}
