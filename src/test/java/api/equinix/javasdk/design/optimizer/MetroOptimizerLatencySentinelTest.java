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

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import api.equinix.javasdk.design.optimizer.enums.ScoreCategory;
import api.equinix.javasdk.design.optimizer.enums.SiteRole;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.LatencyEntry;
import api.equinix.javasdk.design.optimizer.model.LatencyMatrix;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationExplanation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.RiskFinding;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression cover for the {@code Double.MAX_VALUE} latency sentinel that leaked into user-facing
 * text on 2026-07-24.
 *
 * <p><strong>What broke.</strong> {@code LatencyMatrix.worstCase(metro)} ended
 * {@code .max().orElse(Double.MAX_VALUE)}. A request with no user sites produces a matrix with a
 * row per metro but <em>no columns</em>, so the reduction was empty and the sentinel came back as
 * if it were a measurement. The engine's {@code LATENCY_THRESHOLD} loop compared it against
 * {@code maxLatencyMs} unguarded and emitted "SV has worst-case latency of
 * 179769313486231570000&hellip;ms which exceeds the 20ms threshold" — while the same response
 * simultaneously scored LATENCY 100 with the explanation "No sites defined". Self-contradictory
 * output from one run.</p>
 *
 * <p>The fix replaced the sentinel accessors with {@code OptionalDouble}-returning ones and gated
 * both the hard latency filter and the risk surfacing on a single {@code isLatencyBounded}
 * predicate. These tests assert the no-sites path is silent, that no sentinel fingerprint reaches
 * any emitted string, and — extending rather than duplicating
 * {@code MetroOptimizerLeversTest.maxLatencyMsFlagsForcedViolations} — that the with-sites path
 * still filters and still flags, now with the real measured figure in the message.</p>
 */
