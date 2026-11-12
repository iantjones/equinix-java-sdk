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
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.solve.ExhaustiveSolver;
import com.eqixiac.equinix.design.solve.SolveRun;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.spi.SolveProblem;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Pricing ahead ({@code CostModel.prepare}): same answers, each item once, bounded stalls. */
class PricingPrefetchTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final String[] METROS = {"AM", "DC", "FR", "LD", "NY", "PA", "SV"};

    /** Prices differ by metro so the ranking depends on cost; counts every lookup; optionally hangs on one metro. */
    static final class SlowCard implements RateCard {
        final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
        final MetroCode hangOn;

        SlowCard(MetroCode hangOn) {
            this.hangOn = hangOn;
        }

        private void count(String key, MetroCode metro) {
            calls.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            if (metro != null && metro == hangOn) {
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
            count("conn " + metro + " " + bandwidthMbps, metro);
            return Optional.of(PriceQuote.of(BigDecimal.valueOf(150 + 10L * metro.name().charAt(0) % 70), BigDecimal.ZERO,
                    USD, PriceSource.EQUINIX_LIVE));
        }

        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            count("router " + metro, metro);
            return Optional.of(PriceQuote.of(BigDecimal.valueOf(1400 + 7L * metro.name().charAt(1) % 200),
                    BigDecimal.ZERO, USD, PriceSource.EQUINIX_LIVE));
        }

        @Override
        public PriceSource source() {
            return PriceSource.EQUINIX_LIVE;
        }
    }

    private static DesignSpec spec() {
        return DesignSpec.builder().name("prefetch")
                .site(SiteIntent.builder().label("London").metro("LD").build())
                .site(SiteIntent.builder().label("New York").metro("NY").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1").build())
                .workload(WorkloadIntent.builder().label("app").bandwidthMbps(1000).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().maxMetros(2).excludedMetros(List.of("PA")).build())
                .commercial(CommercialTerms.builder().budgetMaxMonthly(new BigDecimal("3700")).build())
                .build();
    }

    private static CatalogDesignContext context(DesignSpec spec) {
        CatalogDesignContext.Builder b = CatalogDesignContext.builder(spec)
                .metro("AM", Region.EMEA, 52.4, 4.9).metro("DC", Region.AMER, 38.9, -77.4)
                .metro("FR", Region.EMEA, 50.1, 8.7).metro("LD", Region.EMEA, 51.5, -0.1)
                .metro("NY", Region.AMER, 40.7, -74.0).metro("PA", Region.EMEA, 48.9, 2.4)
                .metro("SV", Region.AMER, 37.4, -121.9)
                .measuredLatency("LD", "NY", 70.0).measuredLatency("DC", "NY", 6.3).measuredLatency("DC", "LD", 73.4);
        for (String m : METROS) {
            b.reachable(m, "aws");
        }
        return b.build();
    }

    private static OptimizationResult run(SlowCard card, int threads, Duration timeout) {
        RateCardCostModel model = RateCardCostModel.forSpec(spec(), card).defaultRates(CurrencyConverters.none())
                .prefetch(threads, timeout, 2).build();
        return MetroOptimizer.forContext(context(spec()).withCostModel(model)).run();
    }

    @Test
    @DisplayName("pricing ahead in parallel gives exactly the result of pricing one portfolio at a time")
    void sameResultAsSequential() {
        OptimizationResult sequential = run(new SlowCard(null), 0, Duration.ofSeconds(20));
        OptimizationResult parallel = run(new SlowCard(null), 6, Duration.ofSeconds(20));
        assertEquals(sequential.getRecommended(), parallel.getRecommended());
        assertEquals(sequential.getFrontier(), parallel.getFrontier());
        assertEquals(sequential.getSolverNotes(), parallel.getSolverNotes());
        assertEquals(sequential.getSensitivity(), parallel.getSensitivity());
        assertEquals(sequential.getSolveStatistics().feasible(), parallel.getSolveStatistics().feasible());
        assertTrue(sequential.getSolverNotes().stream().anyMatch(n -> n.contains("budget")),
                "the budget (a pricing constraint) rejects some portfolios: " + sequential.getSolverNotes());
    }

    @Test
    @DisplayName("each distinct (item, metro, bandwidth, term) is looked up once per run")
    void eachItemOnce() {
        SlowCard card = new SlowCard(null);
        run(card, 6, Duration.ofSeconds(20));
        assertFalse(card.calls.isEmpty());
        card.calls.forEach((k, v) -> assertEquals(1, v.get(), k + " was looked up " + v.get() + " times"));
        // PA is excluded, but portfolios that fail only that exclusion are still priced: they are the evidence
        // for "what would be chosen without this constraint" in the sensitivity analysis.
    }

    @Test
    @DisplayName("a lookup that hangs is priced from the reference card after the timeout, with a note, and the run finishes")
    void hangingLookupFallsBack() {
        long started = System.nanoTime();
        OptimizationResult r = run(new SlowCard(MetroCode.SV), 6, Duration.ofMillis(300));
        long ms = (System.nanoTime() - started) / 1_000_000;
        assertTrue(ms < 10_000, "a 30 s hang must not stall the run (took " + ms + " ms)");
        assertTrue(r.getRiskAssessment().getFindings().stream().anyMatch(f ->
                        "PRICE_LOOKUP_FALLBACK".equals(f.getCategory()) && f.getDescription().contains("SV")
                                && f.getDescription().contains("did not answer within")),
                r.getRiskAssessment().getFindings().toString());
    }

    @Test
    @DisplayName("the solver hands prepare() only portfolios it will price; portfolios two constraints reject are never priced")
    void preparesOnlyWhatIsPriced() {
        DesignSpec spec = spec();
        List<String> prepared = new ArrayList<>();
        List<String> priced = new ArrayList<>();
        CostModel model = new CostModel() {
            @Override
            public PortfolioCost price(MetroPortfolio portfolio, DesignContext context) {
                priced.add(portfolio.key());
                return new PortfolioCost(List.of(), List.of());
            }

            @Override
            public void prepare(Collection<MetroPortfolio> portfolios, DesignContext context) {
                portfolios.forEach(p -> prepared.add(p.key()));
            }
        };
        FakeDesignContext ctx = new FakeDesignContext(spec).metro("DC", Region.AMER).metro("LD", Region.EMEA)
                .metro("NY", Region.AMER).costModel(model);
        Constraint noLd1 = rejects("no_ld_1", "LD", false);
        Constraint noLd2 = rejects("no_ld_2", "LD", false);
        Constraint pricing = new Constraint() {
            @Override
            public String id() {
                return "priced";
            }

            @Override
            public boolean usesCost() {
                return true;
            }

            @Override
            public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
                context.costModel().price(portfolio, context);
                return ConstraintVerdict.pass("priced", "ok");
            }
        };
        Objective cost = new Objective() {
            @Override
            public String id() {
                return "cost";
            }

            @Override
            public Direction direction() {
                return Direction.MINIMIZE;
            }

            @Override
            public String unit() {
                return "USD";
            }

            @Override
            public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
                context.costModel().price(portfolio, context);
                return ObjectiveValue.of(portfolio.size(), "size");
            }
        };
        SolveRun run = new ExhaustiveSolver().solveDetailed(new SolveProblem(ctx, ctx.metros(), List.of(cost),
                List.of(noLd1, noLd2, pricing), Map.of("cost", 1.0), 1, 2));
        assertTrue(prepared.stream().noneMatch(k -> k.contains("LD")), "rejected twice before pricing: " + prepared);
        assertTrue(priced.stream().noneMatch(k -> k.contains("LD")), priced.toString());
        assertEquals(List.of("DC", "NY", "DC+NY"), prepared);
        assertEquals(3, run.feasible().size());
        for (DesignOption o : run.solution().frontier()) {
            assertFalse(o.portfolio().key().contains("LD"));
        }
    }

    @Test
    @DisplayName("rejection counts keep the order of the first rejected portfolio, as a one-pass search reports them")
    void rejectionOrderUnchanged() {
        FakeDesignContext ctx = new FakeDesignContext(spec()).metro("DC", Region.AMER).metro("LD", Region.EMEA)
                .metro("NY", Region.AMER).costModel((p, c) -> new PortfolioCost(List.of(), List.of()));
        Constraint pricing = rejects("priced", "nothing", true);
        SolveRun run = new ExhaustiveSolver().solveDetailed(new SolveProblem(ctx, ctx.metros(), List.of(),
                List.of(rejects("no_dc", "DC", false), rejects("no_ld_1", "LD", false), rejects("no_ld_2", "LD", false),
                        pricing), Map.of(), 1, 2));
        // DC (the first portfolio) fails only no_dc and waits for the pricing constraint; LD then fails two
        // constraints at once and is settled first, and DC+LD is no_dc's first settled rejection. no_dc still comes
        // first, as it did when every portfolio was finished before the next.
        assertTrue(run.solution().notes().contains("Portfolios rejected, by the first constraint that failed: "
                + "{no_dc=3, no_ld_1=2}."), run.solution().notes().toString());
    }

    private static Constraint rejects(String id, String metro, boolean usesCost) {
        return new Constraint() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public boolean usesCost() {
                return usesCost;
            }

            @Override
            public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
                return portfolio.key().contains(metro) ? ConstraintVerdict.fail(id, "contains " + metro)
                        : ConstraintVerdict.pass(id, "ok");
            }
        };
    }
}
