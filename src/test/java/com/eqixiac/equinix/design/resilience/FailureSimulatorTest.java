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
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureSimulatorTest {

    private static FailureSimulator sim(ResilienceOptions options, String... metros) {
        return new FailureSimulator(DesignGraphBuilder.build(ResilienceFixtures.context(),
                MetroPortfolio.ofCodes(metros), options));
    }

    private static ScenarioOutcome run(FailureSimulator sim, String scenarioId) {
        for (FailureScenario s : ScenarioCatalog.singles(sim.graph())) {
            if (s.id().equals(scenarioId)) {
                return sim.simulate(s);
            }
        }
        for (FailureScenario s : ScenarioCatalog.pairs(sim.graph(), 10_000).scenarios()) {
            if (s.id().equals(scenarioId)) {
                return sim.simulate(s);
            }
        }
        throw new AssertionError("no scenario " + scenarioId);
    }

    private static WorkloadOutcome workload(ScenarioOutcome o, String label) {
        return o.workloads().stream().filter(w -> w.workload().equals(label)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("baseline: both workloads in DC, full service, paths and demand stated")
    void baseline() {
        ScenarioOutcome b = sim(null, "DC", "NY").baseline();
        assertEquals(1.0, b.serviceIndex(), 1e-9);
        WorkloadOutcome api = workload(b, "api");
        assertEquals("DC", api.activeMetro());
        assertEquals(WorkloadStatus.FULL, api.status());
        assertEquals("HQ > DC (0.5 ms)", api.sitePaths().get("HQ"));
        assertTrue(api.cloudPaths().get("aws").startsWith("via conn:aws@DC"), api.cloudPaths().toString());
        assertEquals(2000.0, b.demandMbps(), 1e-9, "api 1000 to aws, batch 500 to aws and 500 to azure");
        assertEquals(1500.0, b.connectionLoadMbps().get("conn:aws@DC"), 1e-9);
    }

    @Nested
    @DisplayName("every scenario type")
    class ScenarioTypes {

        @Test
        @DisplayName("metro loss: workloads fail over to their standby metro and survive")
        void metro() {
            ScenarioOutcome o = run(sim(null, "DC", "NY"), "metro:DC");
            assertEquals(Verdict.SURVIVES, o.verdict());
            WorkloadOutcome api = workload(o, "api");
            assertEquals("NY", api.activeMetro());
            assertTrue(api.failedOver());
            assertEquals("HQ > NY (6.3 ms)", api.sitePaths().get("HQ"));
            assertEquals(List.of("metro:DC", "router:DC", "onramp:aws@DC", "onramp:azure@DC"),
                    o.scenario().failedNodes());
        }

        @Test
        @DisplayName("metro loss in a one-metro design is an outage")
        void metroSingle() {
            ScenarioOutcome o = run(sim(null, "DC"), "metro:DC");
            assertEquals(Verdict.OUTAGE, o.verdict());
            assertEquals(List.of("api", "batch"), o.workloadsDown());
            assertEquals(1.0, o.siteWeightDisconnected(), 1e-9);
            assertEquals(0.0, o.relativeSurvival(), 1e-9);
        }

        @Test
        @DisplayName("on-ramp loss: cloud traffic crosses the backbone to the other metro's connection")
        void onramp() {
            ScenarioOutcome o = run(sim(null, "DC", "NY"), "onramp:aws@DC");
            assertEquals(Verdict.SURVIVES, o.verdict());
            WorkloadOutcome api = workload(o, "api");
            assertEquals("DC", api.activeMetro(), "the workload stays; only its cloud path moves");
            assertEquals("via conn:aws@NY: DC > NY > on-ramp NY > aws (6.3 ms to the on-ramp)", api.cloudPaths().get("aws"));
        }

        @Test
        @DisplayName("single connection loss: a cloud connection, an access circuit, a backbone link")
        void connections() {
            FailureSimulator s = sim(null, "DC", "NY");
            assertEquals(Verdict.SURVIVES, run(s, "conn:aws@DC").verdict());
            ScenarioOutcome access = run(s, "access:HQ@DC");
            assertEquals(ScenarioKind.CONNECTION, access.scenario().kind());
            assertTrue(access.verdict() == Verdict.SURVIVES, access.verdict().name());
            assertEquals(Verdict.UNAFFECTED, run(s, "backbone:DC-NY").verdict(),
                    "every site reaches DC directly, and the clouds are local");
        }

        @Test
        @DisplayName("Cloud Router loss: the host keeps its sites but loses its clouds, so the workload fails over")
        void router() {
            ScenarioOutcome o = run(sim(null, "DC", "NY"), "router:DC");
            assertEquals(Verdict.SURVIVES, o.verdict());
            assertEquals("NY", workload(o, "batch").activeMetro());
            assertEquals(Verdict.OUTAGE, run(sim(null, "DC"), "router:DC").verdict());
        }

        @Test
        @DisplayName("cloud region loss: required cloud lost everywhere, an outage nothing in the design fixes")
        void cloudRegion() {
            ScenarioOutcome o = run(sim(null, "DC", "NY"), "cloud-region:AWS/us-east-1");
            assertEquals(ScenarioKind.CLOUD_REGION, o.scenario().kind());
            assertEquals(Verdict.OUTAGE, o.verdict());
            assertEquals(List.of("aws"), o.requiredCloudsAffected());
            assertEquals(WorkloadStatus.DOWN, workload(o, "api").status());
        }

        @Test
        @DisplayName("Equinix region loss takes every metro in it")
        void equinixRegion() {
            ScenarioOutcome o = run(sim(null, "DC", "NY"), "equinix-region:AMER");
            assertEquals(Verdict.OUTAGE, o.verdict());
            assertEquals(1.0, o.siteWeightDisconnected(), 1e-9);
        }

        @Test
        @DisplayName("pairs: both metros fail together; subsumed pairs (a router and its own metro) are skipped")
        void pairs() {
            FailureSimulator s = sim(null, "DC", "NY");
            ScenarioOutcome both = run(s, "pair:metro:DC+metro:NY");
            assertEquals(Verdict.OUTAGE, both.verdict());
            assertEquals(List.of("metro:DC", "metro:NY"), both.scenario().components());
            ScenarioCatalog.Pairs pairs = ScenarioCatalog.pairs(s.graph(), 10_000);
            assertTrue(pairs.exhaustive());
            assertFalse(pairs.scenarios().stream().anyMatch(p -> p.id().equals("pair:metro:DC+router:DC")));
            ScenarioCatalog.Pairs bounded = ScenarioCatalog.pairs(s.graph(), 5);
            assertEquals(5, bounded.scenarios().size());
            assertEquals(pairs.total(), bounded.total());
            assertFalse(bounded.exhaustive());
        }
    }

    @Nested
    @DisplayName("capacity-aware survival")
    class Capacity {

        @Test
        @DisplayName("standby connections at 40% of the bandwidth: the metro loss survives at 40%, not 'survives'")
        void partialStandby() {
            ScenarioOutcome o = run(sim(ResilienceOptions.defaults().withStandbyCapacityFraction(0.4), "DC", "NY"),
                    "metro:DC");
            assertEquals(Verdict.DEGRADED, o.verdict());
            assertEquals(0.4, o.relativeSurvival(), 1e-9);
            assertEquals(0.4, o.capacityShare(), 1e-9);
            assertEquals(2000.0, o.demandMbps(), 1e-9);
            assertEquals(1200.0, o.shortfallMbps(), 1e-9);
            assertEquals(WorkloadStatus.DEGRADED, workload(o, "api").status());
            assertTrue(workload(o, "api").cloudPaths().get("aws").endsWith("at 40% capacity"));
            assertTrue(o.workloadsDown().isEmpty());
        }

        @Test
        @DisplayName("an on-ramp loss that funnels everything onto an under-sized standby degrades")
        void funnel() {
            ScenarioOutcome o = run(sim(ResilienceOptions.defaults().withStandbyCapacityFraction(0.5), "DC", "NY"),
                    "onramp:aws@DC");
            assertEquals(Verdict.DEGRADED, o.verdict());
            assertEquals(1500.0, o.connectionLoadMbps().get("conn:aws@NY"), 1e-9);
            assertEquals(750.0, o.shortfallMbps(), 1e-9, "NY-aws carries 750 of 1500 Mbps");
        }
    }

    @Nested
    @DisplayName("limits that break after failover")
    class Limits {

        @Test
        @DisplayName("workload maxLatencyMs: the failover path crosses the backbone and breaks the limit")
        void workloadLimit() {
            DesignSpec spec = DesignSpec.builder().name("lat")
                    .site(SiteIntent.builder().label("HQ").metro("DC").build())
                    .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                    .workload(WorkloadIntent.builder().label("api").bandwidthMbps(100).maxLatencyMs(30.0)
                            .clouds(List.of("aws")).build())
                    .build();
            FakeDesignContext ctx = new FakeDesignContext(spec)
                    .metro("DC", Region.AMER).metro("NY", Region.AMER).metro("CH", Region.AMER)
                    .latency("DC", "HQ", 0.5).latency("NY", "HQ", 6.3).latency("CH", "HQ", 20.0)
                    .metroLatency("DC", "NY", 6.3).metroLatency("DC", "CH", 35.0).metroLatency("NY", "CH", 30.0)
                    .reachable("NY", "aws").reachable("CH", "aws");
            FailureSimulator s = new FailureSimulator(DesignGraphBuilder.build(ctx, MetroPortfolio.ofCodes("CH", "DC",
                    "NY"), ResilienceOptions.defaults().withAccessHoming(1)));
            assertTrue(s.baseline().constraintBreaks().isEmpty());
            assertEquals("HQ > DC > NY (6.8 ms)", workload(s.baseline(), "api").sitePaths().get("HQ"));

            ScenarioOutcome o = run(s, "metro:NY");
            assertEquals("CH", workload(o, "api").activeMetro());
            assertEquals(Verdict.DEGRADED, o.verdict());
            assertEquals(1, o.constraintBreaks().size());
            assertTrue(o.constraintBreaks().get(0).contains("35.5 ms from CH, over its 30.0 ms limit"),
                    o.constraintBreaks().get(0));
            assertEquals(1.0, o.siteWeightAffected(), 1e-9);
        }

        @Test
        @DisplayName("spec maxLatencyMs: a site's nearest surviving metro is over the limit")
        void siteLimit() {
            DesignSpec spec = ResilienceFixtures.specBuilder()
                    .constraints(DesignConstraints.builder().maxLatencyMs(5.0).build()).build();
            FailureSimulator s = new FailureSimulator(DesignGraphBuilder.build(ResilienceFixtures.context(spec),
                    MetroPortfolio.ofCodes("DC", "NY"), null));
            ScenarioOutcome o = run(s, "metro:NY");
            assertEquals(Verdict.DEGRADED, o.verdict());
            assertTrue(o.constraintBreaks().stream().anyMatch(b -> b.startsWith("site 'Branch'")), o.constraintBreaks()
                    .toString());
            assertEquals(0.25, o.siteWeightAffected(), 1e-9);
        }

        @Test
        @DisplayName("geo zones: a standby that leaves the allowed zones is a violation only after failover")
        void zones() {
            DesignSpec spec = DesignSpec.builder().name("eu")
                    .site(SiteIntent.builder().label("Paris").metro("PA").build())
                    .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                    .workload(WorkloadIntent.builder().label("app").bandwidthMbps(100).clouds(List.of("aws")).build())
                    .constraints(DesignConstraints.builder().geoZones(List.of("EU")).build())
                    .build();
            FakeDesignContext ctx = new FakeDesignContext(spec)
                    .metro("PA", Region.EMEA, "EU").metro("LD", Region.EMEA, "UK")
                    .latency("PA", "Paris", 0.5).latency("LD", "Paris", 8.0).metroLatency("PA", "LD", 8.0)
                    .reachable("PA", "aws").reachable("LD", "aws");
            FailureSimulator s = new FailureSimulator(DesignGraphBuilder.build(ctx, MetroPortfolio.ofCodes("LD", "PA"),
                    null));
            assertTrue(s.baseline().zoneViolations().isEmpty(), s.baseline().zoneViolations().toString());
            ScenarioOutcome o = run(s, "metro:PA");
            assertFalse(o.zoneViolations().isEmpty());
            assertTrue(o.zoneViolations().get(0).contains("uses LD (zones [UK]), outside [EU]"), o.zoneViolations()
                    .toString());
        }
    }

    @Test
    @DisplayName("deterministic: the same graph gives the same outcomes")
    void deterministic() {
        FailureSimulator a = sim(null, "DC", "NY");
        FailureSimulator b = sim(null, "DC", "NY");
        assertEquals(a.simulateAll(ScenarioCatalog.singles(a.graph())),
                b.simulateAll(ScenarioCatalog.singles(b.graph())));
    }
}
