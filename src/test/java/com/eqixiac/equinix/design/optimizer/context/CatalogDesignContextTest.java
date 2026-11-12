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

package com.eqixiac.equinix.design.optimizer.context;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.geo.RouteFactorCalibration;
import com.eqixiac.equinix.design.geo.SpeedOfLightLatency;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogDesignContextTest {

    private static final SiteIntent HQ = SiteIntent.builder().label("HQ").metro("DC").build();
    private static final SiteIntent LAB = SiteIntent.builder().label("Lab").latitude(37.3382).longitude(-121.8863).build();
    private static final SiteIntent NOWHERE = SiteIntent.builder().label("Nowhere").build();

    private static DesignSpec spec(SiteIntent... sites) {
        DesignSpec.DesignSpecBuilder b = DesignSpec.builder().name("t")
                .workload(WorkloadIntent.builder().label("w").bandwidthMbps(100).build());
        for (SiteIntent s : sites) {
            b.site(s);
        }
        return b.build();
    }

    private static CatalogDesignContext context(DesignSpec spec) {
        return CatalogDesignContext.builder(spec)
                .metro("DC", Region.AMER, 39.0438, -77.4874)
                .metro("SV", Region.AMER, 37.3382, -121.8863)
                .metro("LD", Region.EMEA, 51.5074, -0.1278)
                .metro("XX", Region.APAC, null, null)
                .measuredLatency("DC", "SV", 65.8)
                .build();
    }

    @Test
    @DisplayName("latency: intra-metro assumption, Fabric measurement, calibrated estimate, unknown")
    void latencyRules() {
        CatalogDesignContext ctx = context(spec(HQ, LAB, NOWHERE));
        LatencyFigure same = ctx.latency(MetroId.of("DC"), HQ);
        assertEquals(LatencyFigure.Basis.INTRA_METRO, same.basis());
        assertEquals(CatalogDesignContext.INTRA_METRO_RTT_MS, same.roundTripMs());

        LatencyFigure measured = ctx.latency(MetroId.of("SV"), HQ);
        assertEquals(LatencyFigure.Basis.MEASURED, measured.basis());
        assertEquals(65.8, measured.roundTripMs());
        assertTrue(ctx.latencyMeasured(MetroId.of("SV"), HQ));

        // DC-LD is not measured: estimated from DC's coordinates with the factor fitted from SV-DC.
        LatencyFigure estimated = ctx.latency(MetroId.of("LD"), HQ);
        assertEquals(LatencyFigure.Basis.ESTIMATED, estimated.basis());
        double km = SpeedOfLightLatency.distanceKm(39.0438, -77.4874, 51.5074, -0.1278);
        double factor = ctx.calibration().overall().factor();
        assertEquals(1.75, factor, 0.01, "fitted from the one measured pair");
        assertEquals(SpeedOfLightLatency.roundTrip().millisForKm(km) * factor, estimated.roundTripMs(), 1e-9);
        assertFalse(ctx.latencyMeasured(MetroId.of("LD"), HQ));

        assertTrue(ctx.roundTripLatencyMs(MetroId.of("DC"), NOWHERE).isEmpty(), "no metro, no coordinates: unknown");
        assertTrue(ctx.roundTripLatencyMs(MetroId.of("XX"), LAB).isEmpty(), "a metro without coordinates");
        assertEquals(LatencyFigure.Basis.UNKNOWN, ctx.latency(MetroId.of("XX"), LAB).basis());
        assertEquals("unknown", ctx.latency(MetroId.of("XX"), LAB).render());
    }

    @Test
    @DisplayName("metro-to-metro latency is measured where Fabric measures it and estimated otherwise")
    void metroPairs() {
        CatalogDesignContext ctx = context(spec(HQ));
        assertEquals(65.8, ctx.roundTripLatencyMs(MetroId.of("SV"), MetroId.of("DC")).getAsDouble());
        assertEquals(LatencyFigure.Basis.ESTIMATED, ctx.latency(MetroId.of("DC"), MetroId.of("LD")).basis());
        assertTrue(ctx.roundTripLatencyMs(MetroId.of("DC"), MetroId.of("XX")).isEmpty());
        assertEquals(1, ctx.measuredPairCount());
    }

    @Test
    @DisplayName("an explicit calibration overrides the fitted one")
    void explicitCalibration() {
        CatalogDesignContext ctx = CatalogDesignContext.builder(spec(HQ)).metro("DC", Region.AMER, 39.0, -77.0)
                .metro("NY", Region.AMER, 40.7, -74.0).calibration(RouteFactorCalibration.uncalibrated()).build();
        assertFalse(ctx.calibration().isCalibrated());
    }

    @Test
    @DisplayName("site weights: explicit weight, else headcount x role, else an average site for its role")
    void siteWeights() {
        SiteIntent a = SiteIntent.builder().label("A").metro("DC").weight(3.0).build();
        SiteIntent b = SiteIntent.builder().label("B").metro("SV").headcount(1).role(SiteRole.HEADQUARTERS).build();
        SiteIntent c = SiteIntent.builder().label("C").metro("LD").build();
        CatalogDesignContext ctx = context(spec(a, b, c));
        // raw: A=3, B=1.5, C = mean(3,1.5)=2.25 -> total 6.75
        assertEquals(3 / 6.75, ctx.siteWeight(a), 1e-9);
        assertEquals(1.5 / 6.75, ctx.siteWeight(b), 1e-9);
        assertEquals(2.25 / 6.75, ctx.siteWeight(c), 1e-9);
        assertTrue(ctx.siteWeightingNotes().get(0).contains("C carried neither"), ctx.siteWeightingNotes().toString());

        SiteIntent hq = SiteIntent.builder().label("HQ").metro("DC").role(SiteRole.HEADQUARTERS).build();
        SiteIntent branch = SiteIntent.builder().label("Br").metro("SV").role(SiteRole.BRANCH_OFFICE).build();
        CatalogDesignContext roles = context(spec(hq, branch));
        assertTrue(roles.siteWeight(hq) > roles.siteWeight(branch), "role-only weighting is a real weighting");
        assertTrue(roles.siteWeightingNotes().get(0).contains("by role only"));
    }
}
