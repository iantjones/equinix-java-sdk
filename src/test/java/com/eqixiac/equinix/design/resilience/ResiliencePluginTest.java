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
import com.eqixiac.equinix.design.objective.ResilienceObjective;
import com.eqixiac.equinix.design.solve.ExhaustiveSolver;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.DesignPlugin;
import com.eqixiac.equinix.design.spi.DesignSolution;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.SolveProblem;
import com.eqixiac.equinix.design.sustainability.SustainabilityPlugin;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResiliencePluginTest {

    @Test
    @DisplayName("simulated_resilience: 0 for one metro, 100 with a full standby, 40 with a 40% standby")
    void objective() {
        FakeDesignContext ctx = ResilienceFixtures.context();
        SimulatedResilienceObjective full = new SimulatedResilienceObjective();
        assertEquals(0.0, full.evaluate(MetroPortfolio.ofCodes("DC"), ctx).value(), 1e-9);
        ObjectiveValue two = full.evaluate(MetroPortfolio.ofCodes("DC", "NY"), ctx);
        assertEquals(100.0, two.value(), 1e-9);
        assertTrue(two.explanation().contains("single-element failures"), two.explanation());

        SimulatedResilienceObjective partial = new SimulatedResilienceObjective(
                ResilienceOptions.defaults().withStandbyCapacityFraction(0.4), false);
        ObjectiveValue v = partial.evaluate(MetroPortfolio.ofCodes("DC", "NY"), ctx);
        assertEquals(40.0, v.value(), 1e-9, "survives the metro loss, but at 40% of the bandwidth");
        assertTrue(v.explanation().contains("loss of metro DC"), v.explanation());
    }

    @Test
    @DisplayName("richer than the built-in resilience objective: it sees standby capacity, which metro counting cannot")
    void richerThanBuiltIn() {
        FakeDesignContext ctx = ResilienceFixtures.context();
        MetroPortfolio dcNy = MetroPortfolio.ofCodes("DC", "NY");
        assertEquals(100.0, new ResilienceObjective().evaluate(dcNy, ctx).value(), 1e-9,
                "the built-in objective sees every workload keep a metro");
        assertEquals(40.0, new SimulatedResilienceObjective(ResilienceOptions.defaults()
                .withStandbyCapacityFraction(0.4), false).evaluate(dcNy, ctx).value(), 1e-9);
    }

    @Test
    @DisplayName("through the plug-in, the objective is only simulated when the spec weights it")
    void onlyWhenWeighted() {
        DesignPlugin plugin = new ResiliencePlugin();
        var objective = plugin.objectives().get(0);
        assertEquals(SimulatedResilienceObjective.ID, objective.id());
        ObjectiveValue unweighted = objective.evaluate(MetroPortfolio.ofCodes("DC", "NY"), ResilienceFixtures.context());
        assertFalse(unweighted.known());
        DesignSpec weighted = ResilienceFixtures.specBuilder()
                .objectives(new DesignObjectives(null, Map.of(SimulatedResilienceObjective.ID, 1.0))).build();
        assertTrue(objective.evaluate(MetroPortfolio.ofCodes("DC", "NY"), ResilienceFixtures.context(weighted)).known());
    }

    @Test
    @DisplayName("the solver ranks by simulated survival and min_survival removes portfolios that fall below it")
    void solver() {
        DesignSpec spec = ResilienceFixtures.specBuilder()
                .objectives(new DesignObjectives(null, Map.of(SimulatedResilienceObjective.ID, 1.0))).build();
        FakeDesignContext ctx = ResilienceFixtures.context(spec);
        ResiliencePlugin plugin = new ResiliencePlugin(ResilienceOptions.defaults(), 75.0);
        SolveProblem problem = new SolveProblem(ctx, ctx.metros(), plugin.objectives(), plugin.constraints(),
                Map.of(SimulatedResilienceObjective.ID, 1.0), 1, 2);
        DesignSolution solution = new ExhaustiveSolver().solve(problem);
        assertTrue(solution.isFeasible());
        assertEquals("DC+NY", solution.recommended().portfolio().key());
        for (DesignOption option : solution.frontier()) {
            assertTrue(option.portfolio().size() == 2, option.portfolio().key());
        }
        assertEquals(1, solution.feasible(), "only DC+NY gives both workloads a standby that keeps 75%");
    }

    @Test
    @DisplayName("min_survival: inert without a threshold, opted into by the spec label, malformed label fails")
    void minSurvival() {
        MinSurvivalConstraint c = new MinSurvivalConstraint();
        assertTrue(c.check(MetroPortfolio.ofCodes("DC"), ResilienceFixtures.context()).satisfied());

        DesignSpec opted = ResilienceFixtures.specBuilder().label(MinSurvivalConstraint.LABEL, "60").build();
        ConstraintVerdict single = c.check(MetroPortfolio.ofCodes("DC"), ResilienceFixtures.context(opted));
        assertFalse(single.satisfied());
        assertTrue(single.reason().contains("below the 60% minimum") && single.reason().contains("loss of metro DC"), single.reason());
        assertTrue(c.check(MetroPortfolio.ofCodes("DC", "NY"), ResilienceFixtures.context(opted)).satisfied());

        DesignSpec bad = ResilienceFixtures.specBuilder().label(MinSurvivalConstraint.LABEL, "most").build();
        assertFalse(c.check(MetroPortfolio.ofCodes("DC", "NY"), ResilienceFixtures.context(bad)).satisfied());
    }

    @Test
    @DisplayName("geo_zones_failover: a standby outside the zones fails, naming the scenario; inert without zones")
    void failoverZones() {
        DesignSpec spec = DesignSpec.builder().name("eu")
                .site(SiteIntent.builder().label("Paris").metro("PA").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .workload(WorkloadIntent.builder().label("app").bandwidthMbps(100).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().geoZones(List.of("EU")).build())
                .build();
        FakeDesignContext ctx = new FakeDesignContext(spec)
                .metro("PA", Region.EMEA, "EU").metro("FR", Region.EMEA, "EU").metro("LD", Region.EMEA, "UK")
                .latency("PA", "Paris", 0.5).latency("FR", "Paris", 9.0).latency("LD", "Paris", 8.0)
                .metroLatency("PA", "LD", 8.0).metroLatency("PA", "FR", 9.0).metroLatency("FR", "LD", 12.0)
                .reachable("PA", "aws").reachable("FR", "aws").reachable("LD", "aws");
        FailoverGeoZonesConstraint c = new FailoverGeoZonesConstraint();

        ConstraintVerdict leaves = c.check(MetroPortfolio.ofCodes("LD", "PA"), ctx);
        assertFalse(leaves.satisfied());
        assertTrue(leaves.reason().contains("after loss of metro PA"), leaves.reason());
        assertTrue(c.check(MetroPortfolio.ofCodes("FR", "PA"), ctx).satisfied());

        FakeDesignContext noZones = ResilienceFixtures.context();
        assertTrue(c.check(MetroPortfolio.ofCodes("DC", "LD"), noZones).satisfied());
    }

    @Test
    @DisplayName("both plug-ins are registered for DesignPlugin.discover()")
    void discovered() {
        List<String> names = DesignPlugin.discover().stream().map(DesignPlugin::name).toList();
        assertTrue(names.contains("resilience"), names.toString());
        assertTrue(names.contains("sustainability"), names.toString());
        assertTrue(DesignPlugin.discover().stream().anyMatch(p -> p instanceof SustainabilityPlugin));
    }
}
