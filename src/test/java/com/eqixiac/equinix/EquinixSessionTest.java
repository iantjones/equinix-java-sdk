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

package com.eqixiac.equinix;

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Equinix} session and {@link Design} facade: that every domain
 * obtained from a session shares one core client (hence one OAuth token + connection pool),
 * that accessors are cached, that standalone clients are independent, and that the design
 * facade is wired over the session's Fabric.
 */
class EquinixSessionTest {

    private static BasicEquinixCredentials creds() {
        return new BasicEquinixCredentials("test-id", "test-secret");
    }

    @Test
    @DisplayName("all domains from one session share a single core client (one token + pool)")
    void domainsShareOneCoreClient() throws Exception {
        try (Equinix eq = new Equinix(creds())) {
            Object core = eq.fabric().getEquinixClient();
            assertSame(core, eq.networkEdge().getEquinixClient());
            assertSame(core, eq.customerPortal().getEquinixClient());
            assertSame(core, eq.ibxSmartView().getEquinixClient());
            assertSame(core, eq.internetAccess().getEquinixClient());
            assertSame(core, eq.projects().getEquinixClient());
            assertSame(core, eq.iam().getEquinixClient());
            assertSame(core, eq.sts().getEquinixClient());
        }
    }

    @Test
    @DisplayName("standalone clients each build their own core client (independent token + pool)")
    void standaloneClientsDoNotShare() throws Exception {
        try (Fabric fabric = new Fabric(creds());
             NetworkEdge edge = new NetworkEdge(creds())) {
            assertNotSame(fabric.getEquinixClient(), edge.getEquinixClient(),
                    "two standalone clients must not share a core");
        }
    }

    @Test
    @DisplayName("domain accessors are lazily created and cached")
    void accessorsAreCached() throws Exception {
        try (Equinix eq = new Equinix(creds())) {
            assertSame(eq.fabric(), eq.fabric());
            assertSame(eq.networkEdge(), eq.networkEdge());
        }
    }

    @Test
    @DisplayName("design facade is wired over the session's shared Fabric")
    void designFacadeOverSessionFabric() throws Exception {
        try (Equinix eq = new Equinix(creds())) {
            Design design = eq.design();
            assertNotNull(design);
            assertNotNull(design.optimizeMetros());
            assertNotNull(design.savingsCalculator());
            assertNotNull(design.tcoComparison());
            assertNotNull(design.peeringIntelligence());
        }
    }

    @Test
    @DisplayName("Design.over(fabric) reuses the supplied client without opening a new one")
    void designOverStandaloneFabric() throws Exception {
        try (Fabric fabric = new Fabric(creds())) {
            Design design = Design.over(fabric);
            assertNotNull(design.optimizeMetros());
            // building a design facade must not have triggered authentication / a token fetch
            assertNull(fabric.getEquinixClient().getOAuthToken());
        }
    }

    @Test
    @DisplayName("deploymentWizard(result) returns a builder bound to the facade's Fabric")
    void deploymentWizardBuilderSmoke() throws Exception {
        try (Fabric fabric = new Fabric(creds())) {
            Design design = Design.over(fabric);
            assertNotNull(design.deploymentWizard(minimalOptimizationResult()));
            // like the other facade accessors, building the wizard must not trigger a token fetch
            assertNull(fabric.getEquinixClient().getOAuthToken());
        }
    }

    private static OptimizationResult minimalOptimizationResult() {
        return OptimizationResult.builder()
                .recommendations(Collections.singletonList(
                        MetroRecommendation.builder()
                                .rank(1).metroId(MetroId.of(MetroCode.DC)).metroName("Ashburn")
                                .score(new MetroScore(95.0, Collections.emptyList()))
                                .reasons(Collections.singletonList("Primary metro"))
                                .build()))
                .computedAt(Instant.now())
                .computeTimeMs(1)
                .build();
    }
}
