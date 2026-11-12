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

package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.CorrelatedRisk;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Level;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Remediation;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResilienceAnalyzerTest {

    private static ResilienceReport report(ResilienceOptions options, String... metros) {
        return ResilienceAnalyzer.analyze(ResilienceFixtures.context(), MetroPortfolio.ofCodes(metros), options);
    }

    private static Remediation remediationFor(ResilienceReport r, String scenarioId, String startsWith) {
        return r.remediations().stream()
                .filter(m -> m.addresses().contains(scenarioId) && m.action().startsWith(startsWith))
                .findFirst().orElseThrow(() -> new AssertionError("no '" + startsWith + "' for " + scenarioId + " in "
                        + r.remediations()));
    }

    @Test
    @DisplayName("one metro: N-1 fails, single points of failure named, re-simulated remediation offered")
    void singleMetro() {
        ResilienceReport r = report(null, "DC");
        assertEquals(Level.FAIL, r.n1().level());
        List<String> spofs = r.singlePointsOfFailure().stream().map(ResilienceFindings.SinglePointOfFailure::scenarioId)
                .toList();
        assertTrue(spofs.containsAll(List.of("metro:DC", "router:DC", "onramp:aws@DC", "conn:aws@DC",
                "onramp:azure@DC", "conn:azure@DC", "access:HQ@DC", "access:Branch@DC")), spofs.toString());

        Remediation metro = remediationFor(r, "metro:DC", "Add metro NY");
        assertTrue(metro.verified(), "adding NY was re-simulated and removes the metro loss");
        assertTrue(metro.action().contains("6.3 ms from DC"));
        Remediation twin = remediationFor(r, "conn:aws@DC", "Add a second, redundant aws connection in DC");
        assertTrue(twin.verified());
        assertTrue(twin.action().contains("sized for 1500 Mbps"));
        assertTrue(remediationFor(r, "access:HQ@DC", "Add a second, diverse access circuit").verified());
    }

    @Test
    @DisplayName("two metros: N-1 passes; shared fate is reported but not counted; N-2 fails on the metro pair")
    void twoMetros() {
        ResilienceReport r = report(null, "DC", "NY");
        assertEquals(Level.PASS, r.n1().level(), r.n1().statement());
        assertTrue(r.singlePointsOfFailure().isEmpty());
        assertEquals(Level.FAIL, r.n2().level());
        assertTrue(r.pairs().exhaustive());
        assertEquals(r.pairs().total(), r.n2().scenarios());
        assertTrue(r.pairs().worst().stream().anyMatch(p -> p.scenarioId().equals("pair:metro:DC+metro:NY")
                && p.verdict() == Verdict.OUTAGE), r.pairs().worst().toString());

        // The blast radius is led by the shared-fate events (whole cloud region, whole Equinix region).
        assertEquals(1.0, r.blastRadius().get(0).impact(), 1e-9);
        assertTrue(r.blastRadius().stream().limit(3).anyMatch(s -> s.scenarioId().equals("equinix-region:AMER")));
        Remediation region = remediationFor(r, "equinix-region:AMER", "No metro outside the failure reaches aws, azure");
        assertFalse(region.verified(), "LD (EMEA) reaches aws but not azure: honestly not fixable by adding a metro");
    }

    @Test
    @DisplayName("bounded pairs are stated: evaluated of total")
    void boundedPairs() {
        ResilienceReport r = report(ResilienceOptions.defaults().withMaxPairScenarios(7), "DC", "NY");
        assertFalse(r.pairs().exhaustive());
        assertEquals(7, r.pairs().evaluated());
        assertTrue(r.n2().statement().contains("7 of " + r.pairs().total() + " (bounded"), r.n2().statement());
    }

    @Test
    @DisplayName("capacity shortfall gets a concrete re-simulated 'raise to' remediation")
    void capacityRemediation() {
        ResilienceReport r = report(ResilienceOptions.defaults().withStandbyCapacityFraction(0.4), "DC", "NY");
        assertEquals(Level.DEGRADED, r.n1().level());
        Remediation raise = remediationFor(r, "metro:DC", "Raise the aws@NY connection from 600 to 1500 Mbps and the "
                + "azure@NY connection from 200 to 500 Mbps");
        assertTrue(raise.verified(), "both raises together were re-simulated and remove the shortfall");
        assertTrue(raise.addresses().contains("router:DC"));
        assertTrue(remediationFor(r, "onramp:aws@DC", "Raise the aws@NY connection from 600 to 1500 Mbps").verified());
        assertFalse(r.remediations().stream().anyMatch(m -> m.action().startsWith("Add metro")),
                "a capacity degradation is not answered with more metros");
    }

    @Test
    @DisplayName("correlated risks: primary and standby in one region, connections in one region, access in one region")
    void correlated() {
        ResilienceReport r = report(null, "DC", "NY");
        List<CorrelatedRisk> risks = r.correlatedRisks();
        assertTrue(risks.stream().anyMatch(c -> c.description().equals("workload 'api': primary DC and standby NY are "
                + "both in AMER") && c.severity() == ResilienceFindings.Severity.MEDIUM
                && "equinix-region:AMER".equals(c.scenarioId())), risks.toString());
        assertTrue(risks.stream().anyMatch(c -> c.description().startsWith("every connection to aws (DC, NY)")));
        assertTrue(risks.stream().anyMatch(c -> c.description().startsWith("site 'HQ': both access circuits")));

        // Spreading the design across regions removes the region-level correlation for aws.
        DesignSpec spec = ResilienceFixtures.specBuilder().clearWorkloads()
                .workload(com.eqixiac.equinix.design.spec.WorkloadIntent.builder().label("api").bandwidthMbps(100)
                        .clouds(List.of("aws")).build()).build();
        ResilienceReport spread = ResilienceAnalyzer.analyze(ResilienceFixtures.context(spec),
                MetroPortfolio.ofCodes("DC", "LD"), null);
        assertFalse(spread.correlatedRisks().stream().anyMatch(c -> c.description().startsWith("every connection to aws")),
                spread.correlatedRisks().toString());
    }

    @Test
    @DisplayName("Markdown and JSON: every section, the spec fingerprint, no NaN")
    void rendering() throws Exception {
        ResilienceReport r = report(null, "DC", "NY");
        String md = r.toMarkdown();
        for (String section : List.of("## Single failures", "## Blast radius", "## Single points of failure",
                "## Correlated risks", "## Pairs (N-2)", "## Availability estimate", "## Remediation",
                "```mermaid", "## Assumptions")) {
            assertTrue(md.contains(section), section);
        }
        assertTrue(md.contains(ResilienceFixtures.spec().fingerprint()));
        assertTrue(md.contains("not guarantees"));
        String json = r.toJson();
        assertFalse(json.contains("NaN"));
        JsonNode tree = ResilienceJson.mapper().readTree(json);
        assertEquals(r.specFingerprint(), tree.get("specFingerprint").asText());
        assertEquals("PASS", tree.get("n1").get("level").asText());
        assertTrue(r.toSummary().contains("N-1 passes"), r.toSummary());
    }

    @Test
    @DisplayName("offline forContext over a CatalogDesignContext runs the optimiser and analyses its recommendation")
    void offlineForContext() {
        DesignSpec spec = ResilienceFixtures.specBuilder().constraints(
                com.eqixiac.equinix.design.spec.DesignConstraints.builder().minMetros(2).maxMetros(2).build()).build();
        CatalogDesignContext ctx = CatalogDesignContext.builder(spec)
                .metro("DC", Region.AMER, 38.9, -77.4, "US").metro("NY", Region.AMER, 40.7, -74.0, "US")
                .metro("LD", Region.EMEA, 51.5, -0.1, "UK")
                .measuredLatency("DC", "NY", 6.3).measuredLatency("DC", "LD", 73.4)
                .reachable("DC", "aws", "azure").reachable("NY", "aws", "azure").reachable("LD", "aws")
                .build();
        ResilienceReport r = ResilienceAnalyzer.forContext(ctx).run();
        assertEquals("DC+NY", r.portfolio());
        assertEquals("the Metro Optimizer's recommendation", r.portfolioSource());
        assertEquals(spec.fingerprint(), r.specFingerprint());
        assertTrue(r.timingsMs().containsKey("optimise"));
        ResilienceReport given = ResilienceAnalyzer.forContext(ctx).portfolio(MetroPortfolio.ofCodes("DC")).run();
        assertEquals("given", given.portfolioSource());
        assertEquals(Level.FAIL, given.n1().level());
    }
}
