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
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.LatencyMatrix;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The latency semantics the 2026-07-24 sentinel regression established, restated for the spec-first
 * optimizer: every latency is a round trip; an unknown latency is never a number (no
 * {@code Double.MAX_VALUE}, no fixed guess); a latency bound that cannot be evaluated is reported as
 * not applied rather than silently passing or failing.
 */
@DisplayName("MetroOptimizer latency semantics (round trip, unknown is never a number)")
class MetroOptimizerLatencySentinelTest {

    private static final Pattern MS_FIGURE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ms");

    private final FabricGateway fabric = OptimizerFixtures.standard();

    private OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric, spec).rateCard(ReferenceRateCard.standard()).run();
    }

    private static DesignSpec.DesignSpecBuilder workload() {
        return DesignSpec.builder().name("sentinel")
                .workload(WorkloadIntent.builder().label("Web Tier").bandwidthMbps(1000).build());
    }

    @Test
    @DisplayName("no sites + maxLatencyMs: the bound is reported as NOT applied, latency is unknown, no sentinel")
    void noSitesWithBound() {
        OptimizationResult result = run(workload().constraints(DesignConstraints.builder().maxLatencyMs(20.0).build())
                .build());
        assertFalse(result.getRecommendations().isEmpty(), "an unevaluable bound excludes nothing");
        RiskFinding notApplied = result.getRiskAssessment().getFindings().stream()
                .filter(f -> f.getCategory().equals("LATENCY_BOUND_NOT_EVALUATED")).findFirst().orElseThrow();
        assertTrue(notApplied.getDescription().contains("NOT applied"), notApplied.getDescription());
        assertFalse(result.getRecommended().values().get(Objective.LATENCY).known());
        assertTrue(Double.isNaN(result.getRecommended().values().get(Objective.LATENCY).value()));
        assertTrue(result.getExplanation().getHumanReadable().contains("could not be evaluated"));
        LatencyMatrix matrix = result.getLatencyMatrix();
        assertFalse(matrix.hasSites());
        for (MetroId metro : matrix.getMetros()) {
            assertTrue(matrix.worstCaseMs(metro).isEmpty());
        }
        assertSane(result);
    }

    @Test
    @DisplayName("with sites the bound is applied to each site's nearest metro, with measured round trips")
    void withSites() {
        OptimizationResult result = run(workload().site(SiteIntent.builder().label("HQ").metro("DC").build())
                .constraints(DesignConstraints.builder().maxLatencyMs(5.0).requiredMetros(List.of("SV")).build())
                .build());
        // HQ is 0.5 ms (intra-metro) from DC, so {DC, SV} satisfies the bound even though SV is 65.8 ms away.
        assertTrue(result.getRecommended().portfolio().contains(MetroId.of("DC")));
        assertTrue(result.getRecommended().portfolio().contains(MetroId.of("SV")));
        assertEquals(65.8, result.getLatencyMatrix().worstCaseMs(MetroId.of("SV")).orElseThrow(), 1e-9);
        assertFalse(result.getLatencyMatrix().get(MetroId.of("SV"), "HQ").orElseThrow().isEstimated(),
                "SV-DC is a Fabric measurement");
        assertTrue(result.getRiskAssessment().getFindings().stream()
                .noneMatch(f -> f.getCategory().equals("LATENCY_BOUND_NOT_EVALUATED")));
        assertSane(result);
    }

    @Test
    @DisplayName("a site with no metro and no coordinates has unknown latency: absent, never a number")
    void unknownSite() {
        OptimizationResult result = run(workload().site(SiteIntent.builder().label("Nowhere").build())
                .site(SiteIntent.builder().label("HQ").metro("NY").build()).build());
        for (MetroRecommendation rec : result.getRecommendations()) {
            assertEquals(null, rec.getSiteLatencies().get("Nowhere"));
        }
        assertTrue(result.getRecommended().values().get(Objective.LATENCY).explanation().contains("unknown for Nowhere"));
        assertSane(result);
    }

    private static void assertSane(OptimizationResult result) {
        List<String> emitted = new ArrayList<>();
        emitted.add(result.toSummary());
        emitted.add(result.toMarkdown());
        emitted.add(result.toJson());
        for (String text : emitted) {
            assertFalse(text.contains("7976931348623157"), "no Double.MAX_VALUE sentinel");
            assertFalse(text.contains("Infinity"), "no infinite latency");
            Matcher m = MS_FIGURE.matcher(text);
            while (m.find()) {
                assertTrue(Double.parseDouble(m.group(1)) < 1_000_000, m.group());
            }
        }
    }
}
