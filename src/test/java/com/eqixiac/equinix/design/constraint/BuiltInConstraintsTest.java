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

package com.eqixiac.equinix.design.constraint;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInConstraintsTest {

    private static DesignSpec.DesignSpecBuilder base(DesignConstraints constraints) {
        return DesignSpec.builder().name("k").constraints(constraints)
                .site(SiteIntent.builder().label("HQ").metro("DC").build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build());
    }

    private static FakeDesignContext ctx(DesignSpec spec) {
        return new FakeDesignContext(spec).metro("DC", Region.AMER, "US", "CONUS").metro("NY", Region.AMER, "US")
                .metro("LD", Region.EMEA, "UK").metro("FR", Region.EMEA, "EU").metro("ZZ", null)
                .latency("DC", "HQ", 0.5).latency("NY", "HQ", 6.3).latency("LD", "HQ", 73.4)
                .reachable("DC", "aws").reachable("NY", "aws").reachable("LD", "aws").reachable("FR", "aws");
    }

    private static boolean ok(com.eqixiac.equinix.design.spi.Constraint c, FakeDesignContext ctx, String... metros) {
        return c.check(MetroPortfolio.ofCodes(metros), ctx).satisfied();
    }

    @Test
    @DisplayName("metro and region lists, compliance zones and geo zones (pruning per metro)")
    void lists() {
        FakeDesignContext ctx = ctx(base(DesignConstraints.builder().requiredMetros(List.of("NY"))
                .excludedMetros(List.of("LD")).requiredRegions(List.of(Region.EMEA)).excludedRegions(List.of(Region.APAC))
                .complianceZones(List.of(ComplianceZone.EU_GDPR)).geoZones(List.of("eu", "UK")).build()).build());
        assertFalse(ok(new RequiredMetrosConstraint(), ctx, "DC"));
        assertTrue(ok(new RequiredMetrosConstraint(), ctx, "NY", "DC"));
        assertFalse(new ExcludedMetrosConstraint().admits(ctx.metro(MetroId.of("LD")).orElseThrow(), ctx));
        assertFalse(ok(new RequiredRegionsConstraint(), ctx, "DC", "NY"));
        assertTrue(ok(new RequiredRegionsConstraint(), ctx, "DC", "FR"));
        assertFalse(new ExcludedRegionsConstraint().admits(ctx.metro(MetroId.of("ZZ")).orElseThrow(), ctx),
                "a metro with no region cannot be shown to be outside the excluded ones");
        assertTrue(new ComplianceZonesConstraint().admits(ctx.metro(MetroId.of("FR")).orElseThrow(), ctx));
        assertFalse(new ComplianceZonesConstraint().admits(ctx.metro(MetroId.of("DC")).orElseThrow(), ctx));
        GeoZonesConstraint geo = new GeoZonesConstraint();
        assertTrue(geo.admits(ctx.metro(MetroId.of("FR")).orElseThrow(), ctx));
        assertTrue(geo.admits(ctx.metro(MetroId.of("LD")).orElseThrow(), ctx));
        assertFalse(geo.admits(ctx.metro(MetroId.of("DC")).orElseThrow(), ctx), "US is outside EU/UK");
        assertFalse(geo.admits(ctx.metro(MetroId.of("ZZ")).orElseThrow(), ctx), "no zone data: not admitted");
    }

    @Test
    @DisplayName("redundancy tiers are explicit rules; MULTI_METRO does not force a region spread")
    void redundancy() {
        MinimumRedundancyConstraint c = new MinimumRedundancyConstraint();
        FakeDesignContext multiMetro = ctx(base(DesignConstraints.builder().minimumRedundancy(RedundancyTier.MULTI_METRO).build()).build());
        assertFalse(ok(c, multiMetro, "DC"));
        assertTrue(ok(c, multiMetro, "DC", "NY"), "two AMER metros satisfy MULTI_METRO");
        FakeDesignContext multiRegion = ctx(base(DesignConstraints.builder().minimumRedundancy(RedundancyTier.MULTI_REGION).build()).build());
        assertFalse(ok(c, multiRegion, "DC", "NY"));
        assertTrue(ok(c, multiRegion, "DC", "LD"));
        FakeDesignContext nPlus1 = ctx(base(DesignConstraints.builder().minimumRedundancy(RedundancyTier.N_PLUS_1).build()).build());
        assertFalse(ok(c, nPlus1, "DC", "LD"), "N_PLUS_1 needs two metros in one region");
        assertTrue(ok(c, nPlus1, "DC", "NY"));
    }

    @Test
    @DisplayName("site latency limit applies to known sites only and says so when nothing is evaluable")
    void siteLatency() {
        FakeDesignContext ctx = ctx(base(DesignConstraints.builder().maxLatencyMs(10.0).build()).build());
        assertTrue(ok(new MaxSiteLatencyConstraint(), ctx, "NY"));
        ConstraintVerdict far = new MaxSiteLatencyConstraint().check(MetroPortfolio.ofCodes("LD"), ctx);
        assertFalse(far.satisfied());
        assertTrue(far.reason().contains("73.4 ms"), far.reason());
        ConstraintVerdict unknown = new MaxSiteLatencyConstraint().check(MetroPortfolio.ofCodes("FR"), ctx);
        assertTrue(unknown.satisfied());
        assertTrue(unknown.reason().contains("NOT applied"), unknown.reason());
    }

    @Test
    @DisplayName("workload latency, pinned metro, required clouds, metro count and every-metro-used")
    void workloadRules() {
        DesignSpec spec = base(DesignConstraints.builder().minMetros(1).maxMetros(2).build())
                .workload(WorkloadIntent.builder().label("rt").bandwidthMbps(10).maxLatencyMs(5.0).build())
                .workload(WorkloadIntent.builder().label("db").bandwidthMbps(10).pinnedMetro("NY").build())
                .cloud(CloudIntent.builder().id("gcp").provider(CloudProviderType.GOOGLE_CLOUD).build()).build();
        FakeDesignContext ctx = ctx(spec).reachable("NY", "aws", "gcp");
        assertFalse(ok(new WorkloadLatencyConstraint(), ctx, "LD", "NY"), "rt lands in NY (6.3 ms > 5)");
        assertTrue(ok(new WorkloadLatencyConstraint(), ctx, "DC", "NY"));
        assertFalse(ok(new PinnedWorkloadsConstraint(), ctx, "DC"));
        assertFalse(ok(new RequiredCloudsConstraint(), ctx, "DC"), "gcp is reachable only in NY");
        assertTrue(ok(new RequiredCloudsConstraint(), ctx, "DC", "NY"));
        assertFalse(ok(new MetroCountConstraint(), ctx, "DC", "NY", "LD"));
        FakeDesignContext plain = ctx(base(DesignConstraints.none()).build());
        assertFalse(ok(new EveryMetroUsedConstraint(), plain, "DC", "NY", "LD"), "one workload uses two metros at most");
        assertTrue(ok(new EveryMetroUsedConstraint(), plain, "DC", "NY"));
        assertEquals(List.of("required_metros"), Constraints.forSpec(base(DesignConstraints.builder()
                .requiredMetros(List.of("DC")).build()).build()).stream().map(c -> c.id())
                .filter(id -> id.startsWith("required_m")).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("budget compares in one currency only; another currency is not evaluable, never compared")
    void budget() {
        PortfolioCost usd = new PortfolioCost(List.of(CostLine.exact("x", null, new BigDecimal("1200"), null, "USD",
                PriceSource.EQUINIX_LIVE, null)), List.of());
        DesignSpec spec = base(DesignConstraints.none()).commercial(CommercialTerms.builder()
                .budgetMaxMonthly(new BigDecimal("1000")).build()).build();
        FakeDesignContext ctx = ctx(spec).costModel((p, c) -> usd);
        assertFalse(ok(new BudgetConstraint(), ctx, "DC"));

        DesignSpec eur = base(DesignConstraints.none()).commercial(CommercialTerms.builder().currency("EUR")
                .budgetMaxMonthly(new BigDecimal("1000")).build()).build();
        ConstraintVerdict verdict = new BudgetConstraint().check(MetroPortfolio.ofCodes("DC"),
                ctx(eur).costModel((p, c) -> usd));
        assertTrue(verdict.satisfied());
        assertTrue(verdict.reason().startsWith("budget not evaluable"), verdict.reason());

        PortfolioCost partial = new PortfolioCost(List.of(CostLine.exact("x", null, new BigDecimal("900"), null, "USD",
                PriceSource.EQUINIX_LIVE, null)), List.of("router: no figure"));
        ConstraintVerdict lowerBound = new BudgetConstraint().check(MetroPortfolio.ofCodes("DC"),
                ctx(spec).costModel((p, c) -> partial));
        assertTrue(lowerBound.satisfied());
        assertTrue(lowerBound.reason().contains("lower bound"), lowerBound.reason());
    }
}
