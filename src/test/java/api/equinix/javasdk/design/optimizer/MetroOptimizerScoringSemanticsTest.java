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
import api.equinix.javasdk.design.optimizer.enums.ComplianceZone;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import api.equinix.javasdk.design.optimizer.enums.ScoreCategory;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.RiskFinding;
import api.equinix.javasdk.design.optimizer.model.ScoringWeights;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression cover for two scoring-semantics fixes:
 *
 * <ul>
 *   <li><strong>Latency grading is continuous.</strong> The curve's final segment used to start at
 *       25 instead of 50, a 25-point cliff at the {@code acceptable} threshold — scores in
 *       (25, 50) were unreachable and two near-identical metros could be graded a quarter of the
 *       scale apart. The curve now interpolates linearly 100 &rarr; 95 &rarr; 75 &rarr; 50 &rarr; 0
 *       across the four thresholds.</li>
 *   <li><strong>Compliance-zone filter and score agree.</strong> The filter ORed the zones'
 *       allowed regions while the score ANDed them, so with two disjoint zones (EU_GDPR +
 *       US_FEDRAMP) every metro passed the filter and then scored 0. The shared semantics now:
 *       a deployment satisfies multiple zones when EACH zone is covered by at least one selected
 *       metro; a metro passes the filter when it helps at least one zone and scores the fraction
 *       of requested zones its region is allowed by (pass &hArr; score &gt; 0); a zone the
 *       selected set leaves uncovered — or a force-included metro outside every zone — raises a
 *       {@code COMPLIANCE_GAP} finding, which is what makes
 *       {@code RiskAssessment.hasComplianceGaps()} reachable at all.</li>
 * </ul>
 */
