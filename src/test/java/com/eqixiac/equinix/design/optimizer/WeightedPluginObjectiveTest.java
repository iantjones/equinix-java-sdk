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
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.sustainability.CarbonIntensityTable;
import com.eqixiac.equinix.design.sustainability.CarbonObjective;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * A spec that weights a plug-in objective gets it from the plug-ins on the classpath; a spec that does not
 * is unaffected.
 */
class WeightedPluginObjectiveTest {

    private static FabricGateway fabric() {
        Metro dc = OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                List.of(GeoScopeType.CONUS), OptimizerFixtures.link("PA", 80.0));
        when(dc.getCountry()).thenReturn("US");
        Metro pa = OptimizerFixtures.metro("PA", "Paris", Region.EMEA, 48.8566, 2.3522, List.of(GeoScopeType.EU),
                OptimizerFixtures.link("DC", 80.0));
        when(pa.getCountry()).thenReturn("FR");
        return OptimizerFixtures.gateway(List.of(dc, pa),
                List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "PA:eu-west-3")));
    }

    private static DesignSpec.DesignSpecBuilder base() {
        return DesignSpec.builder().name("carbon")
                .site(SiteIntent.builder().label("HQ").metro("DC").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().maxMetros(1).build());
    }

    private static OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric(), spec).rateCard(ReferenceRateCard.standard()).run();
    }

    @Test
    @DisplayName("an unweighted spec has no carbon objective in its result")
    void absentWhenNotWeighted() {
        OptimizationResult result = run(base().build());
        assertFalse(result.getRecommended().values().containsKey(CarbonObjective.ID));
    }

    @Test
    @DisplayName("weighting carbon adds it from the bundled reference table and it can decide the design")
    void presentWhenWeighted() {
        OptimizationResult result = run(base()
                .objectives(new DesignObjectives(null, Map.of(CarbonObjective.ID, 5.0))).build());

        ObjectiveValue carbon = result.getRecommended().values().get(CarbonObjective.ID);
        assertTrue(carbon != null && carbon.known(), String.valueOf(carbon));
        // France's grid is far cleaner than the US grid in the bundled table, and carbon dominates the weights
        assertEquals("PA", result.getRecommended().portfolio().key());
        assertEquals(CarbonIntensityTable.bundledReference().intensity("PA", "FR"), carbon.value(), 1e-9);
        assertTrue(carbon.explanation().startsWith("Beta:"), carbon.explanation());
    }
}
