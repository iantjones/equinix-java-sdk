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

package api.equinix.javasdk.design.value;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.tco.CostBreakdown;
import api.equinix.javasdk.design.value.tco.DeploymentArchetype;
import api.equinix.javasdk.design.value.tco.TcoCalculator;
import api.equinix.javasdk.design.value.tco.TcoComparison;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the 3-archetype {@link TcoCalculator} using the bundled reference card
 * (null Fabric). Scenario: 100 TB/mo egress from AWS, 10 Gbps, metro DC.
 *
 * <p>Expected (reference 2026-06):
 * public-internet = 0.09 × 100,000 = 9000; on-prem = transit 7500 + hw 300 +
 * xconn 250 + power 975 = 9025; Equinix = private egress 2000 + VC 350 +
 * DX port 1642.50 + cross-connect 300 = 4292.50.</p>
 */
class TcoComparisonTest {

    private TcoComparison run() {
        return TcoCalculator.builder(null) // default reference rate card (Fabric unused)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .compare();
    }

    @Test
    void comparesAllThreeArchetypes() {
        TcoComparison tco = run();
        assertEquals(3, tco.getBreakdowns().size());
        assertTrue(tco.getBreakdowns().stream().allMatch(CostBreakdown::isPriced));

        assertEquals(0, new BigDecimal("9000").compareTo(
                tco.breakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET).orElseThrow().getMonthlyTotal()));
        assertEquals(0, new BigDecimal("9025").compareTo(
                tco.breakdown(DeploymentArchetype.ON_PREM).orElseThrow().getMonthlyTotal()));
        assertEquals(0, new BigDecimal("4292.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
    }

    @Test
    void recommendsEquinixAndComputesSavings() {
        TcoComparison tco = run();
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
        assertEquals(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, tco.getBaseline());
        // 9000 − 4292.50 = 4707.50/mo; × 12 = 56490
        assertEquals(0, new BigDecimal("4707.50").compareTo(tco.getMonthlySavingsVsBaseline()));
        assertEquals(0, new BigDecimal("56490.00").compareTo(tco.getAnnualSavingsVsBaseline()));
        assertTrue(tco.toMarkdown().contains("recommended"));
    }

    @Test
    void honoursArchetypeSubsetAndOnPremOverride() {
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.EQUINIX_INTERCONNECT)
                .onPremTransitPerMbpsMonth(new BigDecimal("2.00")) // ignored: on-prem not requested
                .compare();

        assertEquals(2, tco.getBreakdowns().size());
        assertTrue(tco.breakdown(DeploymentArchetype.ON_PREM).isEmpty());
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended());
    }

    @Test
    void includesCloudRouterInInterconnectArchetype() {
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS)
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .includeCloudRouter("STANDARD")
                .compare();

        // Equinix monthly gains the reference FCR price (1200): 4292.50 + 1200 = 5492.50.
        assertEquals(0, new BigDecimal("5492.50").compareTo(
                tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, tco.getRecommended(), "still cheapest vs 9000 baseline");
    }
}