@DisplayName("MetroOptimizer scoring semantics: continuous latency curve + compliance-zone agreement")
class MetroOptimizerScoringSemanticsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;
    private static final double SG_LAT = 1.3521, SG_LON = 103.8198;

    private static FabricGateway gateway(List<Metro> metroList) {
        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(metroList, null, null, null, null));
        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.<ServiceProfile>of(), null, null, null, null));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static MetroRecommendation recommendation(OptimizationResult result, String code) {
        return result.getRecommendations().stream()
                .filter(r -> code.equals(r.getMetroId().code()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        code + " not recommended: " + result.getRecommendations()));
    }

    private static double componentScore(MetroRecommendation rec, ScoreCategory category) {
        return rec.getScore().getComponents().stream()
                .filter(c -> c.getCategory() == category)
                .findFirst().orElseThrow().getScore();
    }

    private static List<String> codes(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════
    //  Fix: continuous latency grading curve
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("latency grading curve")
    class LatencyCurve {

        /** DC anchor with SV exactly 60ms away by Fabric avgLatency; single stated-weight site. */
        private FabricGateway curveGateway() throws Exception {
            Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                    connectedMetro("DA", 10.0), connectedMetro("SV", 60.0)));
            Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(
                    connectedMetro("DC", 10.0)));
            Metro sv = metro("SV", "Silicon Valley", Region.AMER, SV_LAT, SV_LON, List.of(
                    connectedMetro("DC", 60.0)));
            return gateway(List.of(dc, da, sv));
        }

        private double svLatencyScore(double acceptableMs, double poorMs) throws Exception {
            OptimizationResult result = MetroOptimizer.builder(curveGateway())
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Web").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(100).done()
                    .scoringWeights(ScoringWeights.builder()
                            .latencyAcceptableMs(acceptableMs)
                            .latencyPoorMs(poorMs)
                            .build())
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();
            return componentScore(recommendation(result, "SV"), ScoreCategory.LATENCY);
        }

        @Test
        @DisplayName("the acceptable-to-poor segment interpolates 50 -> 0, not 25 -> 0")
        void finalSegmentStartsAtFifty() throws Exception {
            // SV's weighted mean is exactly 60ms (single site anchored at DC, Fabric avgLatency).
            // With acceptable=50 and poor=150, 60ms sits 10% into the final band:
            // continuous curve  -> 50 * (1 - 10/100) = 45.0
            // pre-fix curve     -> 25 * (1 - 10/100) = 22.5 (a 25-point cliff at the threshold)
            assertEquals(45.0, svLatencyScore(50.0, 150.0), 1e-9,
                    "the final segment must interpolate from 50, closing the unreachable (25,50) band");
        }

        @Test
        @DisplayName("the curve is continuous at the acceptable threshold")
        void curveIsContinuousAtTheAcceptableThreshold() throws Exception {
            // 60ms graded just INSIDE the acceptable band (acceptable=60) scores exactly 50.
            double atThreshold = svLatencyScore(60.0, 150.0);
            assertEquals(50.0, atThreshold, 1e-9, "the acceptable boundary itself grades 50");

            // 60ms graded just PAST the threshold (acceptable=59.9) must land a whisker below 50,
            // not fall off a 25-point cliff as it did before the fix.
            double justPast = svLatencyScore(59.9, 150.0);
            assertTrue(justPast < atThreshold, "past the threshold grades lower: " + justPast);
            assertTrue(atThreshold - justPast < 1.0,
                    "a 0.1ms difference must not cost ~25 points: " + atThreshold + " -> " + justPast);
        }
    }

    // ══════════════════════════════════════════════
    //  Fix: compliance-zone semantics agree across filter, score, and risk
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("compliance zones")
    class ComplianceZones {

        private FabricGateway threeRegionGateway() throws Exception {
            Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                    connectedMetro("LD", 75.0)));
            Metro ld = metro("LD", "London", Region.EMEA, LD_LAT, LD_LON, List.of(
                    connectedMetro("DC", 75.0)));
            Metro sg = metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON, List.of());
            return gateway(List.of(dc, ld, sg));
        }

        @Test
        @DisplayName("two disjoint zones: the filter keeps contributing metros and the score agrees (no more all-pass-then-score-0)")
        void disjointZonesFilterAndScoreAgree() throws Exception {
            // EU_GDPR allows EMEA, US_FEDRAMP allows AMER: no single metro can satisfy both. The
            // pre-fix score ANDed the zones and gave every metro 0 while the filter had passed
            // them all. Now: DC and LD each help one of the two zones (score 50); SG helps
            // neither and is filtered out — pass iff score > 0, so the two paths agree.
            OptimizationResult result = MetroOptimizer.builder(threeRegionGateway())
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Web").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(100).done()
                    .constraints()
                    .compliance(ComplianceZone.EU_GDPR, ComplianceZone.US_FEDRAMP)
                    .maxMetros(4)
                    .done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            assertEquals(List.of("DC", "LD"), codes(result).stream().sorted().collect(Collectors.toList()),
                    "metros helping at least one zone stay; SG (APAC, helps neither) is filtered: "
                            + codes(result));
            assertEquals(50.0, componentScore(recommendation(result, "DC"), ScoreCategory.COMPLIANCE), 1e-9,
                    "DC helps 1 of the 2 requested zones (US_FEDRAMP)");
            assertEquals(50.0, componentScore(recommendation(result, "LD"), ScoreCategory.COMPLIANCE), 1e-9,
                    "LD helps 1 of the 2 requested zones (EU_GDPR)");

            // Both zones are covered by the SET (deployment-level AND holds), so no gap exists.
            assertFalse(result.getRiskAssessment().hasComplianceGaps(),
                    "each zone is covered by a selected metro: " + result.getRiskAssessment().getFindings());
        }

        @Test
        @DisplayName("a requested zone the selected set cannot cover raises COMPLIANCE_GAP and hasComplianceGaps() turns true")
        void unsatisfiedZoneRaisesComplianceGap() throws Exception {
            // Only AMER metros exist, but EU_GDPR (EMEA-only) is requested alongside US_FEDRAMP.
            // Before the fix no COMPLIANCE_GAP finding was ever produced anywhere in the engine, so
            // RiskAssessment.hasComplianceGaps() was unconditionally false — including here, where
            // the deployment demonstrably cannot satisfy EU_GDPR.
            Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                    connectedMetro("DA", 10.0)));
            Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(
                    connectedMetro("DC", 10.0)));
            OptimizationResult result = MetroOptimizer.builder(gateway(List.of(dc, da)))
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Web").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(100).done()
                    .constraints()
                    .compliance(ComplianceZone.EU_GDPR, ComplianceZone.US_FEDRAMP)
                    .maxMetros(4)
                    .done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            assertTrue(result.getRiskAssessment().hasComplianceGaps(),
                    "an uncovered requested zone is a compliance gap: "
                            + result.getRiskAssessment().getFindings());
            RiskFinding gap = result.getRiskAssessment().getFindings().stream()
                    .filter(f -> "COMPLIANCE_GAP".equals(f.getCategory()))
                    .findFirst().orElseThrow();
            assertEquals(RiskSeverity.HIGH, gap.getSeverity());
            assertTrue(gap.getDescription().contains("EU_GDPR"),
                    "the finding names the unsatisfied zone: " + gap.getDescription());
            assertTrue(gap.getDescription().contains("not satisfied by any recommended metro"),
                    gap.getDescription());
        }

        @Test
        @DisplayName("a force-included metro outside every requested zone raises COMPLIANCE_GAP against that metro")
        void forceIncludedMetroConflictRaisesComplianceGap() throws Exception {
            // US_FEDRAMP confines candidacy to AMER; requireMetro(LD) forces an EMEA metro past
            // the compliance filter. The breach must be named against LD instead of shipping a
            // recommendation that quietly contains a metro the constraints would have rejected.
            Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(
                    connectedMetro("LD", 75.0)));
            Metro ld = metro("LD", "London", Region.EMEA, LD_LAT, LD_LON, List.of(
                    connectedMetro("DC", 75.0)));
            OptimizationResult result = MetroOptimizer.builder(gateway(List.of(dc, ld)))
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Web").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(100).done()
                    .constraints()
                    .compliance(ComplianceZone.US_FEDRAMP)
                    .requireMetro(MetroCode.LD)
                    .maxMetros(4)
                    .done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            assertTrue(codes(result).contains("LD"), "precondition: LD was force-included: " + codes(result));

            List<RiskFinding> gaps = result.getRiskAssessment().getFindings().stream()
                    .filter(f -> "COMPLIANCE_GAP".equals(f.getCategory()))
                    .collect(Collectors.toList());
            assertEquals(1, gaps.size(),
                    "US_FEDRAMP itself is covered by DC, so the only gap is the forced metro: " + gaps);
            RiskFinding gap = gaps.get(0);
            assertEquals(MetroId.of(MetroCode.LD), gap.getAffectedMetro(),
                    "the finding is pinned to the conflicting metro: " + gap);
            assertTrue(gap.getDescription().contains("Force-included metro LD"), gap.getDescription());
            assertTrue(gap.getDescription().contains("outside the allowed regions of every requested "
                    + "compliance zone"), gap.getDescription());
            assertTrue(result.getRiskAssessment().hasComplianceGaps());
        }
    }

    // ── stub builders (same shapes as MetroOptimizerLeversTest) ──

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        lenient().when(m.metroId()).thenReturn(MetroId.of(code));
        lenient().when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        lenient().when(m.getName()).thenReturn(name);
        lenient().when(m.getRegion()).thenReturn(region);
        lenient().when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        lenient().when(m.getConnectedMetros()).thenReturn(connected);
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
