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

package com.eqixiac.equinix.design.objective;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInObjectivesTest {

    private static DesignSpec.DesignSpecBuilder base() {
        return DesignSpec.builder().name("o")
                .site(SiteIntent.builder().label("HQ").metro("DC").weight(3.0).build())
                .site(SiteIntent.builder().label("Lab").metro("SV").weight(1.0).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).required(false).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build());
    }

    private static FakeDesignContext ctx(DesignSpec spec) {
        return new FakeDesignContext(spec).metro("DC", Region.AMER, "US").metro("SV", Region.AMER, "US")
                .metro("LD", Region.EMEA, "UK")
                .latency("DC", "HQ", 0.5).latency("DC", "Lab", 65.8).latency("SV", "HQ", 65.8).latency("SV", "Lab", 0.5)
                .reachable("DC", "aws").reachable("SV", "aws", "azure").reachable("LD", "azure");
    }

    @Test
    @DisplayName("latency: site-weighted round trip to the nearest metro, worst case named, unknown sites left out")
    void latency() {
        FakeDesignContext ctx = ctx(base().site(SiteIntent.builder().label("Far").build()).build());
        ObjectiveValue one = new LatencyObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx);
        // weights 3:1 over known sites (Far unknown, weight renormalised): (3*0.5 + 1*65.8)/4
        assertEquals((3 * 0.5 + 65.8) / 4, one.value(), 1e-9);
        assertTrue(one.explanation().contains("worst case Lab 65.8 ms via DC (measured)"), one.explanation());
        assertTrue(one.explanation().contains("unknown for Far"), one.explanation());
        assertEquals(0.5, new LatencyObjective().evaluate(MetroPortfolio.ofCodes("DC", "SV"), ctx).value(), 1e-9);
        assertFalse(new LatencyObjective().evaluate(MetroPortfolio.ofCodes("LD"), ctx).known(),
                "no known latency is unknown, not a number");
        assertFalse(new LatencyObjective().evaluate(MetroPortfolio.ofCodes("DC"),
                new FakeDesignContext(DesignSpec.builder().name("n").workload(WorkloadIntent.builder().label("w")
                        .build()).build())).known());
    }

    @Test
    @DisplayName("provider coverage counts required clouds double")
    void coverage() {
        FakeDesignContext ctx = ctx(base().build());
        assertEquals(100.0 * 2 / 3, new ProviderCoverageObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx).value(), 1e-9);
        assertEquals(100.0, new ProviderCoverageObjective().evaluate(MetroPortfolio.ofCodes("SV"), ctx).value(), 1e-9);
    }

    @Test
    @DisplayName("resilience: share of site weight, required clouds and workloads surviving the worst metro loss")
    void resilience() {
        FakeDesignContext ctx = ctx(base().build());
        assertEquals(0.0, new ResilienceObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx).value(), 1e-9,
                "a single metro survives nothing");
        // DC+SV: api primary DC, standby SV. Losing either keeps every site, aws and the workload.
        ObjectiveValue pair = new ResilienceObjective().evaluate(MetroPortfolio.ofCodes("DC", "SV"), ctx);
        assertEquals(100.0, pair.value(), 1e-9, pair.explanation());
        assertTrue(pair.explanation().contains("worst region loss (AMER)"), pair.explanation());
        // DC+LD: LD reaches no aws, so api has no standby: losing DC loses aws and the workload.
        ObjectiveValue weak = new ResilienceObjective().evaluate(MetroPortfolio.ofCodes("DC", "LD"), ctx);
        assertTrue(weak.value() < 50, weak.explanation());
        assertTrue(weak.explanation().contains("losing DC"), weak.explanation());
    }

    @Test
    @DisplayName("compliance: share of metro/zone checks satisfied; 100 when nothing is stated")
    void compliance() {
        FakeDesignContext none = ctx(base().build());
        assertEquals(100.0, new ComplianceObjective().evaluate(MetroPortfolio.ofCodes("LD"), none).value());
        FakeDesignContext gdpr = ctx(base().constraints(DesignConstraints.builder()
                .complianceZones(List.of(ComplianceZone.EU_GDPR)).geoZones(List.of("uk")).build()).build());
        assertEquals(50.0, new ComplianceObjective().evaluate(MetroPortfolio.ofCodes("DC", "LD"), gdpr).value());
    }

    @Test
    @DisplayName("cost is unknown without a cost model")
    void costWithoutModel() {
        assertFalse(new CostObjective().evaluate(MetroPortfolio.ofCodes("DC"), ctx(base().build())).known());
        assertEquals(5, BuiltInObjectives.all().size());
    }
}
