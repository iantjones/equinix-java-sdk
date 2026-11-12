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


package com.eqixiac.equinix.design.record;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.Design;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Cost;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.CostItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Risk;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.RiskLevel;
import com.eqixiac.equinix.design.record.RecordSection.Status;
import com.eqixiac.equinix.design.resilience.ResilienceAnalyzer;
import com.eqixiac.equinix.design.resilience.ResilienceReport;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceLookup;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** The Design Decision Record over an offline world ({@link RecordFixtures}): no network. */
class DesignDecisionRecordTest {

    private static DesignDecisionRecord full() {
        return RecordFixtures.recorder(RecordFixtures.spec()).record();
    }

    @Test
    @DisplayName("a full record: every section included, chaining optimiser, resilience, growth, value and plan")
    void fullRecord() {
        DesignDecisionRecord r = full();
        DesignSpec spec = RecordFixtures.spec();
        assertEquals(spec.fingerprint(), r.specFingerprint());
        assertEquals(DesignDecisionRecord.SCHEMA_VERSION, r.schemaVersion());
        assertEquals(RecordFixtures.NOW, r.generatedAt());
        r.sections().forEach((name, s) -> assertEquals(Status.INCLUDED, s.status(), name + ": " + s.reason()));

        var d = r.decision().content();
        assertEquals("DC+LD", d.portfolio(), "LD is required and only DC or NY add azure; DC serves HQ");
        assertTrue(d.objectives().stream().allMatch(o -> o.scale() != null && o.weight() != null),
                "every objective states its score scale and weight: " + d.objectives());
        var latency = d.objectives().stream().filter(o -> o.id().equals("latency")).findFirst().orElseThrow();
        assertEquals("ms", latency.unit());
        assertEquals("fixed: 0 scores 100, 200 scores 0", latency.scale());

        var alt = r.alternatives().content();
        assertTrue(alt.constraints().stream().anyMatch(c -> c.constraint().equals("required_metros") && c.binding()
                && "DC+NY".equals(c.withoutIt())), "LD is forced: without it DC+NY wins: " + alt.constraints());

        Cost cost = r.cost().content();
        assertEquals("USD", cost.currency());
        CostItem router = cost.lines().stream().filter(l -> l.label().equals("Fabric Cloud Router STANDARD in LD"))
                .findFirst().orElseThrow();
        assertEquals("GBP", router.originalCurrency());
        assertEquals(0, new BigDecimal("1200").compareTo(router.originalMonthly()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(router.monthly()), "GBP 1200 x 1.25");
        assertEquals("contract", router.source());
        assertEquals(1, cost.ratesUsed().size());
        assertEquals("SUPPLIED", cost.ratesUsed().get(0).kind());
        assertTrue(cost.lines().stream().anyMatch(l -> l.source().equals("reference")), "provider fees: reference");
        BigDecimal sum = cost.lines().stream().map(CostItem::monthly).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(cost.monthly()));
        assertNotNull(cost.growth(), cost.growthNote());
        assertEquals(24, cost.growth().horizonMonths());
        assertNotNull(cost.value(), cost.valueNote());
        assertEquals("aws", cost.value().cloud());
        assertNotNull(cost.value().savings());
        assertNotNull(cost.value().tco());

        var res = r.resilience().content();
        assertEquals("DC+LD", res.portfolio());
        assertEquals("FAIL", res.n1().level(), "batch has no standby: losing DC loses azure");
        assertFalse(res.singlePoints().isEmpty());
        assertTrue(res.availabilityBasis().contains("NOT Equinix or cloud-provider SLAs"));
        assertTrue(res.mermaid().startsWith("flowchart"));

        List<Risk> risks = r.risks().content().items();
        for (int i = 1; i < risks.size(); i++) {
            assertTrue(risks.get(i - 1).severity().compareTo(risks.get(i).severity()) <= 0, "most severe first");
        }
        assertEquals(risks.size(), risks.stream().map(x -> x.category() + x.statement()).distinct().count(),
                "de-duplicated");
        assertTrue(risks.stream().anyMatch(x -> x.severity() == RiskLevel.HIGH && x.source().equals("resilience")));

        var p = r.provenance().content();
        assertEquals(2, p.measuredLatencies(), "HQ-LD and London-DC use the measured DC-LD pair");
        assertEquals(2, p.estimatedLatencies(), "HQ-DC and London-LD are the intra-metro assumption");
        assertTrue(p.assumptions().stream().anyMatch(a -> a.source().equals("growth")));
        assertTrue(p.asOf().contains("Exchange rate GBP to USD: test treasury sheet, as of 2026-11-12"), p.asOf()
                .toString());

        var impl = r.implementation().content();
        assertNotNull(impl.plan(), impl.planNote());
        assertEquals(2, impl.plan().routers());
        assertEquals("USD", impl.plan().currency(), "the plan is priced in the spec currency: GBP converted");
        assertEquals(java.util.Map.of("GBP", new BigDecimal("1.25")), impl.reproduction().pinnedRates());
        assertEquals(impl.reproduction().pinnedSpecFingerprint(), r.reproducibleSpec().fingerprint());
        assertNotEquals(r.specFingerprint(), impl.reproduction().pinnedSpecFingerprint());
    }

