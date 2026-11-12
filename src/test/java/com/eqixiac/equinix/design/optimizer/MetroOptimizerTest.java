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
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetroOptimizerTest {

    private final FabricGateway fabric = OptimizerFixtures.standard();

    private static DesignSpec.DesignSpecBuilder base() {
        return DesignSpec.builder().name("payments")
                .site(SiteIntent.builder().label("HQ").metro("NY").headcount(400).build())
                .site(SiteIntent.builder().label("London").metro("LD").headcount(100).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1")
                        .monthlyEgressGb(10_000d).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build());
    }

    private OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric, spec).rateCard(ReferenceRateCard.standard()).run();
    }

    @Test
    @DisplayName("forSpec: the result carries the spec, its fingerprint, the frontier, verdicts, statistics and sensitivity")
    void forSpecResult() {
        DesignSpec spec = base().build();
        OptimizationResult result = run(spec);
        assertEquals(spec, result.getSpec());
        assertEquals(spec.fingerprint(), result.getSpecFingerprint());
        DesignOption rec = result.getRecommended();
        assertNotNull(rec);
        assertTrue(rec.paretoOptimal());
        assertTrue(result.getFrontier().stream().anyMatch(o -> o.portfolio().equals(rec.portfolio())));
        assertTrue(result.getConstraintVerdicts().stream().allMatch(ConstraintVerdict::satisfied));
        assertEquals(5, result.getSolveStatistics().candidates());
        assertTrue(result.getSolveStatistics().evaluated() > 5);
        assertTrue(result.getSolveStatistics().exact());
        assertNotNull(result.getSensitivity());
        assertEquals(5, result.getSensitivity().weightRanges().size());
        assertTrue(result.getLatencyCalibration().isCalibrated());
        assertEquals(rec.portfolio().size(), result.getRecommendations().size());
    }

    @Test
    @DisplayName("one cost truth: the objective, the cost estimate and the option's lines agree")
    void oneCostTruth() {
        OptimizationResult result = run(base().build());
        DesignOption rec = result.getRecommended();
        BigDecimal total = rec.cost().monthlyTotal().orElseThrow();
        assertEquals(total.doubleValue(), rec.values().get(Objective.COST).value(), 1e-9);
        assertEquals(0, total.compareTo(result.getCostEstimate().getMonthlyTotal()));
        assertEquals("USD", result.getCostEstimate().getCurrency());
    }

    @Test
    @DisplayName("sovereignty: geoZones keeps every metro inside the live geo scopes")
    void sovereignty() {
        OptimizationResult result = run(base().constraints(DesignConstraints.builder()
                .geoZones(List.of("EU", "UK")).build()).build());
        for (MetroId metro : result.getRecommended().portfolio().metros()) {
            assertTrue(List.of("LD", "FR").contains(metro.code()), metro.code());
        }
        assertTrue(result.getSolverNotes().stream().anyMatch(n -> n.contains("ruled out by a constraint")));
    }

    @Test
    @DisplayName("two regions with a required cloud: MULTI_REGION is an explicit rule and Azure must be reachable")
    void twoRegionsWithCloud() {
        DesignSpec spec = base().cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).build())
                .workload(WorkloadIntent.builder().label("erp").bandwidthMbps(500).clouds(List.of("azure")).build())
                .constraints(DesignConstraints.builder().minimumRedundancy(RedundancyTier.MULTI_REGION).build()).build();
        OptimizationResult result = run(spec);
        List<String> codes = result.getRecommended().portfolio().metros().stream().map(MetroId::code)
                .collect(Collectors.toList());
        assertTrue(result.getRecommended().portfolio().size() >= 2);
        assertTrue(codes.stream().anyMatch(c -> List.of("DC", "NY", "SV").contains(c))
                && codes.stream().anyMatch(c -> List.of("LD", "FR").contains(c)), codes.toString());
        assertTrue(codes.contains("DC") || codes.contains("LD"), "azure is only in DC and LD: " + codes);
        assertNotNull(result.getPlacement().placement("erp").orElseThrow().primary());
        // The wizard reads placements by metro.
        MetroId erpMetro = result.getPlacement().placement("erp").orElseThrow().primary();
        assertTrue(result.getTopology().forMetro(erpMetro).stream().anyMatch(p -> p.getWorkloadLabel().equals("erp")));
        assertTrue(result.getRequest().getProviders().stream().anyMatch(p -> p.getLabel().equals("azure")));
    }

    @Test
    @DisplayName("a USD budget below every design leaves nothing feasible and the result says why")
    void infeasible() {
        OptimizationResult result = run(base().commercial(CommercialTerms.builder()
                .budgetMaxMonthly(new BigDecimal("1")).build()).build());
        assertEquals(null, result.getRecommended());
        assertTrue(result.getRecommendations().isEmpty());
        assertTrue(result.toSummary().contains("No viable design"), result.toSummary());
        assertTrue(result.getSolverNotes().stream().anyMatch(n -> n.contains("budget")), result.getSolverNotes().toString());
    }

    @Test
    @DisplayName("markdown and JSON renderings")
    void rendering() throws Exception {
        OptimizationResult result = run(base().build());
        String md = result.toMarkdown();
        for (String section : List.of("## Recommendation and why", "## Trade-off frontier", "## Sensitivity",
                "## Constraints", "## Workload placement", "## Cost", "## Method and assumptions", "## Data provenance")) {
            assertTrue(md.contains(section), section + " missing:\n" + md);
        }
        assertTrue(md.contains("(recommended)"));
        JsonNode json = new ObjectMapper().readTree(result.toJson());
        assertEquals(result.getSpecFingerprint(), json.get("specFingerprint").asText());
        assertTrue(json.has("frontier") && json.has("sensitivity") && json.has("latencyCalibration"));
        assertTrue(result.toSummary().contains("Searched"), result.toSummary());
    }

    @Test
    @DisplayName("the fluent builder is a thin convenience that builds a DesignSpec")
    void builderBuildsSpec() {
        MetroOptimizer.Builder builder = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro("NY").headcount(400).done()
                .addWorkload("api").bandwidthMbps(1000).dependsOn(CloudProviderType.AWS).done()
                .constraints().maxMetros(2).geoZones("CONUS").done();
        DesignSpec spec = builder.toSpec();
        assertEquals(1, spec.clouds().size());
        assertEquals(List.of(spec.clouds().get(0).id()), spec.workloads().get(0).clouds());
        assertEquals(List.of("CONUS"), spec.constraints().geoZones());
        OptimizationResult result = builder.rateCard(ReferenceRateCard.standard()).optimize();
        assertEquals(spec.fingerprint(), result.getSpecFingerprint());
        assertThrows(IllegalArgumentException.class,
                () -> MetroOptimizer.builder(fabric).addWorkload("Edge").maxLatencyToleranceMs(0));
    }

    @Test
    @DisplayName("an invalid spec is refused before any data is read")
    void invalidSpec() {
        assertThrows(IllegalArgumentException.class, () -> run(DesignSpec.builder().name("x").build()));
    }

    @Test
    @DisplayName("a single-metro answer is flagged as a single point of failure")
    void singleMetroFinding() {
        OptimizationResult result = run(base().constraints(DesignConstraints.builder().maxMetros(1).build()).build());
        List<String> categories = result.getRiskAssessment().getFindings().stream().map(RiskFinding::getCategory)
                .collect(Collectors.toList());
        assertTrue(categories.contains("SINGLE_POINT_OF_FAILURE"), categories.toString());
        assertEquals(Region.AMER, result.primaryMetro().getRegion());
    }

    @Test
    @DisplayName("a workload on two clouds with a native multicloud environment raises the informational finding")
    void nativeMulticloudFinding() {
        DesignSpec spec = base().cloud(CloudIntent.builder().id("gcp").provider(CloudProviderType.GOOGLE_CLOUD)
                        .region("us-east4").required(false).build())
                .workload(WorkloadIntent.builder().label("analytics").bandwidthMbps(1000)
                        .clouds(List.of("aws", "gcp")).build())
                .build();
        RiskFinding finding = run(spec).getRiskAssessment().getFindings().stream()
                .filter(f -> f.getCategory().equals("NATIVE_MULTICLOUD_ALTERNATIVE")).findFirst().orElseThrow();
        assertEquals(com.eqixiac.equinix.design.optimizer.enums.RiskSeverity.INFO, finding.getSeverity());
        assertTrue(finding.getDescription().contains("'analytics'"), finding.getDescription());
        assertTrue(finding.getDescription().contains("matches the regions in this spec"), finding.getDescription());
    }

    @Test
    @DisplayName("a budget in another currency is compared after converting the prices into it")
    void budgetCurrency() {
        OptimizationResult tight = run(base().commercial(CommercialTerms.builder().currency("EUR")
                .budgetMaxMonthly(new BigDecimal("1")).build()).build());
        assertTrue(tight.getRecommended() == null, "every design costs more than EUR 1 once converted");
        OptimizationResult ample = run(base().commercial(CommercialTerms.builder().currency("EUR")
                .budgetMaxMonthly(new BigDecimal("1000000")).build()).build());
        assertNotNull(ample.getRecommended());
        assertTrue(ample.getConstraintVerdicts().stream().anyMatch(v -> v.constraintId().equals("budget")
                && v.reason().startsWith("EUR ")), ample.getConstraintVerdicts().toString());
    }
}

