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

import com.eqixiac.equinix.design.spi.MetroPortfolio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Availability composition against values worked out by hand with round assumptions: metro 0.99,
 * router 0.98, connection 0.97, on-ramp 0.96, backbone 0.95, site access 0.9, cloud region 0.999.
 */
class AvailabilityModelTest {

    private static final AvailabilityAssumptions ROUND = new AvailabilityAssumptions(Map.of(
            ElementType.METRO, 0.99, ElementType.CLOUD_ROUTER, 0.98, ElementType.CLOUD_CONNECTION, 0.97,
            ElementType.CLOUD_ONRAMP, 0.96, ElementType.BACKBONE_LINK, 0.95, ElementType.SITE_ACCESS, 0.9,
            ElementType.CLOUD_REGION, 0.999), "test figures");

    private static WorkloadAvailability of(List<WorkloadAvailability> list, String label) {
        return list.stream().filter(a -> a.workload().equals(label)).findFirst().orElseThrow();
    }

    private static List<WorkloadAvailability> estimate(String... metros) {
        DesignGraph g = DesignGraphBuilder.build(ResilienceFixtures.context(), MetroPortfolio.ofCodes(metros), null);
        return AvailabilityModel.estimate(g, ROUND);
    }

    @Test
    @DisplayName("one metro: series of metro, router, connection, on-ramp and cloud region; single-homed sites")
    void series() {
        WorkloadAvailability api = of(estimate("DC"), "api");
        // branch = 0.99 x 0.98 x 0.97 x 0.96 = 0.90345024; core = branch x 0.999
        assertEquals(0.90254678976, api.coreAvailability(), 1e-11);
        // each site has one access circuit: 0.9
        assertEquals(0.812292110784, api.endToEndAvailability(), 1e-11);
        assertEquals(81.2292110784, api.percent(), 1e-9);
        assertEquals((1 - 0.812292110784) * 525_960, api.downtimeMinutesPerYear(), 1e-6);
    }

    @Test
    @DisplayName("primary and standby metros in parallel; dual-homed sites in parallel")
    void parallel() {
        List<WorkloadAvailability> list = estimate("DC", "NY");
        WorkloadAvailability api = of(list, "api");
        // core = (1 - (1 - 0.90345024)^2) x 0.999 = 0.9896874657...
        assertEquals(0.9896874657000985, api.coreAvailability(), 1e-12);
        // access = 1 - 0.1^2 = 0.99 for both sites
        assertEquals(0.9797905910430975, api.endToEndAvailability(), 1e-12);
        assertTrue(api.formula().startsWith("parallel("), api.formula());

        WorkloadAvailability batch = of(list, "batch");
        // two clouds in each branch and two distinct cloud regions:
        // branch = 0.99 x 0.98 x (0.97 x 0.96)^2 = 0.841292863488; core = (1 - (1 - branch)^2) x 0.999^2
        assertEquals(0.9728633955425658, batch.coreAvailability(), 1e-12);
    }

    @Test
    @DisplayName("defaults are labelled assumptions, not SLAs, and every figure is validated")
    void assumptions() {
        AvailabilityAssumptions d = AvailabilityAssumptions.defaults();
        assertTrue(d.source().contains("NOT Equinix or cloud-provider SLAs"));
        assertEquals(ElementType.values().length, d.availability().size());
        assertThrows(IllegalArgumentException.class,
                () -> new AvailabilityAssumptions(Map.of(ElementType.METRO, 1.5), "x"));
        assertEquals(0.9999, new AvailabilityAssumptions(Map.of(), null).of(ElementType.METRO), 0.0,
                "a missing element type falls back to the default");
    }
}
