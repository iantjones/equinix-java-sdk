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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import api.equinix.javasdk.design.optimizer.enums.ScoreCategory;
import api.equinix.javasdk.design.optimizer.model.*;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the report-rendering surface of {@link OptimizationResult} —
 * {@link OptimizationResult#toSummary()}, {@link OptimizationResult#toMarkdown()}, and
 * {@link OptimizationResult#toJson()} — against a fully-populated result (primary + secondary
 * metros, per-dimension scores, latency matrix, deployment topology, risk assessment, and cost
 * estimate). These accessors are the human-facing output of the optimizer and previously had no
 * direct coverage; this locks in their section structure and JSON round-trip.
 */
@DisplayName("OptimizationResult rendering (toSummary / toMarkdown / toJson)")
class OptimizationResultRenderTest {

    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);

    private OptimizationResult result;

    @BeforeEach
    void buildFullResult() {
        // ── Primary metro: Ashburn (DC), full score breakdown ──
        MetroScore dcScore = new MetroScore(92.5, Arrays.asList(
                ScoreComponent.builder()
                        .category(ScoreCategory.LATENCY)
                        .score(95.0)
                        .weight(0.30)
                        .explanation("HQ: 0.5ms, Branch: 12.0ms")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.PROVIDER_COVERAGE)
                        .score(100.0)
                        .weight(0.25)
                        .explanation("2/2 providers available")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.COST)
                        .score(80.0)
                        .weight(0.20)
                        .explanation("Competitive AMER pricing")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.REDUNDANCY)
                        .score(90.0)
                        .weight(0.15)
                        .explanation("2 metros across 1 region(s)")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.COMPLIANCE)
                        .score(100.0)
                        .weight(0.10)
                        .explanation("Meets all compliance requirements")
                        .build()));

        MetroRecommendation primary = MetroRecommendation.builder()
                .rank(1)
                .metroId(DC)
                .metroName("Ashburn")
                .region(Region.AMER)
                .score(dcScore)
                .reasons(Arrays.asList("Excellent average latency of 6.2ms to user sites",
                        "All 2 required/preferred providers available"))
                .availableProviders(Arrays.asList(
                        ProviderAvailability.builder()
                                .providerLabel("AWS Direct Connect")
                                .available(true)
                                .sellerRegions(List.of("us-east-1"))
                                .serviceProfileUuid("sp-aws-1")
                                .build(),
                        ProviderAvailability.builder()
                                .providerLabel("Azure ExpressRoute")
                                .available(true)
                                .sellerRegions(List.of("eastus"))
                                .serviceProfileUuid("sp-azure-1")
                                .build()))
                .siteLatencies(orderedLatencies("HQ", 0.5, "Branch", 12.0))
                .assignedWorkloads(List.of(
                        WorkloadPlacement.builder()
                                .workloadLabel("Web Tier")
                                .assignedMetro(DC)
                                .reasoning("Placed in highest-scored metro")
                                .build()))
                .build();

        // ── Secondary metro: Dallas (DA) ──
        MetroScore daScore = new MetroScore(78.0, Arrays.asList(
                ScoreComponent.builder()
                        .category(ScoreCategory.LATENCY)
                        .score(70.0)
                        .weight(0.30)
                        .explanation("HQ: 30.0ms, Branch: 8.0ms")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.PROVIDER_COVERAGE)
                        .score(100.0)
                        .weight(0.25)
                        .explanation("2/2 providers available")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.COST)
                        .score(80.0)
                        .weight(0.20)
                        .explanation("Competitive AMER pricing")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.REDUNDANCY)
                        .score(90.0)
                        .weight(0.15)
                        .explanation("2 metros across 1 region(s)")
                        .build(),
                ScoreComponent.builder()
                        .category(ScoreCategory.COMPLIANCE)
                        .score(100.0)
                        .weight(0.10)
                        .explanation("Meets all compliance requirements")
                        .build()));

        MetroRecommendation secondary = MetroRecommendation.builder()
                .rank(2)
                .metroId(DA)
                .metroName("Dallas")
                .region(Region.AMER)
                .score(daScore)
                .reasons(Arrays.asList("Good average latency of 19.0ms to user sites",
                        "Located in AMER region"))
                .availableProviders(List.of(
                        ProviderAvailability.builder()
                                .providerLabel("AWS Direct Connect")
                                .available(true)
                                .sellerRegions(List.of("us-east-1"))
                                .serviceProfileUuid("sp-aws-1")
                                .build()))
                .siteLatencies(orderedLatencies("HQ", 30.0, "Branch", 8.0))
                .assignedWorkloads(List.of(
                        WorkloadPlacement.builder()
                                .workloadLabel("DR")
                                .assignedMetro(DA)
                                .reasoning("Placed in AMER for geographic diversity from primary")
                                .build()))
                .build();

        // ── Latency matrix ──
        LatencyMatrix latencyMatrix = new LatencyMatrix(
                Arrays.asList(DC, DA),
                Arrays.asList("HQ", "Branch"),
                Arrays.asList(
                        Arrays.asList(new LatencyEntry(DC, "HQ", 0.5, false),
                                new LatencyEntry(DC, "Branch", 12.0, true)),
                        Arrays.asList(new LatencyEntry(DA, "HQ", 30.0, false),
                                new LatencyEntry(DA, "Branch", 8.0, true))));

        // ── Topology ──
        DeploymentTopology topology = new DeploymentTopology(Arrays.asList(
                WorkloadPlacement.builder()
                        .workloadLabel("Web Tier")
                        .assignedMetro(DC)
                        .reasoning("Placed in highest-scored metro")
                        .build(),
                WorkloadPlacement.builder()
                        .workloadLabel("DR")
                        .assignedMetro(DA)
                        .reasoning("Placed in AMER for geographic diversity from primary")
                        .build()));

        // ── Risk assessment (includes one CRITICAL so toSummary emits its WARNING line) ──
        RiskAssessment risk = new RiskAssessment(
                Arrays.asList(
                        RiskFinding.builder()
                                .severity(RiskSeverity.CRITICAL)
                                .category("REDUNDANCY_GAP")
                                .description("Requested MULTI_REGION redundancy requires at least 2 metros but the set is single-region")
                                .recommendation("Relax constraints to allow more eligible metros")
                                .affectedMetro(null)
                                .build(),
                        RiskFinding.builder()
                                .severity(RiskSeverity.MEDIUM)
                                .category("SINGLE_REGION")
                                .description("All metros are in the AMER region")
                                .recommendation("Consider adding a metro in another region for disaster resilience")
                                .affectedMetro(null)
                                .build()),
                RiskSeverity.CRITICAL,
                45.0);

        // ── Cost estimate (per-metro rows carry their own currency since the currency-fidelity
        // fix: MetroCostBreakdown gained a currency field and the renders stopped hardcoding $) ──
        MetroCostBreakdown dcCost = new MetroCostBreakdown(DC, new BigDecimal("1200.00"),
                new BigDecimal("1000.00"), lineItems(), "USD", PriceSource.EQUINIX_LIVE);
        MetroCostBreakdown daCost = new MetroCostBreakdown(DA, new BigDecimal("1100.00"),
                new BigDecimal("1000.00"), lineItems(), "USD", PriceSource.ESTIMATE);
        CostEstimate cost = CostEstimate.builder()
                .monthlyTotal(new BigDecimal("2300.00"))
                .setupTotal(new BigDecimal("2000.00"))
                .currency("USD")
                .perMetro(Arrays.asList(dcCost, daCost))
                .withinBudget(false)
                .costDisclaimer("Per-metro costs use live Equinix Fabric pricing where available.")
                .source(PriceSource.COMPOSITE)
                .build();

        // ── Explanation ──
        OptimizationExplanation explanation = OptimizationExplanation.builder()
                .methodology("The optimizer evaluates all metros, filtering to candidates, then scores across 5 dimensions.")
                .assumptions(Arrays.asList("Latency via Fabric avgLatency data", "Cost estimates use a simplified model"))
                .dataFreshness("Data fetched at optimization time from live Fabric APIs")
                .humanReadable("Analyzed metros, selected top 2 by BALANCED strategy")
                .build();

        result = OptimizationResult.builder()
                .recommendations(Arrays.asList(primary, secondary))
                .topology(topology)
                .latencyMatrix(latencyMatrix)
                .riskAssessment(risk)
                .costEstimate(cost)
                .explanation(explanation)
                .computedAt(Instant.parse("2026-07-01T12:00:00Z"))
                .computeTimeMs(420)
                .build();
    }

    @Nested
    @DisplayName("toSummary()")
    class Summary {

        @Test
        @DisplayName("names the primary metro, additional metros, cost, and over-budget/critical flags")
        void summaryContainsKeyLines() {
            String summary = result.toSummary();

            assertTrue(summary.contains("Recommended primary metro: Ashburn"),
                    "summary should name the primary metro: " + summary);
            assertTrue(summary.contains("92.5/100"), "summary should carry the primary composite score");
            assertTrue(summary.contains("Additional metros: Dallas"),
                    "summary should list the secondary metro");
            assertTrue(summary.contains("WARNING: 1 critical risk(s) identified"),
                    "summary should flag the critical risk");
            // Updated by the currency-fidelity fix: the amount renders with the ACTUAL currency's
            // symbol from CostEstimate.getCurrency() ("$" for USD) instead of a hardcoded "$"
            // followed by the code — the old "$2300.00 USD" printed dollars for every currency.
            assertTrue(summary.contains("Estimated monthly cost: $2300.00"),
                    "summary should carry the aggregate monthly cost in its actual currency: " + summary);
            assertTrue(summary.contains("OVER BUDGET"),
                    "summary should flag the over-budget estimate");
            assertTrue(summary.contains("Computed in 420ms"), "summary should carry the compute time");
        }

        @Test
        @DisplayName("empty recommendations produce the no-viable-metros message")
        void emptySummary() {
            OptimizationResult empty = OptimizationResult.builder()
                    .recommendations(Collections.emptyList())
                    .computedAt(Instant.now())
                    .computeTimeMs(0)
                    .build();
            assertEquals("No viable metros found matching the given constraints.", empty.toSummary());
        }
    }

    @Nested
    @DisplayName("toMarkdown()")
    class Markdown {

        @Test
        @DisplayName("emits every top-level section header")
        void markdownHasAllSections() {
            String md = result.toMarkdown();

            assertTrue(md.contains("# Metro Optimization Report"), "title header");
            assertTrue(md.contains("## Executive Summary"), "executive summary section");
            assertTrue(md.contains("## Ranked Recommendations"), "recommendations section");
            assertTrue(md.contains("## Latency Matrix"), "latency matrix section");
            assertTrue(md.contains("## Deployment Topology"), "topology section");
            assertTrue(md.contains("## Risk Assessment"), "risk section");
            assertTrue(md.contains("## Cost Estimate"), "cost section");
            assertTrue(md.contains("## Methodology"), "methodology section");
        }

        @Test
        @DisplayName("renders per-metro detail: rank headers, score breakdown, providers, risk, cost table")
        void markdownRendersDetail() {
            String md = result.toMarkdown();

            assertTrue(md.contains("### #1: Ashburn (DC)"), "primary rank header: " + md);
            assertTrue(md.contains("### #2: Dallas (DA)"), "secondary rank header");
            assertTrue(md.contains("**Composite Score**: 92.5/100"), "primary composite score");
            assertTrue(md.contains("LATENCY: 95.0"), "score breakdown line");
            assertTrue(md.contains("(weight: 30%)"), "score weight formatting");
            assertTrue(md.contains("Providers Available"), "provider availability line");
            assertTrue(md.contains("AWS Direct Connect"), "available provider label");
            assertTrue(md.contains("Resiliency Score: 45.0/100"), "resiliency score");
            assertTrue(md.contains("**[CRITICAL]**"), "critical finding rendered");
            assertTrue(md.contains("| **Total** | **$2300.00**"), "cost total row");
            assertTrue(md.contains("_Generated: 2026-07-01T12:00:00Z (420ms)_"), "generated timestamp");
        }
    }

    @Nested
    @DisplayName("currency fidelity (no hardcoded dollar sign)")
    class CurrencyFidelity {

        @Test
        @DisplayName("a EUR estimate renders euro amounts everywhere and never a dollar sign")
        void euroEstimateRendersEuroNotDollar() {
            // Regression for the currency-fidelity fix: toSummary/toMarkdown hardcoded "$" for
            // every currency and MetroCostBreakdown carried no currency field at all, so a
            // EUR-quoted Frankfurt/London estimate rendered as dollars — misstating the amount by
            // whatever the exchange rate happened to be. Amounts must render with the actual
            // currency from CostEstimate.getCurrency() and each row's MetroCostBreakdown.getCurrency().
            MetroId ld = MetroId.of("LD");
            MetroCostBreakdown ldCost = new MetroCostBreakdown(ld, new BigDecimal("1800.00"),
                    new BigDecimal("500.00"), lineItems(), "EUR", PriceSource.EQUINIX_LIVE);
            assertEquals("EUR", ldCost.getCurrency(), "the per-metro breakdown carries its currency");

            CostEstimate euros = CostEstimate.builder()
                    .monthlyTotal(new BigDecimal("1800.00"))
                    .setupTotal(new BigDecimal("500.00"))
                    .currency("EUR")
                    .perMetro(Collections.singletonList(ldCost))
                    .withinBudget(true)
                    .costDisclaimer("Live Fabric pricing, quoted in EUR for EMEA metros.")
                    .source(PriceSource.EQUINIX_LIVE)
                    .build();

            MetroRecommendation london = MetroRecommendation.builder()
                    .rank(1)
                    .metroId(ld)
                    .metroName("London")
                    .region(Region.EMEA)
                    .score(new MetroScore(80.0, Collections.emptyList()))
                    .reasons(Collections.singletonList("Located in EMEA region"))
                    .availableProviders(Collections.emptyList())
                    .siteLatencies(Collections.emptyMap())
                    .estimatedCost(ldCost)
                    .assignedWorkloads(Collections.emptyList())
                    .build();

            OptimizationResult euroResult = OptimizationResult.builder()
                    .recommendations(Collections.singletonList(london))
                    .costEstimate(euros)
                    .computedAt(Instant.parse("2026-07-01T12:00:00Z"))
                    .computeTimeMs(10)
                    .build();

            String summary = euroResult.toSummary();
            assertTrue(summary.contains("Estimated monthly cost: €1800.00"),
                    "the summary renders the euro symbol for a EUR estimate: " + summary);
            assertFalse(summary.contains("$"), "no dollar sign may appear in a EUR summary: " + summary);

            String md = euroResult.toMarkdown();
            assertTrue(md.contains("| LD | €1800.00 | €500.00 |"),
                    "per-metro rows render their own currency: " + md);
            assertTrue(md.contains("| **Total** | **€1800.00** | **€500.00** |"),
                    "the total row renders the aggregate currency: " + md);
            assertFalse(md.contains("$"), "no dollar sign may appear in a EUR report: " + md);
        }

        @Test
        @DisplayName("a currency with no distinct symbol renders as '<amount> <code>'")
        void symbollessCurrencyRendersAmountThenCode() {
            MetroId sg = MetroId.of("SG");
            // CHF has no distinct symbol in the US locale, so the code itself is rendered.
            MetroCostBreakdown sgCost = new MetroCostBreakdown(sg, new BigDecimal("900.00"),
                    new BigDecimal("100.00"), lineItems(), "CHF", PriceSource.EQUINIX_LIVE);
            CostEstimate francs = CostEstimate.builder()
                    .monthlyTotal(new BigDecimal("900.00"))
                    .setupTotal(new BigDecimal("100.00"))
                    .currency("CHF")
                    .perMetro(Collections.singletonList(sgCost))
                    .withinBudget(true)
                    .costDisclaimer("d")
                    .source(PriceSource.EQUINIX_LIVE)
                    .build();
            OptimizationResult chfResult = OptimizationResult.builder()
                    .recommendations(Collections.singletonList(MetroRecommendation.builder()
                            .rank(1).metroId(sg).metroName("Zurich-ish").region(Region.EMEA)
                            .score(new MetroScore(70.0, Collections.emptyList()))
                            .reasons(Collections.emptyList())
                            .availableProviders(Collections.emptyList())
                            .siteLatencies(Collections.emptyMap())
                            .assignedWorkloads(Collections.emptyList())
                            .build()))
                    .costEstimate(francs)
                    .computedAt(Instant.parse("2026-07-01T12:00:00Z"))
                    .computeTimeMs(10)
                    .build();

            String summary = chfResult.toSummary();
            assertTrue(summary.contains("Estimated monthly cost: 900.00 CHF"),
                    "a symbol-less currency renders amount-then-code: " + summary);
            assertFalse(summary.contains("$"), summary);
        }
    }

    @Nested
    @DisplayName("toJson()")
    class Json {

        @Test
        @DisplayName("produces parseable JSON carrying the key result fields")
        void jsonParsesBack() throws Exception {
            String json = result.toJson();
            assertFalse(json.contains("\"error\""), "serialization should not have errored: " + json);

            JsonNode root = new ObjectMapper().readTree(json);
            assertNotNull(root.get("recommendations"), "recommendations array present");
            assertEquals(2, root.get("recommendations").size(), "two recommendations serialized");

            JsonNode first = root.get("recommendations").get(0);
            assertEquals(1, first.get("rank").asInt(), "primary rank preserved");
            assertEquals("Ashburn", first.get("metroName").asText(), "primary name preserved");
            assertEquals(92.5, first.get("score").get("composite").asDouble(), 1e-9,
                    "primary composite score preserved");

            assertEquals(420, root.get("computeTimeMs").asLong(), "compute time preserved");
            assertEquals(0, new BigDecimal("2300.00").compareTo(root.get("costEstimate").get("monthlyTotal").decimalValue()),
                    "aggregate monthly cost preserved");
            assertEquals("COMPOSITE", root.get("costEstimate").get("source").asText(),
                    "aggregate price source preserved");
        }
    }

    // ── helpers ──

    private static Map<String, Double> orderedLatencies(String a, double la, String b, double lb) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(a, la);
        m.put(b, lb);
        return m;
    }

    private static Map<String, BigDecimal> lineItems() {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("Fabric connection", new BigDecimal("1200.00"));
        return m;
    }
}
