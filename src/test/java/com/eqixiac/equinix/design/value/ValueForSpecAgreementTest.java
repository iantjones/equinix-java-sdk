package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.cost.RateCardCostModel;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TCO, Savings and the optimiser's RateCardCostModel price the same spec identically, TCO and Savings take a
 * spec ({@code forSpec}) and carry its fingerprint, and an incomplete archetype is never silently called priced.
 */
class ValueForSpecAgreementTest {

    /** Live-like USD card: IP_VC 200, router 1500, private 0.02 / internet 0.09 per GB; London in GBP. */
    static class Card implements RateCard {
        boolean router = true;

        @Override
        public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
            return Optional.of(metro == MetroCode.LD
                    ? PriceQuote.monthly(new BigDecimal("150"), Currency.getInstance("GBP"), PriceSource.EQUINIX_LIVE)
                    : PriceQuote.monthly(new BigDecimal("200"), Currency.getInstance("USD"), PriceSource.EQUINIX_LIVE));
        }

        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            return router ? Optional.of(PriceQuote.monthly(new BigDecimal("1500"), Currency.getInstance("USD"),
                    PriceSource.EQUINIX_LIVE)) : Optional.empty();
        }

        @Override
        public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
            return Optional.of(EgressRate.of(new BigDecimal(path == EgressPath.PRIVATE ? "0.02" : "0.09"),
                    Currency.getInstance("USD"), PriceSource.REFERENCE));
        }

        @Override
        public PriceSource source() {
            return PriceSource.EQUINIX_LIVE;
        }
    }

    private static DesignSpec spec(String metro) {
        return DesignSpec.builder().name("value")
                .site(SiteIntent.builder().label("HQ").metro(metro).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1")
                        .monthlyEgressGb(10_000d).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(700).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().requiredMetros(List.of(metro)).build())
                .commercial(CommercialTerms.builder().term(Term.MONTH_1).build())
                .build();
    }

    private static PortfolioCost costModel(DesignSpec spec, Card card, String metro) {
        RateCardCostModel model = RateCardCostModel.forSpec(spec, card).defaultRates(CurrencyConverters.bundledOnly())
                .build();
        FakeDesignContext ctx = new FakeDesignContext(spec).metro(metro, Region.AMER).latency(metro, "HQ", 0.5)
                .reachable(metro, "aws").costModel(model);
        return model.price(MetroPortfolio.ofCodes(metro), ctx);
    }

    private static BigDecimal line(PortfolioCost cost, String prefix) {
        return cost.lines().stream().filter(l -> l.label().startsWith(prefix)).map(CostLine::monthly).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("same spec, same figures: Fabric connection, router, provider hosted fee and private egress agree")
    void tcoSavingsAndCostModelAgree() {
        DesignSpec spec = spec("DC");
        Card card = new Card();
        PortfolioCost cost = costModel(spec, card, "DC");
        TcoComparison tco = TcoCalculator.forSpec(null, spec, null, MetroPortfolio.ofCodes("DC")).rateCard(card)
                .defaultRates(CurrencyConverters.bundledOnly()).compare();
        SavingsEstimate savings = SavingsCalculator.forSpec(null, spec, null, MetroPortfolio.ofCodes("DC"))
                .rateCard(card).defaultRates(CurrencyConverters.bundledOnly()).calculate();
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();

        // 700 Mbps rounds up to the 1000 Mbps Fabric tier everywhere.
        BigDecimal connection = line(cost, "Fabric IP_VC");
        BigDecimal router = line(cost, "Fabric Cloud Router");
        BigDecimal providerFee = line(cost, "Provider hosted-connection fee");
        BigDecimal egress = line(cost, "Data out of aws");
        assertEquals(0, new BigDecimal("200").compareTo(connection));
        assertEquals(0, new BigDecimal("1500").compareTo(router));
        assertEquals(0, new BigDecimal("240.90").compareTo(providerFee), "AWS 1G hosted 0.33 USD/h x 730");
        assertEquals(0, new BigDecimal("200.00").compareTo(egress), "10,000 GB x 0.02");

        assertEquals(0, connection.compareTo(equinix.getLineItems().get("Equinix Fabric connection")));
        assertEquals(0, router.compareTo(equinix.getLineItems().get("Fabric Cloud Router")));
        assertEquals(0, providerFee.compareTo(equinix.getLineItems().get("Cloud provider interconnect port")));
        assertEquals(0, egress.compareTo(equinix.getLineItems().get("Cloud egress (private interconnect)")));
        assertEquals(0, cost.monthlyTotal().orElseThrow().compareTo(equinix.getMonthlyTotal()),
                "router-originated build: no cross-connect, no cabinet, so the totals agree too");

        assertEquals(0, connection.add(router).compareTo(savings.getEquinixMonthlyCost()));
        assertEquals(0, providerFee.compareTo(savings.getCloudProviderPortMonthlyCost()));
        assertEquals(0, egress.compareTo(savings.getPrivateEgressMonthlyCost()));

        assertEquals(spec.fingerprint(), tco.getSpecFingerprint());
        assertEquals(spec.fingerprint(), savings.getSpecFingerprint());
        assertTrue(tco.getAssumptions().contains("700 Mbps of workloads to 'aws' rounded up to the 1000 Mbps Fabric tier"));
        assertTrue(tco.toMarkdown().contains("Derived from spec fingerprint `" + spec.fingerprint().substring(0, 12)));
    }

    @Test
    @DisplayName("TCO: a missing component makes the archetype incomplete, with an explicit unpriced line, never recommended")
    void tcoCompletenessRule() {
        DesignSpec spec = spec("DC");
        Card card = new Card();
        card.router = false;
        TcoComparison tco = TcoCalculator.forSpec(null, spec, null, MetroPortfolio.ofCodes("DC")).rateCard(card)
                .defaultRates(CurrencyConverters.bundledOnly()).compare();
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertFalse(equinix.isPriced());
        assertEquals(1, equinix.getUnpriced().size());
        assertTrue(equinix.getUnpriced().get(0).startsWith("Fabric Cloud Router (STANDARD): Card has no Cloud Router"),
                equinix.getUnpriced().get(0));
        assertNotEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
        assertTrue(tco.getRecommendationNote().startsWith("The recommendation is chosen among complete archetypes only."),
                tco.getRecommendationNote());
        // Its priced part (200 + 240.90 + 200 = 640.90) is below the internet total (900): it might be cheaper.
        assertTrue(tco.getRecommendationNote().contains("provisional"), tco.getRecommendationNote());
        assertTrue(tco.toMarkdown().contains("unpriced: Fabric Cloud Router (STANDARD)"));
    }

    @Test
    @DisplayName("TCO and Savings convert a GBP Fabric price into the comparison currency instead of dropping it")
    void crossCurrencyIsConverted() {
        DesignSpec spec = spec("LD");
        Card card = new Card();
        TcoComparison tco = TcoCalculator.forSpec(null, spec, null, MetroPortfolio.ofCodes("LD")).rateCard(card)
                .defaultRates(CurrencyConverters.bundledOnly()).compare();
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertTrue(equinix.isPriced(), equinix.getUnpriced().toString());
        assertEquals("USD", equinix.getCurrency());
        // GBP 150 x 1.33069435 (ECB 2026-11-11 via the bundled table) = USD 199.60
        assertEquals(0, new BigDecimal("199.60").compareTo(equinix.getLineItems().get("Equinix Fabric connection")));
        assertEquals(0, costModel(spec, card, "LD").monthlyTotal().orElseThrow().compareTo(equinix.getMonthlyTotal()),
                "the cost model converts at the same rate");

        SavingsEstimate eur = SavingsCalculator.forSpec(null, spec, null, MetroPortfolio.ofCodes("LD")).rateCard(card)
                .currency("EUR").defaultRates(CurrencyConverters.bundledOnly()).calculate();
        assertEquals("EUR", eur.getCurrency());
        assertTrue(eur.isComplete(), eur.getUnpriced().toString());
        // EUR target: GBP 150 x (1 / 0.83618) = EUR 179.39; USD router 1500 x (1 / 1.1127) = EUR 1348.07
        assertEquals(0, new BigDecimal("1527.46").compareTo(eur.getEquinixMonthlyCost()), eur.getDisclaimer());
    }
}
