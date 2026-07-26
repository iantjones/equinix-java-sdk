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
import api.equinix.javasdk.design.optimizer.enums.LatencySensitivity;
import api.equinix.javasdk.design.optimizer.enums.RiskSeverity;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.RiskFinding;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
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
 * Regression cover for two placement fixes:
 *
 * <ul>
 *   <li><strong>Provider dependencies are a hard constraint on EVERY placement path.</strong>
 *       The latency-critical/proximity rule used to ignore {@code dependsOn} entirely, so a
 *       latency-critical workload could be dropped into a metro missing the very cloud it declared
 *       — behind a rationale that read like an unconstrained proximity win. It now chooses among
 *       dependency-carrying metros, and when no recommended metro carries them it places at best
 *       latency, says so on the placement, and raises a HIGH
 *       {@code WORKLOAD_PROVIDER_NOT_COVERED} finding naming the unmet dependency.</li>
 *   <li><strong>{@code LatencySensitivity.thresholdMs} is wired as the default per-workload
 *       ceiling.</strong> The threshold was documented as driving placement and read by nothing —
 *       HIGH/MEDIUM/LOW were behaviorally identical. A workload's effective ceiling is now its
 *       explicit {@code maxLatencyToleranceMs} if set, else its sensitivity tier's threshold, so
 *       CRITICAL/HIGH place under tighter default ceilings than LOW. Explicit-ceiling behavior is
 *       unchanged.</li>
 * </ul>
 */