    @Test
    @DisplayName("JSON round trip: fromJson(toJson()) is equal and re-serialises byte for byte")
    void jsonRoundTrip() {
        DesignDecisionRecord r = full();
        String json = r.toJson();
        DesignDecisionRecord back = DesignDecisionRecord.fromJson(json);
        assertEquals(r, back);
        assertEquals(json, back.toJson());
        assertEquals(r.toMarkdown(), back.toMarkdown());
        assertFalse(json.contains("NaN"));
        assertThrows(IllegalArgumentException.class, () -> DesignDecisionRecord.fromJson("{not json"));
    }

    @Test
    @DisplayName("one engine failing does not lose the record: its sections say failed and why")
    void failingSection() {
        DesignSpec spec = RecordFixtures.spec();
        CatalogDesignContext broken = RecordFixtures.context(spec).withCostModel((portfolio, ctx) -> {
            throw new IllegalStateException("price feed down");
        });
        DesignDecisionRecord r = DesignDecisionRecorder.forContext(broken)
                .clock(java.time.Clock.fixed(RecordFixtures.NOW, java.time.ZoneOffset.UTC)).record();

        assertEquals(Status.INCLUDED, r.context().status());
        for (RecordSection<?> s : List.of(r.decision(), r.alternatives(), r.sensitivity(), r.cost())) {
            assertEquals(Status.FAILED, s.status());
            assertEquals("the Metro Optimizer failed: IllegalStateException: price feed down", s.reason());
        }
        assertEquals(Status.SKIPPED, r.resilience().status());
        assertTrue(r.resilience().reason().startsWith("no recommended design to analyse"));
        assertEquals(Status.INCLUDED, r.implementation().status(), "reproduction info survives");
        assertNull(r.implementation().content().plan());
        assertTrue(r.risks().content().items().stream().anyMatch(x -> x.category().equals("SECTION_FAILED")
                && x.statement().contains("price feed down")));
        String md = r.toMarkdown();
        assertTrue(md.contains("## Decision\n\n_Failed: the Metro Optimizer failed: IllegalStateException: price "
                + "feed down._"), md);
        assertEquals(r, DesignDecisionRecord.fromJson(r.toJson()), "a failed record round-trips too");

        // A card that fails for committed terms: the optimisation (on demand) stands; the term view and the plan
        // (priced at 12 months) say what failed, and nothing else is lost.
        RateCard base = RecordFixtures.card();
        RateCard termsDown = new RateCard() {
            private void check(Term term) {
                if (term != null && term != Term.MONTH_1) {
                    throw new IllegalStateException("term prices unavailable");
                }
            }

            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int mbps, MetroCode metro, Term term) {
                check(term);
                return base.connection(type, mbps, metro, term);
            }

            @Override
            public PriceLookup<PriceQuote> connectionLookup(ConnectionType type, int mbps, MetroCode metro, Term term) {
                check(term);
                return base.connectionLookup(type, mbps, metro, term);
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String code, MetroCode metro, Term term) {
                check(term);
                return base.cloudRouter(code, metro, term);
            }

            @Override
            public PriceLookup<PriceQuote> cloudRouterLookup(String code, MetroCode metro, Term term) {
                check(term);
                return base.cloudRouterLookup(code, metro, term);
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
                return base.egress(provider, region, path, term);
            }

            @Override
            public PriceLookup<EgressRate> egressLookup(CloudProviderType provider, String region, EgressPath path,
                                                        Term term) {
                return base.egressLookup(provider, region, path, term);
            }

            @Override
            public PriceSource source() {
                return base.source();
            }
        };
        DesignDecisionRecord partial = RecordFixtures.recorder(spec).rateCard(termsDown).record();
        assertEquals(Status.INCLUDED, partial.decision().status());
        assertEquals(Status.INCLUDED, partial.resilience().status());
        assertEquals(Status.INCLUDED, partial.cost().status());
        assertNull(partial.cost().content().growth());
        assertEquals("the growth planner failed: IllegalStateException: term prices unavailable",
                partial.cost().content().growthNote());
        assertNull(partial.implementation().content().plan());
        assertTrue(partial.implementation().content().planNote().startsWith("The deployment plan could not be built: "
                + "IllegalStateException: term prices unavailable"), partial.implementation().content().planNote());
        assertTrue(partial.toMarkdown().contains("#### Term and growth\n\n_Not available: the growth planner failed: "
                + "IllegalStateException: term prices unavailable._"));
    }

    @Test
    @DisplayName("a supplied result computed for another spec is rejected; one for this spec is reused as given")
    void fingerprintMismatchRejected() {
        DesignSpec spec = RecordFixtures.spec();
        DesignSpec other = RecordFixtures.specBuilder().constraints(DesignConstraints.builder()
                .requiredMetros(List.of("LD")).maxMetros(3).build()).build();
        OptimizationResult foreign = MetroOptimizer.forContext(RecordFixtures.context(other))
                .rateCard(RecordFixtures.card()).run();
        DesignDecisionRecorder recorder = RecordFixtures.recorder(spec);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> recorder.optimization(foreign));
        assertTrue(e.getMessage().contains("another spec"), e.getMessage());

        ResilienceReport foreignReport = ResilienceAnalyzer.analyze(RecordFixtures.context(other),
                MetroPortfolio.ofCodes("DC", "LD"), null);
        assertThrows(IllegalArgumentException.class, () -> recorder.resilience(foreignReport));
        assertThrows(IllegalArgumentException.class, () -> recorder.plan(DeploymentPlan.builder()
                .specFingerprint(other.fingerprint()).build()));

        OptimizationResult mine = MetroOptimizer.forContext(RecordFixtures.context(spec))
                .rateCard(RecordFixtures.card()).currencyConverter(RecordFixtures.gbpToUsd())
                .defaultRates(com.eqixiac.equinix.design.cost.CurrencyConverters.none()).run();
        DesignDecisionRecord reused = RecordFixtures.recorder(spec).optimization(mine).record();
        assertEquals(mine.getRecommended().portfolio().key(), reused.decision().content().portfolio());
        assertEquals(full().decision(), reused.decision());
    }

    @Test
    @DisplayName("Markdown: deterministic, ADR-shaped, specific numbers, no em dashes")
    void deterministicMarkdown() {
        String md = full().toMarkdown();
        assertEquals(md, full().toMarkdown(), "same inputs, same document");
        String[] expected = {
                "# Metro design for ddr-test\n",
                "- Status: Proposed\n",
                "- Date: 2026-11-12\n",
                "- Spec: `ddr-test`, fingerprint `" + RecordFixtures.spec().fingerprint() + "`\n",
                "## Context\n",
                "- Must include metros LD\n",
                "- Traffic growth 40%/year\n",
                "## Decision\n\nDeploy in **DC + LD** (portfolio `DC+LD`, weighted score ",
                "| Objective | Value | Score | Weight | Scale |\n",
                "| latency | 0.5 ms | 99.8 | 30% | fixed: 0 scores 100, 200 scores 0 |\n",
                "## Consequences\n",
                "## Alternatives considered\n",
                "| required_metros | yes | 3 | DC+NY |",
                "## Evidence\n",
                "### B. Cost\n",
                "| Fabric Cloud Router STANDARD in LD | LD | USD 1,500.00 | 1,500.00 to 1,500.00 | 0.00 | contract |",
                "converted from GBP 1200 at 1.25 USD/GBP (test treasury sheet, as of 2026-11-12)",
                "| GBP | USD | 1.25 | SUPPLIED | test treasury sheet | 2026-11-12 | +/-0% |\n",
                "#### Term and growth\n",
                "### C. Resilience\n",
                "These are computed from planning assumptions, not SLAs",
                "```mermaid\nflowchart",
                "### D. Risks and findings\n",
                "| HIGH | resilience | N1_FAIL |",
                "### E. Assumptions and provenance\n",
                "Site to metro round trips of the recommendation: 2 measured by Fabric, 2 estimated, 0 unknown.",
                "### F. Implementation and reproduction\n",
                "with these exchange rates pinned in `commercial.exchangeRates`: GBP 1.25 (as of 2026-11-12)",
        };
        for (String line : expected) {
            assertTrue(md.contains(line), "missing: " + line + "\n---\n" + md);
        }
        assertFalse(md.contains("\u2014"), "no em dashes");
        assertFalse(md.contains("\u2013"), "no en dashes");
        assertFalse(md.contains("NaN"));
        assertTrue(md.indexOf("## Decision") < md.indexOf("## Consequences")
                && md.indexOf("## Consequences") < md.indexOf("## Alternatives considered")
                && md.indexOf("## Alternatives considered") < md.indexOf("## Evidence"), "ADR order");
    }

    @Test
    @DisplayName("compare(): a re-run with a changed spec reports the spec, recommendation, cost and risk changes")
    void compareRecords() {
        DesignDecisionRecord before = full();
        assertTrue(DesignDecisionRecord.compare(before, full()).isEmpty(), "the same inputs change nothing");
        assertTrue(DesignDecisionRecord.compare(before, full()).toMarkdown().contains("Nothing changed."));

        DesignSpec relaxed = RecordFixtures.specBuilder().constraints(DesignConstraints.builder().maxMetros(2).build())
                .build();
        DesignDecisionRecord after = RecordFixtures.recorder(relaxed).record();
        DecisionDelta delta = DesignDecisionRecord.compare(before, after);
        assertTrue(delta.specChanged());
        assertTrue(delta.specChanges().contains("constraints.requiredMetros: [\"LD\"] -> (unset)"),
                delta.specChanges().toString());
        assertTrue(delta.recommendationChanged());
        assertEquals("DC+LD", delta.recommendationBefore());
        assertEquals("DC+NY", delta.recommendationAfter());
        assertNotNull(delta.costChange(), "both totals in USD");
        assertEquals(0, after.cost().content().monthly().subtract(before.cost().content().monthly())
                .compareTo(delta.costChange()));
        assertTrue(delta.costLineChanges().stream().anyMatch(l -> l.startsWith("removed: Fabric Cloud Router STANDARD "
                + "in LD")), delta.costLineChanges().toString());
        assertFalse(delta.objectiveChanges().isEmpty());

        String md = delta.toMarkdown();
        assertTrue(md.contains("## Recommendation\n\nChanged: `DC+LD` -> `DC+NY`."), md);
        assertTrue(md.contains("## Spec\n"), md);
        assertFalse(md.contains("\u2014"));
        DecisionDelta back = RecordJson.read(RecordJson.write(delta), DecisionDelta.class);
        assertEquals(delta, back, "the delta is JSON-serialisable");
    }

    @Test
    @DisplayName("the facade hands out a recorder for a spec")
    void facade() {
        DesignDecisionRecorder recorder = Design.over(mock(FabricGateway.class)).decisionRecord(RecordFixtures.spec());
        assertNotNull(recorder);
        assertThrows(IllegalArgumentException.class, () -> DesignDecisionRecorder.forSpec(null, RecordFixtures.spec()));
    }
}
