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

package com.eqixiac.equinix.design.placement;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadPlacerTest {

    private static DesignSpec spec(WorkloadIntent... workloads) {
        DesignSpec.DesignSpecBuilder b = DesignSpec.builder().name("p")
                .site(SiteIntent.builder().label("HQ").metro("DC").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).build());
        for (WorkloadIntent w : workloads) {
            b.workload(w);
        }
        return b.build();
    }

    private static FakeDesignContext ctx(DesignSpec spec) {
        return new FakeDesignContext(spec).metro("DC", Region.AMER).metro("NY", Region.AMER).metro("SV", Region.AMER)
                .latency("DC", "HQ", 0.5).latency("NY", "HQ", 6.3).latency("SV", "HQ", 65.8)
                .reachable("DC", "aws").reachable("NY", "aws", "azure").reachable("SV", "aws", "azure");
    }

    @Test
    @DisplayName("primary is the eligible metro nearest the sites; the next eligible metro is the standby")
    void primaryAndStandby() {
        DesignSpec spec = spec(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build());
        PlacementPlan plan = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC", "NY", "SV"), ctx(spec));
        PlacementPlan.Placement api = plan.placement("api").orElseThrow();
        assertEquals(MetroId.of("DC"), api.primary());
        assertEquals(MetroId.of("NY"), api.standby());
        // azure is named by no workload: connected at the best metro reaching it (and a standby), unsized.
        PlacementPlan.Placement azure = plan.placement("cloud:azure").orElseThrow();
        assertEquals(MetroId.of("NY"), azure.primary());
        assertEquals(MetroId.of("SV"), azure.standby());
        assertTrue(plan.idleMetros().isEmpty());
        FakeDesignContext withLd = ctx(spec).metro("LD", Region.EMEA).latency("LD", "HQ", 73.4);
        PlacementPlan idle = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC", "LD"), withLd);
        assertEquals(Set.of(MetroId.of("LD")), idle.idleMetros(), "LD reaches no cloud and hosts nothing");
        PlacementPlan.CloudLeg dcAws = plan.legsIn(MetroId.of("DC")).get(0);
        assertEquals(1000, dcAws.sizedMbps());
        assertEquals(List.of("api"), dcAws.workloads());
    }

    @Test
    @DisplayName("a workload goes where all its clouds are reachable, and records what is missing otherwise")
    void cloudReachability() {
        DesignSpec spec = spec(WorkloadIntent.builder().label("multi").bandwidthMbps(500)
                .clouds(List.of("aws", "azure")).build());
        PlacementPlan plan = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC", "SV"), ctx(spec));
        assertEquals(MetroId.of("SV"), plan.placement("multi").orElseThrow().primary(), "DC lacks azure");

        PlacementPlan dcOnly = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC"), ctx(spec));
        assertTrue(dcOnly.placement("multi").orElseThrow().issues().get(0).contains("missing azure"));
    }

    @Test
    @DisplayName("pinned workloads run only in their metro and get no standby; a missing pin is not placed")
    void pinned() {
        DesignSpec spec = spec(WorkloadIntent.builder().label("db").bandwidthMbps(100).pinnedMetro("SV").build());
        PlacementPlan.Placement db = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC", "SV"), ctx(spec))
                .placement("db").orElseThrow();
        assertEquals(MetroId.of("SV"), db.primary());
        assertNull(db.standby());
        PlacementPlan.Placement missing = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC"), ctx(spec))
                .placement("db").orElseThrow();
        assertNull(missing.primary());
        assertTrue(missing.issues().get(0).contains("pinned to SV"));
    }

    @Test
    @DisplayName("maxLatencyMs filters metros; when none qualifies the breach is recorded")
    void workloadLatency() {
        DesignSpec spec = spec(WorkloadIntent.builder().label("rt").bandwidthMbps(100).maxLatencyMs(10.0).build());
        PlacementPlan.Placement rt = WorkloadPlacer.place(MetroPortfolio.ofCodes("NY", "SV"), ctx(spec))
                .placement("rt").orElseThrow();
        assertEquals(MetroId.of("NY"), rt.primary());
        assertNull(rt.standby(), "SV is beyond the limit, so it cannot be the standby either");
        PlacementPlan.Placement far = WorkloadPlacer.place(MetroPortfolio.ofCodes("SV"), ctx(spec))
                .placement("rt").orElseThrow();
        assertTrue(far.issues().get(0).contains("10.0 ms"), far.issues().toString());
    }

    @Test
    @DisplayName("an unsized workload is carried on its leg but marked unsized")
    void unsized() {
        DesignSpec spec = spec(WorkloadIntent.builder().label("x").clouds(List.of("aws")).build());
        PlacementPlan plan = WorkloadPlacer.place(MetroPortfolio.ofCodes("DC"), ctx(spec));
        PlacementPlan.CloudLeg leg = plan.legsIn(MetroId.of("DC")).get(0);
        assertEquals(0, leg.sizedMbps());
        assertEquals(List.of("x"), leg.unsizedWorkloads());
    }
}