@DisplayName("MetroOptimizer placement: hard provider dependencies + sensitivity-implied ceilings")
class MetroOptimizerPlacementDependencyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;

    /** Four metros with Fabric avgLatency to the DC anchor: DC 0.5 (self), DA 10, SV 60, LD 75. */
    private FabricGateway gatewayWithAwsAt(String... awsMetros) throws Exception {
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

        ServiceProfile awsProfile = mock(ServiceProfile.class);
        lenient().when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        lenient().when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        List<ServiceProfileMetro> coverage = new java.util.ArrayList<>();
        for (String code : awsMetros) {
            coverage.add(serviceProfileMetro(code, "us-east-1"));
        }
        lenient().when(awsProfile.metros()).thenReturn(coverage);

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(new PaginatedFilteredList<>(
                awsMetros.length > 0 ? List.of(awsProfile) : List.<ServiceProfile>of(),
                null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static WorkloadPlacement placement(OptimizationResult result, String label) {
        return result.getTopology().getPlacements().stream()
                .filter(p -> label.equals(p.getWorkloadLabel()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no placement for '" + label + "': " + result.getTopology().getPlacements()));
    }

    private static List<RiskFinding> findings(OptimizationResult result, String category) {
        return result.getRiskAssessment().getFindings().stream()
                .filter(f -> category.equals(f.getCategory()))
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════
    //  Fix: proximity placement honours provider dependencies
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("provider dependencies constrain the latency-critical/proximity path")
    class ProximityHonoursDependencies {

        @Test
        @DisplayName("a latency-critical workload is placed in the dependency-carrying metro, not the closest one")
        void latencyCriticalWorkloadHonoursDependencies() throws Exception {
            // AWS only in DA. The proximity rule used to place this CRITICAL workload in DC
            // (0.5ms, no AWS) while its declared AWS dependency was silently ignored.
            FabricGateway fabric = gatewayWithAwsAt("DA");

            OptimizationResult result = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000)
                        .maxLatencyToleranceMs(100)
                        .dependsOn(CloudProviderType.AWS).done()
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement edge = placement(result, "Edge");
            assertEquals(MetroId.of(MetroCode.DA), edge.getAssignedMetro(),
                    "the dependency is a hard constraint: DA is the only metro carrying AWS: "
                            + edge.getReasoning());
            assertTrue(edge.getReasoning().contains("Lowest weighted latency"), edge.getReasoning());
            assertTrue(edge.getReasoning().contains(
                            "chosen among the metros carrying its declared providers (Amazon Web Services)"),
                    "the rationale states the dependency constrained the choice: " + edge.getReasoning());
            assertTrue(findings(result, "WORKLOAD_PROVIDER_NOT_COVERED").isEmpty(),
                    "the dependency WAS satisfied, so nothing may be flagged: "
                            + result.getRiskAssessment().getFindings());
        }

        @Test
        @DisplayName("a dependency no recommended metro satisfies falls back to best latency and raises a HIGH finding")
        void unplaceableDependencyFallsBackAndFlags() throws Exception {
            // AWS resolves in the account (SV carries it) but SV is not selected: maxMetros(2)
            // keeps the two closest metros to the DC anchor. The workload must still be placed
            // (best latency), the placement must say the dependency could not be honoured, and the
            // risk assessment must carry a HIGH finding naming it — previously this shape was
            // completely silent on the proximity path.
            FabricGateway fabric = gatewayWithAwsAt("SV");

            OptimizationResult result = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000)
                        .maxLatencyToleranceMs(100)
                        .dependsOn(CloudProviderType.AWS).done()
                    .constraints().maxMetros(2).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            assertEquals(List.of("DA", "DC"), result.getRecommendations().stream()
                            .map(r -> r.getMetroId().code()).sorted().collect(Collectors.toList()),
                    "precondition: the AWS metro (SV) is not in the selected set");

            WorkloadPlacement edge = placement(result, "Edge");
            assertEquals(MetroId.of(MetroCode.DC), edge.getAssignedMetro(),
                    "with the dependency unplaceable, best latency wins: " + edge.getReasoning());
            assertTrue(edge.getReasoning().contains("NOTE its declared providers (Amazon Web Services) "
                            + "are not all available in any recommended metro"),
                    "the degradation is stated on the placement: " + edge.getReasoning());

            RiskFinding notCovered = findings(result, "WORKLOAD_PROVIDER_NOT_COVERED").stream()
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "the unmet dependency must be a first-class finding: "
                                    + result.getRiskAssessment().getFindings()));
            assertEquals(RiskSeverity.HIGH, notCovered.getSeverity());
            assertTrue(notCovered.getDescription().contains("Edge"),
                    "the finding names the workload: " + notCovered.getDescription());
            assertTrue(notCovered.getDescription().contains("Amazon Web Services"),
                    "the finding names the unmet dependency: " + notCovered.getDescription());
            assertTrue(notCovered.getDescription().contains(
                            "no recommended metro carries Amazon Web Services at all"),
                    notCovered.getDescription());
        }

        @Test
        @DisplayName("the dependency outranks the workload's latency ceiling when the two conflict")
        void dependencyOutranksLatencyCeiling() throws Exception {
            // AWS only in SV (60ms). The EDGE_COMPUTE workload's implied CRITICAL ceiling (5ms)
            // admits only DC — which has no AWS. A reachable cloud beats a fast metro that cannot
            // reach it, so the workload lands in SV and the rationale says which constraint gave.
            FabricGateway fabric = gatewayWithAwsAt("SV");

            OptimizationResult result = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000)
                        .dependsOn(CloudProviderType.AWS).done()
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement edge = placement(result, "Edge");
            assertEquals(MetroId.of(MetroCode.SV), edge.getAssignedMetro(),
                    "the dependency is the harder constraint: " + edge.getReasoning());
            assertTrue(edge.getReasoning().contains("only available outside its latency ceiling"),
                    "the rationale states the ceiling had to give: " + edge.getReasoning());
            assertTrue(findings(result, "WORKLOAD_PROVIDER_NOT_COVERED").isEmpty(),
                    "the dependency IS satisfied in the set, so no coverage finding: "
                            + result.getRiskAssessment().getFindings());
        }
    }

    // ══════════════════════════════════════════════
    //  Fix: LatencySensitivity.thresholdMs is the default per-workload ceiling
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("sensitivity-implied latency ceilings")
    class ImpliedCeilings {

        @Test
        @DisplayName("a MEDIUM workload's implied 50ms ceiling narrows placement; a LOW one's 200ms does not")
        void impliedCeilingTracksTheSensitivityTier() throws Exception {
            // Same request shape, two sensitivities. Before the fix HIGH/MEDIUM/LOW were
            // behaviorally identical: thresholdMs was consulted by nothing.
            FabricGateway fabric = gatewayWithAwsAt();

            OptimizationResult medium = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("App").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement app = placement(medium, "App");
            assertEquals(MetroId.of(MetroCode.DC), app.getAssignedMetro());
            assertTrue(app.getReasoning().contains("within the workload's implied latency ceiling"),
                    "the lead names the implied-ceiling scope: " + app.getReasoning());
            assertTrue(app.getReasoning().contains("50ms default latency ceiling implied by its "
                            + "MEDIUM latency sensitivity"),
                    "the rationale names the tier the ceiling came from: " + app.getReasoning());
            assertTrue(app.getReasoning().contains("ruled out 2 of the 4 recommended metros"),
                    "SV (60ms) and LD (75ms) breach the implied 50ms ceiling: " + app.getReasoning());

            FabricGateway fabric2 = gatewayWithAwsAt();
            OptimizationResult low = MetroOptimizer.builder(fabric2)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Archive").type(WorkloadType.GENERAL_COMPUTE)
                        .latencySensitivity(LatencySensitivity.LOW).bandwidthMbps(1000).done()
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement archive = placement(low, "Archive");
            // All four metros sit within LOW's 200ms threshold, so the implied ceiling binds
            // nothing and — being a tier default the caller never stated — stays silent.
            assertEquals("Placed in highest-scored metro", archive.getReasoning(),
                    "an implied ceiling that binds nothing adds no clause");
        }

        @Test
        @DisplayName("an explicit maxLatencyToleranceMs outranks the tier's implied ceiling (behavior unchanged)")
        void explicitCeilingOutranksImpliedOne() throws Exception {
            // EDGE_COMPUTE is CRITICAL (implied 5ms), but the caller states 100ms explicitly.
            // The explicit lever keeps its documented pre-fix behavior.
            FabricGateway fabric = gatewayWithAwsAt();

            OptimizationResult result = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000)
                        .maxLatencyToleranceMs(100).done()
                    .constraints().maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement edge = placement(result, "Edge");
            assertEquals(MetroId.of(MetroCode.DC), edge.getAssignedMetro());
            assertTrue(edge.getReasoning().contains(
                            "within the workload's 100ms latency tolerance to every user site"),
                    "the explicit ceiling is the one accounted for: " + edge.getReasoning());
            assertFalse(edge.getReasoning().contains("implied"),
                    "the tier default must not be reported when an explicit ceiling overrides it: "
                            + edge.getReasoning());
        }

        @Test
        @DisplayName("an implied ceiling no metro honours is explained on the placement but raises no finding")
        void unhonourableImpliedCeilingIsExplainedNotFlagged() throws Exception {
            // With DC excluded, every remaining metro breaches EDGE_COMPUTE's implied 5ms CRITICAL
            // ceiling (DA 10, SV 60, LD 75). The placement states it — but unlike an explicit
            // tolerance it raises no WORKLOAD_LATENCY_TOLERANCE_UNMET finding, because the caller
            // never stated the ceiling; it is a tier default.
            FabricGateway fabric = gatewayWithAwsAt();

            OptimizationResult result = MetroOptimizer.builder(fabric)
                    .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                    .addWorkload("Edge").type(WorkloadType.EDGE_COMPUTE).bandwidthMbps(1000).done()
                    .constraints().excludeMetro(MetroCode.DC).maxMetros(4).done()
                    .rateCard(ReferenceRateCard.standard())
                    .optimize();

            WorkloadPlacement edge = placement(result, "Edge");
            assertEquals(MetroId.of(MetroCode.DA), edge.getAssignedMetro(),
                    "closest surviving metro wins: " + edge.getReasoning());
            assertTrue(edge.getReasoning().contains("no recommended metro is within the 5ms default "
                            + "latency ceiling implied by its CRITICAL latency sensitivity"),
                    edge.getReasoning());
            assertTrue(edge.getReasoning().contains("the closest is DA at 10ms"), edge.getReasoning());
            assertTrue(findings(result, "WORKLOAD_LATENCY_TOLERANCE_UNMET").isEmpty(),
                    "implied ceilings narrow placement but never raise the explicit-ceiling finding: "
                            + result.getRiskAssessment().getFindings());
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

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
