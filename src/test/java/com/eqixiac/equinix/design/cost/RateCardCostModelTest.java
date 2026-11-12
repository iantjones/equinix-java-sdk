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

package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.objective.CostObjective;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateCardCostModelTest {

    private static final Currency USD = Currency.getInstance("USD");

    /** A live-tagged card: 200/month per connection, 1500 per router; records lookups. */
    static class CountingCard implements RateCard {
        final List<String> lookups = new ArrayList<>();
        PriceSource source = PriceSource.EQUINIX_LIVE;

        @Override
        public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
            lookups.add("conn " + metro + " " + bandwidthMbps);
            return Optional.of(PriceQuote.of(BigDecimal.valueOf(200), BigDecimal.ZERO, USD, source));
        }

        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            lookups.add("router " + metro);
            return Optional.of(PriceQuote.of(BigDecimal.valueOf(1500), BigDecimal.ZERO, USD, source));
        }

        @Override
        public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
            return Optional.of(EgressRate.of(path == EgressPath.PRIVATE ? new BigDecimal("0.02") : new BigDecimal("0.09"),
                    USD, PriceSource.REFERENCE));
        }

        @Override
        public PriceSource source() {
            return source;
        }
    }

    private static DesignSpec spec(Integer bandwidth) {
        return DesignSpec.builder().name("c")
                .site(SiteIntent.builder().label("HQ").metro("DC").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).monthlyEgressGb(1000d).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(bandwidth).clouds(List.of("aws")).build())
                .build();
    }

    private static FakeDesignContext ctx(DesignSpec spec, RateCardCostModel model) {
        return new FakeDesignContext(spec).metro("DC", Region.AMER).metro("NY", Region.AMER).metro("SV", Region.AMER)
                .latency("DC", "HQ", 0.5).latency("NY", "HQ", 6.3).latency("SV", "HQ", 65.8)
                .reachable("DC", "aws").reachable("NY", "aws").costModel(model);
    }

    @Test
    @DisplayName("prices connection, router, provider fee and egress; a metro with no leg gets nothing")
    void pricesWhatTheDesignNeeds() {
        CountingCard card = new CountingCard();
        RateCardCostModel model = RateCardCostModel.builder(card).build();
        // SV reaches no cloud: no leg, so no connection, router or port charge there.
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC", "SV"), ctx(spec(1000), model));
        assertTrue(cost.isComplete(), cost.unpriced().toString());
        for (CostLine line : cost.lines()) {
            assertFalse(line.metro() != null && line.metro().code().equals("SV"), "no phantom charge in SV: " + line);
        }
        // DC: IP_VC 1000 (200) + router (1500) + AWS 1G HOSTED connection (0.33 USD/h x 730 = 240.90)
        // + egress 1000 GB private (20). A Fabric cloud connection is the provider's hosted product, not a
        // dedicated port (which would be 0.30 x 730 = 219.00).
        assertEquals(new BigDecimal("1960.90"), cost.monthlyTotal().orElseThrow().setScale(2));
        CostLine port = cost.lines().stream().filter(l -> l.label().startsWith("Provider")).findFirst().orElseThrow();
        assertEquals(PriceSource.REFERENCE, port.source());
        assertEquals(0, port.monthly().compareTo(new BigDecimal("240.90")));
        assertEquals(0, port.monthlyLow().compareTo(new BigDecimal("180.68")), "reference band -25%: 240.90 x 0.75");
        assertTrue(port.note().contains("hosted connection") && port.note().contains("retrieved 2026-11-12"), port.note());
        CostLine live = cost.lines().stream().filter(l -> l.label().startsWith("Fabric IP_VC")).findFirst().orElseThrow();
        assertEquals(live.monthly(), live.monthlyLow(), "a live price is exact");
    }

    @Test
    @DisplayName("a standby metro doubles the connections; the bandwidth is rounded up to a Fabric tier")
    void standbyAndTiers() {
        CountingCard card = new CountingCard();
        RateCardCostModel model = RateCardCostModel.builder(card).providerPorts(null).build();
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC", "NY"), ctx(spec(700), model));
        assertTrue(card.lookups.contains("conn DC 1000") && card.lookups.contains("conn NY 1000"), card.lookups.toString());
        assertEquals(2, cost.unpriced().size(), "provider fees unpriced without a table: " + cost.unpriced());
        assertEquals(1000, RateCardCostModel.tierFor(700));
        assertEquals(-1, RateCardCostModel.tierFor(200_000));
    }

    @Test
    @DisplayName("lookups are cached per (metro, item)")
    void cached() {
        CountingCard card = new CountingCard();
        RateCardCostModel model = RateCardCostModel.builder(card).build();
        FakeDesignContext ctx = ctx(spec(1000), model);
        model.price(MetroPortfolio.ofCodes("DC"), ctx);
        int first = card.lookups.size();
        model.price(MetroPortfolio.ofCodes("DC"), ctx);
        model.price(MetroPortfolio.ofCodes("DC", "SV"), ctx);
        assertEquals(first, card.lookups.size());
    }

    @Test
    @DisplayName("an unsized workload is unpriced, so the cost objective is unknown and says what is missing")
    void unsizedIsUnpricedAndCostUnknown() {
        RateCardCostModel model = RateCardCostModel.builder(new CountingCard()).build();
        FakeDesignContext ctx = ctx(spec(null), model);
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC"), ctx);
        assertFalse(cost.isComplete());
        ObjectiveValue value = new CostObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx);
        assertFalse(value.known());
        assertTrue(value.explanation().contains("no bandwidthMbps"), value.explanation());
    }

    @Test
    @DisplayName("the cost objective is the cost model's total, and egress uses the internet rate when not connected")
    void objectiveEqualsModelTotal() {
        RateCardCostModel model = RateCardCostModel.builder(new CountingCard()).build();
        FakeDesignContext ctx = ctx(spec(1000), model);
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC"), ctx);
        assertEquals(cost.monthlyTotal().orElseThrow().doubleValue(),
                new CostObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx).value(), 1e-9);

        PortfolioCost internet = model.price(MetroPortfolio.ofCodes("SV"), ctx);
        CostLine egress = internet.lines().stream().filter(l -> l.label().startsWith("Data out")).findFirst().orElseThrow();
        assertEquals(0, egress.monthly().compareTo(new BigDecimal("90.00")), "1000 GB at the internet rate");
        assertTrue(egress.label().contains("internet"));
    }

    @Test
    @DisplayName("bands come from the source only")
    void bands() {
        assertEquals(0.0, PriceBands.halfWidth(PriceSource.EQUINIX_LIVE));
        assertEquals(0.25, PriceBands.halfWidth(PriceSource.REFERENCE));
        assertEquals(0.5, PriceBands.halfWidth(PriceSource.ESTIMATE));
        assertTrue(ReferenceRateCard.standard().providerConnection(CloudProviderType.AWS,
                com.eqixiac.equinix.design.value.ratecard.ConnectionModel.HOSTED, 1000).isPriced());
    }


    private static final class EurRouterCard extends CountingCard {
        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            return Optional.of(PriceQuote.of(BigDecimal.valueOf(1280), BigDecimal.ZERO, Currency.getInstance("EUR"),
                    PriceSource.EQUINIX_LIVE));
        }
    }

    @Test
    @DisplayName("a currency no source covers is unpriced with the reason; it is never added raw")
    void noRateAnywhereIsUnpriced() {
        RateCardCostModel model = RateCardCostModel.builder(new EurRouterCard())
                .defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none()).build();
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC"), ctx(spec(1000), model));
        assertFalse(cost.isComplete());
        assertTrue(cost.unpriced().get(0).contains("priced in EUR 1280/month; no exchange rate EUR to USD in any source"),
                cost.unpriced().toString());
        assertEquals(Optional.of("USD"), cost.currency());
    }

    @Test
    @DisplayName("pinned rate: exact, no widening unless the spec states a band; the line keeps its original amount")
    void pinnedRateConversion() {
        RateCardCostModel exact = RateCardCostModel.builder(new EurRouterCard()).currency("USD")
                .exchangeRates(new com.eqixiac.equinix.design.spec.CommercialTerms.ExchangeRates(
                        java.util.Map.of("EUR", new BigDecimal("1.10")), "2026-11-11", "caller rate", null))
                .defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none()).build();
        CostLine router = exact.price(MetroPortfolio.ofCodes("DC"), ctx(spec(1000), exact)).lines().stream()
                .filter(l -> l.label().contains("Router")).findFirst().orElseThrow();
        assertEquals("USD", router.currency());
        assertEquals(0, router.monthly().compareTo(new BigDecimal("1408.00")), "1280 x 1.10");
        assertEquals(router.monthly(), router.monthlyLow());
        assertEquals(router.monthly(), router.monthlyHigh());
        assertEquals("EUR", router.conversion().originalCurrency());
        assertEquals(0, router.conversion().originalMonthly().compareTo(new BigDecimal("1280")));
        assertTrue(router.note().contains("converted from EUR 1280 at 1.1 USD/EUR (pinned in spec (caller rate), as of "
                + "2026-11-11)"), router.note());

        RateCardCostModel banded = RateCardCostModel.builder(new EurRouterCard())
                .exchangeRates(new com.eqixiac.equinix.design.spec.CommercialTerms.ExchangeRates(
                        java.util.Map.of("EUR", new BigDecimal("1.10")), "2026-11-11", "caller rate", 5.0)).build();
        CostLine wide = banded.price(MetroPortfolio.ofCodes("DC"), ctx(spec(1000), banded)).lines().stream()
                .filter(l -> l.label().contains("Router")).findFirst().orElseThrow();
        // 1280 x 1.10 x 0.95 = 1337.60; x 1.05 = 1478.40.
        assertEquals(0, wide.monthlyLow().compareTo(new BigDecimal("1337.60")));
        assertEquals(0, wide.monthlyHigh().compareTo(new BigDecimal("1478.40")));
        assertTrue(wide.note().contains("range widened +/-5% for exchange-rate uncertainty"), wide.note());
    }

    @Test
    @DisplayName("bundled rate: converted, marked an estimate, widened +/-5%")
    void bundledRateConversion() {
        RateCardCostModel model = RateCardCostModel.builder(new EurRouterCard())
                .defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.bundledOnly()).build();
        PortfolioCost cost = model.price(MetroPortfolio.ofCodes("DC"), ctx(spec(1000), model));
        assertTrue(cost.isComplete());
        CostLine router = cost.lines().stream().filter(l -> l.label().contains("Router")).findFirst().orElseThrow();
        // EUR -> USD at the ECB 2026-11-11 rate 1.1127: 1280 x 1.1127 = 1424.26; low x 0.95 = 1353.04; high x 1.05 = 1495.47.
        assertEquals(0, router.monthly().compareTo(new BigDecimal("1424.26")));
        assertEquals(0, router.monthlyLow().compareTo(new BigDecimal("1353.04")));
        assertEquals(0, router.monthlyHigh().compareTo(new BigDecimal("1495.47")));
        assertEquals(PriceSource.ESTIMATE, router.source());
        assertTrue(new CostObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx(spec(1000), model)).known());
        assertEquals(Optional.of("1 of 4 line(s) converted to USD: EUR at bundled reference table, as of 2026-11-11 "
                + "(ECB euro reference rates; AED via its USD peg)"), CurrencyConverters.advisory(cost));
    }
}

