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
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code constraints.allowAccessMetros}: a metro that carries nothing but is the nearest one to a site.
 * AWS is offered in Ashburn only, so London can serve the London office and nothing else.
 */
class AccessMetroTest {

    private static FabricGateway fabric() {
        return OptimizerFixtures.gateway(List.of(
                OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                        List.of(GeoScopeType.CONUS), OptimizerFixtures.link("LD", 73.4)),
                OptimizerFixtures.metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of(GeoScopeType.UK),
                        OptimizerFixtures.link("DC", 73.4))),
                List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1")));
    }

    private static DesignSpec spec(Boolean allowAccessMetros) {
        return DesignSpec.builder().name("access")
                .site(SiteIntent.builder().label("Ashburn office").metro("DC").build())
                .site(SiteIntent.builder().label("London office").metro("LD").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws")).build())
                .constraints(DesignConstraints.builder().requiredMetros(List.of("DC", "LD"))
                        .allowAccessMetros(allowAccessMetros).build())
                .build();
    }

    private static OptimizationResult run(DesignSpec spec) {
        return MetroOptimizer.forSpec(fabric(), spec).rateCard(ReferenceRateCard.standard()).run();
    }

    @Test
    @DisplayName("by default a metro that carries nothing makes the design infeasible")
    void defaultRejectsAnIdleMetro() {
        assertNull(run(spec(null)).getRecommended());
    }

    @Test
    @DisplayName("with the option the nearest metro to a site is allowed, priced and reported")
    void accessMetroIsAllowedPricedAndReported() {
        OptimizationResult result = run(spec(true));

        assertEquals("DC+LD", result.getRecommended().portfolio().key());
        List<String> labels = result.getRecommended().cost().lines().stream().map(l -> l.label()).toList();
        assertTrue(labels.contains("Fabric Cloud Router STANDARD in LD"), labels.toString());
        assertTrue(labels.stream().anyMatch(l -> l.startsWith("Fabric backbone") && l.endsWith("DC to LD")),
                labels.toString());
        assertTrue(result.getRiskAssessment().getFindings().stream().map(RiskFinding::getCategory)
                .anyMatch("ACCESS_METRO"::equals));
    }

    @Test
    @DisplayName("the option is left out of the spec's JSON and fingerprint when unset")
    void optionalInTheSpec() {
        DesignSpec plain = spec(null);
        assertTrue(!com.eqixiac.equinix.design.spec.DesignSpecIO.toJson(plain).contains("allowAccessMetros"));
        assertTrue(!plain.fingerprint().equals(spec(true).fingerprint()));
    }
}
