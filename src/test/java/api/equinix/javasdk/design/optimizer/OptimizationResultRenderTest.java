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
                new ScoreComponent(ScoreCategory.LATENCY, 95.0, 0.30, "HQ: 0.5ms, Branch: 12.0ms"),
                new ScoreComponent(ScoreCategory.PROVIDER_COVERAGE, 100.0, 0.25, "2/2 providers available"),
                new ScoreComponent(ScoreCategory.COST, 80.0, 0.20, "Competitive AMER pricing"),
                new ScoreComponent(ScoreCategory.REDUNDANCY, 90.0, 0.15, "2 metros across 1 region(s)"),
                new ScoreComponent(ScoreCategory.COMPLIANCE, 100.0, 0.10, "Meets all compliance requirements")));

        MetroRecommendation primary = MetroRecommendation.builder()
                .rank(1)
                .metroId(DC)
                .metroName("Ashburn")
                .region(Region.AMER)
                .score(dcScore)
                .reasons(Arrays.asList("Excellent average latency of 6.2ms to user sites",
                        "All 2 required/preferred providers available"))
                .availableProviders(Arrays.asList(
                        new ProviderAvailability("AWS Direct Connect", true, List.of("us-east-1"), "sp-aws-1"),
                        new ProviderAvailability("Azure ExpressRoute", true, List.of("eastus"), "sp-azure-1")))
                .siteLatencies(orderedLatencies("HQ", 0.5, "Branch", 12.0))
                .assignedWorkloads(List.of(
                        new WorkloadPlacement("Web Tier", DC, "Placed in highest-scored metro")))
                .build();

        // ── Secondary metro: Dallas (DA) ──
        MetroScore daScore = new MetroScore(78.0, Arrays.asList(
                new ScoreComponent(ScoreCategory.LATENCY, 70.0, 0.30, "HQ: 30.0ms, Branch: 8.0ms"),
                new ScoreComponent(ScoreCategory.PROVIDER_COVERAGE, 100.0, 0.25, "2/2 providers available"),
                new ScoreComponent(ScoreCategory.COST, 80.0, 0.20, "Competitive AMER pricing"),
                new ScoreComponent(ScoreCategory.REDUNDANCY, 90.0, 0.15, "2 metros across 1 region(s)"),
                new ScoreComponent(ScoreCategory.COMPLIANCE, 100.0, 0.10, "Meets all compliance requirements")));

        MetroRecommendation secondary = MetroRecommendation.builder()
                .rank(2)
                .metroId(DA)
                .metroName("Dallas")
                .region(Region.AMER)
                .score(daScore)
                .reasons(Arrays.asList("Good average latency of 19.0ms to user sites",
                        "Located in AMER region"))
                .availableProviders(List.of(
                        new ProviderAvailability("AWS Direct Connect", true, List.of("us-east-1"), "sp-aws-1")))
                .siteLatencies(orderedLatencies("HQ", 30.0, "Branch", 8.0))
                .assignedWorkloads(List.of(
                        new WorkloadPlacement("DR", DA, "Placed in AMER for geographic diversity from primary")))
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
                new WorkloadPlacement("Web Tier", DC, "Placed in highest-scored metro"),
                new WorkloadPlacement("DR", DA, "Placed in AMER for geographic diversity from primary")));

        // ── Risk assessment (includes one CRITICAL so toSummary emits its WARNING line) ──
        RiskAssessment risk = new RiskAssessment(
                Arrays.asList(
                        new RiskFinding(RiskSeverity.CRITICAL, "REDUNDANCY_GAP",
                                "Requested MULTI_REGION redundancy requires at least 2 metros but the set is single-region",
                                "Relax constraints to allow more eligible metros", null),
                        new RiskFinding(RiskSeverity.MEDIUM, "SINGLE_REGION",
                                "All metros are in the AMER region",
                                "Consider adding a metro in another region for disaster resilience", null)),
                RiskSeverity.CRITICAL,
                45.0);

        // ── Cost estimate ──
        MetroCostBreakdown dcCost = new MetroCostBreakdown(DC, new BigDecimal("1200.00"),
                new BigDecimal("1000.00"), lineItems(), PriceSource.EQUINIX_LIVE);
        MetroCostBreakdown daCost = new MetroCostBreakdown(DA, new BigDecimal("1100.00"),
                new BigDecimal("1000.00"), lineItems(), PriceSource.ESTIMATE);
        CostEstimate cost = new CostEstimate(
                new BigDecimal("2300.00"), new BigDecimal("2000.00"), "USD",
                Arrays.asList(dcCost, daCost), false,
                "Per-metro costs use live Equinix Fabric pricing where available.",
                PriceSource.COMPOSITE);

        // ── Explanation ──
        OptimizationExplanation explanation = new OptimizationExplanation(
                "The optimizer evaluates all metros, filtering to candidates, then scores across 5 dimensions.",
                Arrays.asList("Latency via Fabric avgLatency data", "Cost estimates use a simplified model"),
                "Data fetched at optimization time from live Fabric APIs",
                "Analyzed metros, selected top 2 by BALANCED strategy");

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
            assertTrue(summary.contains("Estimated monthly cost: $2300.00 USD"),
                    "summary should carry the aggregate monthly cost");
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