@DisplayName("MetroOptimizer latency sentinel (no sites + maxLatencyMs)")
class MetroOptimizerLatencySentinelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Decimal figures the reports render with an "ms" suffix. */
    private static final Pattern MS_FIGURE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ms");

    /** {@code String.format("%.1f", Double.MAX_VALUE)} starts with these digits. */
    private static final String FORMATTED_MAX_VALUE_FINGERPRINT = "17976931";

    /** Present in both the {@code %.1f} expansion and the {@code 1.7976931348623157E308} form. */
    private static final String MAX_VALUE_MANTISSA_FINGERPRINT = "7976931348623157";

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;

    private FabricGateway fabric;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                connectedMetro("DA", 10.0), connectedMetro("SV", 60.0), connectedMetro("LD", 75.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(
                connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", Region.AMER, SV_LAT, SV_LON, List.of(
                connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));
        Metro ld = metro("LD", "London", Region.EMEA, LD_LAT, LD_LON, List.of(
                connectedMetro("DC", 75.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv, ld), null, null, null, null));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.<ServiceProfile>of(), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
    }

    // ══════════════════════════════════════════════
    //  6. No sites + a latency bound: silent, and sentinel-free
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("no user sites + maxLatencyMs: zero LATENCY_THRESHOLD findings and no sentinel anywhere")
    void noSitesWithLatencyBoundIsSilentAndSentinelFree() {
        // The exact live shape: a bound is set but there is no site to measure to. The bound is
        // not evaluable, so it must neither exclude anything nor flag anything.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().maxLatencyMs(20.0).maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(4, result.getRecommendations().size(),
                "an unevaluable bound excludes nothing: " + codes(result));

        List<RiskFinding> latencyFindings = result.getRiskAssessment().getFindings().stream()
                .filter(f -> "LATENCY_THRESHOLD".equals(f.getCategory()))
                .collect(Collectors.toList());
        assertTrue(latencyFindings.isEmpty(),
                "with no sites there is no evidence either way, so nothing may be flagged: " + latencyFindings);

        // The matrix itself reports "not measurable" rather than a number.
        LatencyMatrix matrix = result.getLatencyMatrix();
        assertFalse(matrix.hasSites(), "a request with no sites yields a matrix with no columns");
        for (MetroId metro : matrix.getMetros()) {
            assertTrue(matrix.worstCaseMs(metro).isEmpty(), metro + " has nothing to measure to");
            assertTrue(matrix.averageMs(metro).isEmpty(), metro + " has nothing to measure to");
        }

        // The scoring and rationale text agree with each other: neither claims a measurement.
        for (MetroRecommendation rec : result.getRecommendations()) {
            assertEquals("No sites defined", explanationFor(rec, ScoreCategory.LATENCY));
            assertTrue(rec.getSiteLatencies().isEmpty(), "no sites, no per-site latencies");
            assertTrue(rec.getReasons().stream().noneMatch(r -> r.contains("average latency")),
                    "no run may claim 'Excellent average latency of 0.0ms' for a request with no sites: "
                            + rec.getReasons());
        }

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    @Test
    @DisplayName("no user sites and NO bound behaves identically: still silent, still sentinel-free")
    void noSitesWithoutBoundIsAlsoSentinelFree() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(4, result.getRecommendations().size());
        assertTrue(result.getRiskAssessment().getFindings().stream()
                .noneMatch(f -> "LATENCY_THRESHOLD".equals(f.getCategory())));
        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    // ══════════════════════════════════════════════
    //  7. With sites: the bound still filters, still flags — with real figures
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("with sites the bound still excludes far metros and flags a requireMetro-forced breach")
    void withSitesTheBoundStillFiltersAndFlagsWithRealFigures() {
        // Site anchored at DC. Fabric avgLatency from DC: self 0.5ms, DA 10ms, SV 60ms, LD 75ms.
        // A 5ms bound leaves DC alone eligible; requireMetro(SV) forces the 60ms metro back in,
        // which must then surface as a LATENCY_THRESHOLD finding carrying the MEASURED figure.
        // (MetroOptimizerLeversTest already covers the filter/flag split; this adds the assertion
        // that the message text is a real measurement and not the old sentinel.)
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxLatencyMs(5.0).requireMetro(MetroCode.SV).maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DC", "SV"), codes(result).stream().sorted().collect(Collectors.toList()),
                "DC is within the bound, SV is force-included, DA (10ms) and LD (75ms) are excluded");

        List<RiskFinding> latencyFindings = result.getRiskAssessment().getFindings().stream()
                .filter(f -> "LATENCY_THRESHOLD".equals(f.getCategory()))
                .collect(Collectors.toList());
        assertEquals(1, latencyFindings.size(), "only the forced breach is flagged: " + latencyFindings);

        RiskFinding breach = latencyFindings.get(0);
        assertEquals(RiskSeverity.MEDIUM, breach.getSeverity());
        assertEquals(MetroId.of(MetroCode.SV), breach.getAffectedMetro());
        // Built with the same formatter the engine uses, so the assertion holds under any locale.
        assertTrue(breach.getDescription().contains(String.format("%.1fms", 60.0)),
                "the message must carry the measured worst case, not a sentinel: " + breach.getDescription());
        assertTrue(breach.getDescription().contains("exceeds the " + String.format("%.0fms", 5.0) + " threshold"),
                breach.getDescription());

        LatencyMatrix matrix = result.getLatencyMatrix();
        assertTrue(matrix.hasSites());
        assertEquals(60.0, matrix.worstCaseMs(MetroId.of(MetroCode.SV)).orElseThrow(), 1e-9);
        assertEquals(0.5, matrix.worstCaseMs(MetroId.of(MetroCode.DC)).orElseThrow(), 1e-9);

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    // ══════════════════════════════════════════════
    //  8. An unevaluable bound is stated, not silently dropped
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a maxLatencyMs the run cannot evaluate is surfaced as a named finding, not ignored")
    void unevaluableLatencyBoundIsSurfacedExplicitly() {
        // Silence is not a fix for a nonsense message: the caller asked for a hard bound and the
        // run did not apply it, so the run has to say so — in the findings AND in the text that
        // previously claimed the bound had been part of the filtering.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxLatencyMs(20.0).maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        RiskFinding notEvaluated = result.getRiskAssessment().getFindings().stream()
                .filter(f -> "LATENCY_BOUND_NOT_EVALUATED".equals(f.getCategory()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "a requested bound that was not applied must be named: "
                                + result.getRiskAssessment().getFindings()));
        assertEquals(RiskSeverity.MEDIUM, notEvaluated.getSeverity());
        assertTrue(notEvaluated.getDescription().contains("was NOT applied"), notEvaluated.getDescription());
        assertTrue(notEvaluated.getDescription().contains("no user sites"), notEvaluated.getDescription());
        assertNotNull(notEvaluated.getRecommendation(), "it says what input is missing");

        // The methodology must not go on claiming a bound was part of the filtering.
        String methodology = result.getExplanation().getMethodology();
        assertTrue(methodology.contains("was NOT applied"),
                "the methodology must not claim a bound it never applied: " + methodology);
        assertTrue(result.getExplanation().getHumanReadable().contains("could not be evaluated"),
                result.getExplanation().getHumanReadable());

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    @Test
    @DisplayName("with sites the bound IS applied, and nothing claims otherwise")
    void evaluableLatencyBoundIsNotFlaggedAsSkipped() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxLatencyMs(15.0).maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .noneMatch(f -> "LATENCY_BOUND_NOT_EVALUATED".equals(f.getCategory())),
                "the bound was applied, so it must not be reported as skipped: "
                        + result.getRiskAssessment().getFindings());
        assertFalse(result.getExplanation().getMethodology().contains("was NOT applied"),
                result.getExplanation().getMethodology());
        assertTrue(result.getExplanation().getMethodology().contains("max-latency bound"),
                result.getExplanation().getMethodology());
    }

    // ══════════════════════════════════════════════
    //  9. Sites with no weight still steer placement (and say how they were weighted)
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("sites carrying no headcount and no weight still rank metros by proximity")
    void zeroWeightSitesStillDriveRanking() {
        // This is the shape every design_optimize_placement call produces: the MCP tool never sets
        // headcount, and weight defaults to 0, so UserSite.effectiveWeight returned 0 for every
        // site. Latency then scored a flat 100 everywhere — rank-neutral — while the report still
        // listed real per-site latencies and the tool advertised proximity-driven placement.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals("DC", result.primaryMetro().getMetroId().code(),
                "the site anchor must win on latency: " + codes(result));

        // Latency actually separates the metros rather than scoring a constant.
        List<Double> latencyScores = result.getRecommendations().stream()
                .map(rec -> rec.getScore().getComponents().stream()
                        .filter(c -> c.getCategory() == ScoreCategory.LATENCY)
                        .findFirst().orElseThrow().getScore())
                .collect(Collectors.toList());
        assertTrue(latencyScores.stream().distinct().count() > 1,
                "latency must not be a rank-neutral constant: " + latencyScores);

        // ...and the explanation admits where the weighting came from.
        String explanation = explanationFor(result.primaryMetro(), ScoreCategory.LATENCY);
        assertTrue(explanation.contains("HQ: 0.5ms"), explanation);
        assertTrue(explanation.contains("no headcount or explicit weight was given"),
                "the reader must be told the weighting was inferred: " + explanation);

        // The guard that claimed the opposite is gone. It was unreachable (every SiteRole multiplier
        // is positive, so weighted sites always total > 0) AND its text — "Sites defined but none
        // carry any weight, so latency could not rank metros" — was contradicted by this very test,
        // which proves zero-weight sites DO rank metros.
        assertTrue(emittedStrings(result).stream().noneMatch(s -> s.contains("none carry any weight")),
                "no output may claim latency could not rank metros while it demonstrably did");

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    @Test
    @DisplayName("MIXED weighting: a site with no weight is still counted, and the provenance says so")
    void mixedWeightingCountsTheUnweightedSiteAndSaysSo() {
        // The shape design_optimize_placement produces most often: its site schema exposes 'weight'
        // and NO headcount, so one site can carry a weight while another carries neither. The
        // all-zero fallback did not fire (the total was positive), so the second site was weighted
        // ZERO — silently deleted from every proximity calculation — and isImplied() stayed false, so
        // not one word was said about it. Here 'Lab' must actually move the ranking: DA (27.5ms
        // weighted mean to both sites) must beat DC (30.25ms), which wins outright if Lab weighs 0.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).weight(10).done()
                .addSite("Lab").nearestMetro(MetroCode.SV).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals("DA", result.primaryMetro().getMetroId().code(),
                "the unweighted site must pull the ranking; DC only wins if it was zeroed: "
                        + codes(result));

        String explanation = explanationFor(result.primaryMetro(), ScoreCategory.LATENCY);
        assertTrue(explanation.contains("HQ: 10.0ms"), explanation);
        assertTrue(explanation.contains("Lab: 45.0ms"),
                "both sites are measured, not just the weighted one: " + explanation);
        assertTrue(explanation.contains("sites weighted by headcount/explicit weight and role, except "
                        + "'Lab', which carry neither a headcount nor an explicit weight and were "
                        + "weighted as an average site for their role"),
                "the mixed case must name the site whose weight was inferred: " + explanation);

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    @Test
    @DisplayName("the per-site fallback weights by ROLE: swapping the roles swaps the winning metro")
    void impliedWeightsFollowSiteRole() {
        // Neither site carries a weight or a headcount, so both are weighted by role importance.
        // That has to be a real weighting rather than a uniform one, or "weighted by role" is
        // another claim the output makes without acting on it.
        OptimizationResult hqAtDc = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).role(SiteRole.HEADQUARTERS).done()
                .addSite("Branch").nearestMetro(MetroCode.SV).role(SiteRole.BRANCH_OFFICE).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        assertEquals("DC", hqAtDc.primaryMetro().getMetroId().code(),
                "the headquarters end of the pair pulls the winner: " + codes(hqAtDc));

        OptimizationResult hqAtSv = MetroOptimizer.builder(fabric)
                .addSite("Branch").nearestMetro(MetroCode.DC).role(SiteRole.BRANCH_OFFICE).done()
                .addSite("HQ").nearestMetro(MetroCode.SV).role(SiteRole.HEADQUARTERS).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        assertEquals("SV", hqAtSv.primaryMetro().getMetroId().code(),
                "swapping the roles must swap the winner: " + codes(hqAtSv));

        assertTrue(explanationFor(hqAtSv.primaryMetro(), ScoreCategory.LATENCY)
                        .contains("sites weighted by role only"),
                explanationFor(hqAtSv.primaryMetro(), ScoreCategory.LATENCY));
    }

    @Test
    @DisplayName("a proximity-weighted workload placed against zero-weight sites reports a real average")
    void proximityPlacementCarriesAMeasuredAverage() {
        // EDGE_COMPUTE is latency-CRITICAL, so it takes the proximity branch — the branch whose
        // min-search was seeded with Double.MAX_VALUE and formatted that seed into the placement
        // rationale whenever the search advanced on nothing.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).done()
                .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        String reasoning = result.getTopology().getPlacements().get(0).getReasoning();
        assertTrue(reasoning.contains("Lowest weighted latency"), reasoning);
        assertTrue(reasoning.contains(String.format("%.1fms avg", 0.5)),
                "the rationale must carry the measured average: " + reasoning);
        assertTrue(reasoning.contains("no headcount or explicit weight was given"),
                "and say how the weighting behind it was derived: " + reasoning);

        assertNoSentinelAnywhere(result);
        assertEveryLatencyFigureIsSane(result);
    }

    // ══════════════════════════════════════════════
    //  LatencyMatrix accessors in isolation
    // ══════════════════════════════════════════════

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("a site-less matrix reports absent, while the deprecated accessors still answer MAX_VALUE")
    void siteLessMatrixReportsAbsentNotSentinel() {
        MetroId sv = MetroId.of(MetroCode.SV);
        LatencyMatrix noSites = new LatencyMatrix(
                List.of(sv), List.of(), List.of(List.<LatencyEntry>of()));

        assertFalse(noSites.hasSites());
        assertTrue(noSites.worstCaseMs(sv).isEmpty(), "an empty row is not measurable");
        assertTrue(noSites.averageMs(sv).isEmpty(), "an empty row is not measurable");
        assertEquals("(empty matrix)", noSites.toTableString());

        // The deprecated sentinel accessors are retained for source compatibility and still answer
        // Double.MAX_VALUE. The regression is fixed by the engine no longer consulting them —
        // pinned here so the sentinel semantics can never be quietly re-adopted as "a number".
        assertEquals(Double.MAX_VALUE, noSites.worstCase(sv));
        assertEquals(Double.MAX_VALUE, noSites.average(sv));
    }

    @Test
    @DisplayName("a populated matrix measures per metro, and an unknown metro is absent rather than MAX_VALUE")
    void populatedMatrixMeasuresPerMetro() {
        MetroId dc = MetroId.of(MetroCode.DC);
        MetroId sv = MetroId.of(MetroCode.SV);
        LatencyMatrix matrix = new LatencyMatrix(
                List.of(dc, sv),
                List.of("HQ", "Branch"),
                List.of(
                        List.of(new LatencyEntry(dc, "HQ", 0.5, false), new LatencyEntry(dc, "Branch", 12.0, true)),
                        List.of(new LatencyEntry(sv, "HQ", 60.0, false), new LatencyEntry(sv, "Branch", 45.0, true))));

        assertTrue(matrix.hasSites());
        assertEquals(12.0, matrix.worstCaseMs(dc).orElseThrow(), 1e-9);
        assertEquals(6.25, matrix.averageMs(dc).orElseThrow(), 1e-9);
        assertEquals(60.0, matrix.worstCaseMs(sv).orElseThrow(), 1e-9);
        assertEquals(52.5, matrix.averageMs(sv).orElseThrow(), 1e-9);

        MetroId absent = MetroId.of(MetroCode.LD);
        assertTrue(matrix.worstCaseMs(absent).isEmpty(), "a metro not in the matrix is absent, not MAX_VALUE");
        assertTrue(matrix.averageMs(absent).isEmpty(), "a metro not in the matrix is absent, not MAX_VALUE");
        assertFalse(matrix.toTableString().contains(FORMATTED_MAX_VALUE_FINGERPRINT));
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    /** Asserts no string this result can emit carries a {@code Double.MAX_VALUE} fingerprint. */
    private static void assertNoSentinelAnywhere(OptimizationResult result) {
        for (String emitted : emittedStrings(result)) {
            assertFalse(emitted.contains(FORMATTED_MAX_VALUE_FINGERPRINT),
                    "Double.MAX_VALUE formatted as a measurement leaked into: " + emitted);
            assertFalse(emitted.contains(MAX_VALUE_MANTISSA_FINGERPRINT),
                    "Double.MAX_VALUE leaked (scientific form) into: " + emitted);
        }
    }

    /** Asserts every "...ms" figure the result renders is finite and of a plausible magnitude. */
    private static void assertEveryLatencyFigureIsSane(OptimizationResult result) {
        for (String emitted : emittedStrings(result)) {
            Matcher matcher = MS_FIGURE.matcher(emitted);
            while (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1));
                assertTrue(Double.isFinite(value), "non-finite ms figure in: " + emitted);
                assertTrue(value < 1_000_000.0,
                        "implausible ms figure " + matcher.group(1) + " in: " + emitted);
            }
        }
    }

    /** Every human-readable string the result can produce, plus its JSON serialization. */
    private static List<String> emittedStrings(OptimizationResult result) {
        List<String> emitted = new ArrayList<>();
        emitted.add(result.toSummary());
        emitted.add(result.toMarkdown());
        emitted.add(result.toJson());

        OptimizationExplanation explanation = result.getExplanation();
        emitted.add(explanation.getMethodology());
        emitted.add(explanation.getHumanReadable());
        emitted.add(explanation.getDataFreshness());
        emitted.addAll(explanation.getAssumptions());

        for (RiskFinding finding : result.getRiskAssessment().getFindings()) {
            emitted.add(finding.getDescription());
            emitted.add(finding.getRecommendation());
        }

        for (MetroRecommendation rec : result.getRecommendations()) {
            emitted.addAll(rec.getReasons());
            rec.getScore().getComponents().forEach(c -> emitted.add(c.getExplanation()));
            rec.getAvailableProviders().forEach(p -> emitted.add(p.getProviderLabel()));
            rec.getSiteLatencies().forEach((site, latency) ->
                    emitted.add(String.format("%s: %.1fms", site, latency)));
        }

        result.getTopology().getPlacements().forEach(p -> emitted.add(p.getReasoning()));
        emitted.add(result.getTopology().summary());
        emitted.add(result.getLatencyMatrix().toTableString());
        emitted.add(result.getCostEstimate().getCostDisclaimer());

        emitted.removeIf(Objects::isNull);
        return emitted;
    }

    private static List<String> codes(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .collect(Collectors.toList());
    }

    private static String explanationFor(MetroRecommendation rec, ScoreCategory category) {
        return rec.getScore().getComponents().stream()
                .filter(c -> c.getCategory() == category)
                .findFirst().orElseThrow()
                .getExplanation();
    }

    // ── stub builders (same shapes as MetroOptimizerEngineRunTest / MetroOptimizerLeversTest) ──

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}",
                ConnectedMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
