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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cover for making MULTI-REGION redundancy a real, enforced constraint (Part A) and for per-workload
 * cloud eligibility that lets single-cloud EMEA/APAC metros qualify (Part B), plus the budget
 * reporting finding (Part C). Reproduced from the live 2026-07-24 GlobalPay run, where
 * {@code design_plan_deployment} picked NY+MT+DC+TR (all AMER) under a MULTI_REGION request and only
 * warned about it: selection was greedy global top-N, so a single region filled the whole set.
 *
 * <p>Metros are served with coordinates and no inter-metro latency data, so latency is the Haversine
 * fibre estimate from the (AMER-anchored) site. That deterministically makes the AMER metros
 * out-score the EMEA/APAC ones — the exact shape that made greedy top-N collapse to one region.</p>
 */
@DisplayName("MetroOptimizer multi-region spread, per-workload eligibility, and budget reporting")
class MetroOptimizerRegionSpreadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Real-ish centroids so the Haversine ordering is AMER (near NY) < EMEA < APAC.
    private static final double NY_LAT = 40.7128, NY_LON = -74.0060;
    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double CH_LAT = 41.8781, CH_LON = -87.6298;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double LD_LAT = 51.5074, LD_LON = -0.1278;
    private static final double FR_LAT = 50.1109, FR_LON = 8.6821;
    private static final double SG_LAT = 1.3521, SG_LON = 103.8198;
    private static final double TY_LAT = 35.6762, TY_LON = 139.6503;

    // ══════════════════════════════════════════════
    //  Part A — MULTI_REGION spreads across regions instead of clustering in one
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("HEADLINE: MULTI_REGION over a candidate pool of 3 regions selects across >=2 regions, not all-AMER")
    void multiRegionSpreadsAcrossRegions() throws Exception {
        // Four AMER metros out-score two EMEA and one APAC metro from an AMER-anchored site, so
        // greedy top-4 would return four AMER metros (the live NY+MT+DC+TR bug). MULTI_REGION must
        // instead spread across regions.
        FabricGateway fabric = gateway(List.of(
                metro("NY", "New York", Region.AMER, NY_LAT, NY_LON),
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("CH", "Chicago", Region.AMER, CH_LAT, CH_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON),
                metro("FR", "Frankfurt", Region.EMEA, FR_LAT, FR_LON),
                metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.NY).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().redundancy(RedundancyTier.MULTI_REGION).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        Set<Region> regions = selectedRegions(result);
        assertTrue(regions.size() >= 2,
                "MULTI_REGION must span at least two regions, not cluster in one: " + codeRegions(result));
        assertFalse(regions.equals(Set.of(Region.AMER)),
                "the all-AMER greedy result is exactly the live failure this fix removes: " + codeRegions(result));
        // With 3 regions available and max_metros defaulting to 4, round-robin reaches all three.
        assertEquals(Set.of(Region.AMER, Region.EMEA, Region.APAC), regions,
                "round-robin best-per-region reaches every available region: " + codeRegions(result));

        // No single-region risk, because the set genuinely spread.
        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .noneMatch(f -> "SINGLE_REGION".equals(f.getCategory())),
                "a genuinely multi-region set raises no single-region finding: "
                        + result.getRiskAssessment().getFindings());
    }

    @Test
    @DisplayName("CONTROL: the SAME pool under greedy (redundancy none) does cluster all-AMER — proving the tier drives the spread")
    void greedyStillClustersProvingTheContrast() throws Exception {
        FabricGateway fabric = gateway(List.of(
                metro("NY", "New York", Region.AMER, NY_LAT, NY_LON),
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("CH", "Chicago", Region.AMER, CH_LAT, CH_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON),
                metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.NY).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().maxMetros(4).done() // redundancy none => greedy top-N
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(Set.of(Region.AMER), selectedRegions(result),
                "with no redundancy tier the greedy top-N still picks the highest-scored (AMER) metros: "
                        + codeRegions(result));
    }

    @Test
    @DisplayName("MULTI_REGION with only one region carrying candidates is a HIGH blocker finding, not a silent single-region set")
    void multiRegionWithOnlyOneRegionAvailableIsBlocked() throws Exception {
        // AWS is required and present only in the AMER metros, so only AMER qualifies. Spread is
        // genuinely impossible; the run must say so and name what to relax rather than quietly
        // returning an all-AMER set and calling it multi-region.
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("CH", "Chicago", Region.AMER, CH_LAT, CH_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON)),
                List.of(profile("sp-aws", "AWS Direct Connect",
                        spm("DC", "us-east-1"), spm("DA", "us-east-1"), spm("CH", "us-east-1"))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().redundancy(RedundancyTier.MULTI_REGION).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertTrue(result.getRecommendations().size() >= 2, "the AMER metros are still recommended");
        assertEquals(Set.of(Region.AMER), selectedRegions(result), "only AMER carries AWS here");

        RiskFinding blocker = finding(result, "SINGLE_REGION");
        assertEquals(RiskSeverity.HIGH, blocker.getSeverity());
        assertTrue(blocker.getDescription().contains("MULTI_REGION"), blocker.getDescription());
        assertTrue(blocker.getDescription().contains("no other region has a metro that meets the constraints"),
                "the finding names WHY spread was impossible: " + blocker.getDescription());
        assertTrue(blocker.getRecommendation().contains("Relax"),
                "and says what to relax: " + blocker.getRecommendation());
    }

    @Test
    @DisplayName("the MULTI_REGION explanation describes region round-robin spread, not a plain top-N-by-score selection")
    void multiRegionExplanationDescribesSpreadNotTopN() throws Exception {
        // The engine dropped higher-scored AMER metros to reach EMEA/APAC (round-robin best-per-region),
        // so the agent-facing text must say THAT — not the greedy "selected the top N by <strategy>
        // strategy", which would tell the reading LLM the set was the highest-scored N when it was not.
        FabricGateway fabric = gateway(List.of(
                metro("NY", "New York", Region.AMER, NY_LAT, NY_LON),
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("CH", "Chicago", Region.AMER, CH_LAT, CH_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON),
                metro("FR", "Frankfurt", Region.EMEA, FR_LAT, FR_LON),
                metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.NY).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().redundancy(RedundancyTier.MULTI_REGION).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        String humanReadable = result.getExplanation().getHumanReadable();
        assertFalse(humanReadable.contains("selected the top"),
                "a MULTI_REGION run must not describe itself as the greedy top-N: " + humanReadable);
        assertTrue(humanReadable.contains("spread across regions"),
                "the human_readable states the set was spread across regions: " + humanReadable);
        assertTrue(humanReadable.contains("round-robin best-per-region"),
                "and names the actual selection method: " + humanReadable);

        String methodology = result.getExplanation().getMethodology();
        assertTrue(methodology.contains("round-robin best-per-region"),
                "the methodology describes region round-robin selection for a spread tier: " + methodology);
        assertFalse(methodology.contains("The highest-scoring metros are then selected"),
                "the greedy selection sentence must not appear for a spread tier: " + methodology);
    }

    @Test
    @DisplayName("CONTROL: a greedy (redundancy none) run keeps the plain top-N-by-strategy explanation")
    void greedyExplanationKeepsTopNWording() throws Exception {
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("CH", "Chicago", Region.AMER, CH_LAT, CH_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        String humanReadable = result.getExplanation().getHumanReadable();
        assertTrue(humanReadable.contains("selected the top " + result.getRecommendations().size()),
                "a non-redundancy run keeps the greedy top-N wording: " + humanReadable);
        assertFalse(humanReadable.contains("spread across regions"), humanReadable);

        String methodology = result.getExplanation().getMethodology();
        assertTrue(methodology.contains("The highest-scoring metros are then selected"),
                "the greedy selection sentence describes a non-spread run: " + methodology);
        assertFalse(methodology.contains("round-robin best-per-region"), methodology);
    }

    // ══════════════════════════════════════════════
    //  Part B — per-workload cloud eligibility
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a single-cloud EMEA/APAC metro qualifies to host the single-cloud workload it can serve")
    void singleCloudMetroQualifiesForItsWorkload() throws Exception {
        // require_clouds is the UNION [aws, azure]; DC carries both, LD only AWS, SG only Azure.
        // The old rule forced EVERY metro to carry BOTH and excluded LD and SG. Now LD qualifies to
        // host the AWS-only workload and SG the Azure-only one.
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON),
                metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON)),
                List.of(
                        profile("sp-aws", "AWS Direct Connect", spm("DC", "us-east-1"), spm("LD", "eu-west-1")),
                        profile("sp-azr", "Azure ExpressRoute", spm("DC", "east-us"), spm("SG", "southeastasia"))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.AZURE).done()
                .addWorkload("Payments-AWS").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AWS).done()
                .addWorkload("Fraud-Azure").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AZURE).done()
                .constraints().maxMetros(4).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertEquals(List.of("DC", "LD", "SG"), sortedCodes(result),
                "LD (AWS-only) and SG (Azure-only) must qualify to host their single-cloud workloads: "
                        + sortedCodes(result));

        // Each workload lands where ITS cloud is, and the coverage guarantee holds (no finding).
        assertTrue(placementMetro(result, "Payments-AWS").matches("DC|LD"),
                "the AWS workload is placed where AWS is available");
        assertTrue(placementMetro(result, "Fraud-Azure").matches("DC|SG"),
                "the Azure workload is placed where Azure is available");
        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .noneMatch(f -> "REQUIRED_CLOUD_NOT_COVERED".equals(f.getCategory())),
                "both required clouds are reachable in the set: " + result.getRiskAssessment().getFindings());
    }

    @Test
    @DisplayName("A+B together: MULTI_REGION reaches EMEA/APAC because single-cloud metros now qualify")
    void multiRegionReachesOtherRegionsViaEligibility() throws Exception {
        // The full GlobalPay shape: require_clouds is the union of the workloads' clouds; only NY
        // carries all three, but DC/LD/SG each carry enough to host at least one workload. Widened
        // eligibility puts EMEA and APAC metros into the pool, which is what lets the spread land there.
        FabricGateway fabric = gateway(List.of(
                metro("NY", "New York", Region.AMER, NY_LAT, NY_LON),
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("LD", "London", Region.EMEA, LD_LAT, LD_LON),
                metro("SG", "Singapore", Region.APAC, SG_LAT, SG_LON)),
                List.of(
                        profile("sp-aws", "AWS Direct Connect",
                                spm("NY", "us-east-1"), spm("DC", "us-east-1"), spm("LD", "eu-west-1")),
                        profile("sp-azr", "Azure ExpressRoute", spm("NY", "east-us"), spm("DC", "east-us")),
                        profile("sp-gcp", "Google Cloud Partner Interconnect",
                                spm("NY", "us-east1"), spm("SG", "asia-southeast1"))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.NY).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.AZURE).done()
                .requireProvider(CloudProviderType.GOOGLE_CLOUD).done()
                .addWorkload("Payments").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AWS).dependsOn(CloudProviderType.AZURE).done()
                .addWorkload("Fraud-ML").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.GOOGLE_CLOUD).done()
                .addWorkload("DR").type(WorkloadType.DISASTER_RECOVERY).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AWS).done()
                .constraints().redundancy(RedundancyTier.MULTI_REGION).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        Set<Region> regions = selectedRegions(result);
        assertTrue(regions.contains(Region.EMEA) || regions.contains(Region.APAC),
                "eligibility widened the pool so the spread reaches beyond AMER: " + codeRegions(result));
        assertTrue(regions.size() >= 2, "the set spans multiple regions: " + codeRegions(result));

        // The DR workload lands in a different region from the primary and where its AWS is reachable.
        String drMetro = placementMetro(result, "DR");
        assertTrue(drMetro.matches("DC|LD"), "DR needs AWS, available in DC and LD: " + drMetro);
    }

    // ══════════════════════════════════════════════
    //  Part B — required-cloud coverage is a real check
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a required cloud reachable only in a metro that fails eligibility raises REQUIRED_CLOUD_NOT_COVERED")
    void requiredCloudNotCoveredRaisesFinding() throws Exception {
        // require_clouds=[aws, gcp]; the only workload needs AWS. AWS is in DC/DA/SV (AMER); GCP is
        // only in TY, which carries neither AWS nor the workload's cloud, so TY never qualifies. GCP
        // therefore resolves (it exists) but no recommended metro reaches it: a coverage gap, not a
        // lookup miss.
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON),
                metro("SV", "Silicon Valley", Region.AMER, 37.3382, -121.8863),
                metro("TY", "Tokyo", Region.APAC, TY_LAT, TY_LON)),
                List.of(
                        profile("sp-aws", "AWS Direct Connect",
                                spm("DC", "us-east-1"), spm("DA", "us-east-1"), spm("SV", "us-west-2")),
                        profile("sp-gcp", "Google Cloud Partner Interconnect", spm("TY", "asia-northeast1"))));

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.GOOGLE_CLOUD).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AWS).done()
                .constraints().maxMetros(3).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertFalse(sortedCodes(result).contains("TY"),
                "TY carries neither AWS nor the workload's cloud, so it does not qualify: " + sortedCodes(result));

        RiskFinding coverage = finding(result, "REQUIRED_CLOUD_NOT_COVERED");
        assertEquals(RiskSeverity.HIGH, coverage.getSeverity());
        assertTrue(coverage.getDescription().contains("Google Cloud"), coverage.getDescription());
        assertTrue(coverage.getDescription().contains("coverage guarantee across the set"),
                coverage.getDescription());

        // GCP resolved (it exists in TY), so this is NOT reported as a lookup-miss PROVIDER_UNAVAILABLE.
        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .filter(f -> "PROVIDER_UNAVAILABLE".equals(f.getCategory()))
                        .noneMatch(f -> f.getDescription().contains("Google Cloud")),
                "GCP is available somewhere, so it is a coverage gap not a lookup miss: "
                        + result.getRiskAssessment().getFindings());
    }

    // ══════════════════════════════════════════════
    //  Part C — budget is reported against, not enforced
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("no budget set means no cap: within_budget is true and no BUDGET_EXCEEDED finding is raised")
    void budgetUnsetMeansNoCap() throws Exception {
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(10_000).done()
                .constraints().maxMetros(2).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertTrue(result.getCostEstimate().isWithinBudget(),
                "with no ceiling nothing can be over budget");
        assertTrue(result.getRiskAssessment().getFindings().stream()
                        .noneMatch(f -> "BUDGET_EXCEEDED".equals(f.getCategory())),
                "a null budget is a no-cap: nothing is checked and nothing is flagged: "
                        + result.getRiskAssessment().getFindings());
    }

    @Test
    @DisplayName("a budget the estimate exceeds sets within_budget=false and raises a BUDGET_EXCEEDED finding")
    void budgetExceededIsReported() throws Exception {
        FabricGateway fabric = gateway(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON),
                metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON)),
                List.of());

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(10_000).done()
                .constraints().monthlyBudget(1, 10).maxMetros(2).done() // a $10/mo ceiling cannot cover interconnects
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertFalse(result.getCostEstimate().isWithinBudget(), "the estimate exceeds the $10 ceiling");

        RiskFinding budget = finding(result, "BUDGET_EXCEEDED");
        assertEquals(RiskSeverity.MEDIUM, budget.getSeverity(),
                "the budget is reported against, not enforced, so an overrun is MEDIUM");
        assertTrue(budget.getDescription().contains("reporting check, not a filter"),
                "the finding states the budget was not enforced: " + budget.getDescription());
        assertTrue(budget.getDescription().contains("exceeds"), budget.getDescription());
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private static Set<Region> selectedRegions(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(MetroRecommendation::getRegion)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String codeRegions(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code() + "=" + r.getRegion())
                .collect(Collectors.joining(", "));
    }

    private static List<String> sortedCodes(OptimizationResult result) {
        return result.getRecommendations().stream()
                .map(r -> r.getMetroId().code())
                .sorted()
                .collect(Collectors.toList());
    }

    private static RiskFinding finding(OptimizationResult result, String category) {
        return result.getRiskAssessment().getFindings().stream()
                .filter(f -> category.equals(f.getCategory()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a " + category + " finding, got: "
                        + result.getRiskAssessment().getFindings()));
    }

    /** The metro code a workload was placed in, from the topology. */
    private static String placementMetro(OptimizationResult result, String workloadLabel) {
        return result.getTopology().getPlacements().stream()
                .filter(p -> workloadLabel.equals(p.getWorkloadLabel()))
                .map(WorkloadPlacement::getAssignedMetro)
                .map(MetroId::code)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no placement for '" + workloadLabel + "' in "
                        + result.getTopology().getPlacements()));
    }

    // ── stub builders ──

    private static FabricGateway gateway(List<Metro> metros, List<ServiceProfile> profiles) {
        Metros metrosClient = mock(Metros.class);
        when(metrosClient.list()).thenReturn(new PaginatedList<>(metros, null, null, null, null));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(profiles, null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metrosClient);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static Metro metro(String code, String name, Region region, double lat, double lon) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(new ArrayList<>());
        return m;
    }

    private static ServiceProfile profile(String uuid, String name, ServiceProfileMetro... metros) {
        ServiceProfile profile = mock(ServiceProfile.class);
        when(profile.getUuid()).thenReturn(uuid);
        when(profile.getName()).thenReturn(name);
        when(profile.metros()).thenReturn(List.of(metros));
        return profile;
    }

    private static ServiceProfileMetro spm(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
