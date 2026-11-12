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
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCatalogLoaderTest {

    private static DesignSpec spec(CloudIntent... clouds) {
        DesignSpec.DesignSpecBuilder b = DesignSpec.builder().name("l")
                .workload(WorkloadIntent.builder().label("w").bandwidthMbps(100).build());
        for (CloudIntent c : clouds) {
            b.cloud(c);
        }
        return b.build();
    }

    @Test
    @DisplayName("geo scopes become zones with their ISO-style aliases; measured latencies and coordinates load")
    void metros() {
        CatalogDesignContext ctx = FabricCatalogLoader.load(OptimizerFixtures.standard(), spec(), null);
        assertEquals(5, ctx.metros().size());
        assertEquals(Set.of("CONUS", "US"), ctx.metro(MetroId.of("DC")).orElseThrow().geoZones());
        assertEquals(Region.EMEA, ctx.metro(MetroId.of("FR")).orElseThrow().region());
        assertEquals(73.4, ctx.roundTripLatencyMs(MetroId.of("LD"), MetroId.of("DC")).getAsDouble());
        assertTrue(ctx.dataNotes().get(0).contains("read to its last page"), ctx.dataNotes().toString());
        assertTrue(ctx.calibration().isCalibrated());
        assertEquals(6, ctx.measuredPairCount());
    }

    @Test
    @DisplayName("a metro with no geo scope or zone still gets zones from its country (live shape of DB, ML, SG)")
    void zonesFromCountry() {
        Metro dublin = OptimizerFixtures.metro("DB", "Dublin", Region.EMEA, 53.3, -6.3, List.of());
        org.mockito.Mockito.when(dublin.getCountry()).thenReturn("IE");
        Metro singapore = OptimizerFixtures.metro("SG", "Singapore", Region.APAC, 1.3, 103.8, List.of());
        org.mockito.Mockito.when(singapore.getCountry()).thenReturn("SG");
        Metro london = OptimizerFixtures.metro("LD", "London", Region.EMEA, 51.5, -0.1, List.of());
        org.mockito.Mockito.when(london.getCountry()).thenReturn("GB");

        assertEquals(Set.of("IE", "EU"), FabricCatalogLoader.zonesOf(dublin));
        assertEquals(Set.of("SG"), FabricCatalogLoader.zonesOf(singapore));
        assertEquals(Set.of("GB", "UK"), FabricCatalogLoader.zonesOf(london));
    }

    @Test
    @DisplayName("clouds resolve to metros by service profile; the chosen profile prefers the cloud's region and "
            + "does not depend on catalogue order")
    void providers() {
        ServiceProfile east = OptimizerFixtures.profile("sp-b", "AWS Direct Connect", "DC:us-east-1");
        ServiceProfile west = OptimizerFixtures.profile("sp-a", "AWS Direct Connect (Hosted)", "DC:us-west-2");
        List<ServiceProfile> profiles = new ArrayList<>(List.of(east, west));
        FabricGateway forward = OptimizerFixtures.gateway(List.of(OptimizerFixtures.metro("DC", "Ashburn", Region.AMER,
                39.0, -77.5, List.of(GeoScopeType.CONUS))), profiles);
        Collections.reverse(profiles);
        FabricGateway reversed = OptimizerFixtures.gateway(List.of(OptimizerFixtures.metro("DC", "Ashburn", Region.AMER,
                39.0, -77.5, List.of(GeoScopeType.CONUS))), profiles);
        DesignSpec spec = spec(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1").build(),
                CloudIntent.builder().id("ghost").serviceProfileName("No Such Provider").build());
        for (FabricGateway fabric : List.of(forward, reversed)) {
            CatalogDesignContext ctx = FabricCatalogLoader.load(fabric, spec, null);
            ProviderAvailability aws = ctx.providerAvailability(MetroId.of("DC")).get("aws");
            assertEquals("sp-b", aws.getServiceProfileUuid(), "the profile serving us-east-1 wins");
            assertEquals(2, aws.getProfileOptions().size(), "every matching profile is kept as an option");
            assertEquals(Set.of("aws"), ctx.reachableClouds(MetroId.of("DC")));
            assertTrue(ctx.dataNotes().stream().anyMatch(n -> n.contains("'ghost' is reachable from no metro")),
                    ctx.dataNotes().toString());
        }
    }
}
