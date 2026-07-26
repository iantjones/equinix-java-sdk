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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.design.optimizer.model.CostEstimate;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.ScoringWeights;
import api.equinix.javasdk.design.optimizer.model.WorkloadProfile;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The cost-side currency-mixing (C9) and co-located precision guards on the optimizer and wizard:
 * a multi-metro set whose metros are priced in different currencies (which live Fabric pricing
 * genuinely produces per region) must not be summed into a fabricated single total, bandwidth split
 * across metros must not silently drop Mbps to integer truncation, and a zeroed required-provider
 * weight must not divide-by-zero into a NaN score.
 */
@DisplayName("Optimizer/wizard currency mixing + integer/NaN guards")
class CostCurrencyGuardsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;

    private FabricGateway fabric;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(connectedMetro("DA", 10.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(connectedMetro("DC", 10.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da), null, null, null, null));

        ServiceProfile awsProfile = mock(ServiceProfile.class);
        when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(awsProfile.metros()).thenReturn(List.of(
                serviceProfileMetro("DC", "us-east-1"),
                serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
    }

    /** A card that prices every connection/router, quoting USD in DC and EUR everywhere else. */
    private static RateCard perMetroCurrencyCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                Currency ccy = metro == MetroCode.DC ? Currency.getInstance("USD") : Currency.getInstance("EUR");
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                        ccy, PriceSource.EQUINIX_LIVE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                Currency ccy = metro == MetroCode.DC ? Currency.getInstance("USD") : Currency.getInstance("EUR");
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO,
                        ccy, PriceSource.EQUINIX_LIVE));
            }

            @Override
            public PriceSource source() {
                return PriceSource.EQUINIX_LIVE;
            }
        };
    }

    // ── estimateCosts: cross-metro currency mixing ──

    @Test
    @DisplayName("a metro set priced in two currencies produces no fabricated aggregate total")
    void mixedCurrencyMetrosProduceNoSingleTotal() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(2).done()
                .rateCard(perMetroCurrencyCard())
                .optimize();

        assertEquals(2, result.getRecommendations().size(), "DC and DA both selected");

        CostEstimate cost = result.getCostEstimate();
        assertNull(cost.getMonthlyTotal(), "USD + EUR metros must not be summed into one total");
        assertNull(cost.getSetupTotal());
        assertNull(cost.getCurrency(), "no single currency for a mixed-currency estimate");
        assertNotNull(cost.getMonthlyByCurrency());
        assertEquals(2, cost.getMonthlyByCurrency().size(), "one subtotal per currency: " + cost.getMonthlyByCurrency());
        assertTrue(cost.getMonthlyByCurrency().containsKey("USD") && cost.getMonthlyByCurrency().containsKey("EUR"));
        assertTrue(cost.getCostDisclaimer().contains("multiple currencies"), cost.getCostDisclaimer());
        // Each per-metro figure is still valid on its own.
        assertEquals(2, cost.getPerMetro().size());
        // The rendered summary shows the per-currency breakdown, never "$null".
        String summary = result.toSummary();
        assertFalse(summary.contains("$null"), summary);
        assertTrue(summary.contains("multiple currencies"), summary);
    }

    @Test
    @DisplayName("a single-currency metro set still totals and evaluates budget normally")
    void singleCurrencyMetrosStillTotal() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .constraints().maxMetros(2).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        CostEstimate cost = result.getCostEstimate();
        assertNotNull(cost.getMonthlyTotal(), "all-USD reference pricing totals normally");
        assertEquals("USD", cost.getCurrency());
        assertTrue(cost.isWithinBudget(), "no budget set => within budget");
    }

    // ── per-metro cost bandwidth follows the topology ──

    @Test
    @DisplayName("per-metro pricing bandwidth follows the topology and loses no Mbps")
    void bandwidthSplitPreservesTotal() {
        List<Integer> sized = new ArrayList<>();
        // Updated for the topology-driven cost fix: the estimate no longer splits the total
        // EVENLY across metros (the even split priced a fiction that contradicted the topology in
        // the same result — this run's single workload sits in ONE metro, yet each metro was
        // costed at half of it). Each metro is now priced at the bandwidth the topology assigns
        // it, so the single 101 Mbps workload prices as 101 at its assigned metro and 0 at the
        // other — and the per-metro sizes still sum to the declared total, preserving the original
        // no-truncation guarantee (the old integer division dropped 1 Mbps of a 101 split).
        MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(101).done()
                .constraints().maxMetros(2).done()
                .rateCard(recordingRateCard(sized))
                .optimize();

        assertEquals(2, sized.size(), "one pricing lookup per selected metro: " + sized);
        assertEquals(101, sized.stream().mapToInt(Integer::intValue).sum(),
                "the per-metro bandwidths must sum to the total, not 100: " + sized);
        assertTrue(sized.contains(101),
                "the workload's whole bandwidth is priced at its assigned metro, not split evenly: " + sized);
        assertTrue(sized.contains(0),
                "a metro the topology assigns nothing to is priced at zero workload bandwidth: " + sized);
    }

    // ── NaN guard in provider-coverage scoring ──

    @Test
    @DisplayName("a zeroed required-provider weight does not divide-by-zero into a NaN score")
    void zeroRequiredProviderWeightDoesNotProduceNaN() {
        // A preferred provider whose seller regions do NOT match the requested ones drives the
        // region-bonus branch with regionMatches=0; a required-provider weight of 0 then made the
        // bonus a 0.0/0.0 = NaN that poisoned the composite score.
        ScoringWeights weights = ScoringWeights.builder()
                .latencyWeight(1.0)
                .providerCoverageWeight(1.0)
                .costWeight(0.0)
                .redundancyWeight(0.0)
                .complianceWeight(0.0)
                .requiredProviderWeight(0.0)
                .build();

        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).done()
                .preferProvider(CloudProviderType.AWS).sellerRegions("ap-southeast-1").done()
                .scoringWeights(weights)
                .constraints().maxMetros(2).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        assertFalse(result.getRecommendations().isEmpty());
        for (MetroRecommendation rec : result.getRecommendations()) {
            double composite = rec.getScore().getComposite();
            assertTrue(Double.isFinite(composite), "composite score must be finite, not NaN: " + composite);
        }
    }

    // ── wizard: estimatePricing currency mixing ──

    @Test
    @DisplayName("a plan spanning two currencies omits the fabricated plan total")
    void wizardPlanPricingOmitsTotalWhenCurrenciesMix() {
        // Clean single-currency optimization for the topology, then price the plan with the
        // per-metro-currency card so DC lines are USD and DA lines are EUR.
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).dependsOn(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .constraints().maxMetros(2).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        DeploymentPlan plan = DeploymentWizard.builder(fabric, result)
                .routerNamePrefix("FCR")
                .rateCard(perMetroCurrencyCard())
                .plan();

        PlanPricing pricing = plan.getPricing();
        assertNull(pricing.getMonthlyTotal(), "USD DC lines + EUR DA lines must not be summed");
        assertNull(pricing.getSetupTotal());
        assertNull(pricing.getCurrency());
        assertTrue(pricing.getDisclaimer().contains("multiple currencies"), pricing.getDisclaimer());
        // The plan summary must not print "$null".
        assertFalse(plan.toSummary().contains("$null"), plan.toSummary());
    }

    // ── wizard: minimum-bandwidth floor honoured ──

    @Test
    @DisplayName("a workload profile's minimum bandwidth raises the wizard's connection sizing")
    void wizardHonoursProfileMinimumBandwidth() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(10)
                    .profile(WorkloadProfile.builder().minBandwidthMbps(1000.0).build())
                    .dependsOn(CloudProviderType.AWS)
                    .done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .constraints().maxMetros(1).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        DeploymentPlan plan = DeploymentWizard.builder(fabric, result)
                .routerNamePrefix("FCR")
                .bandwidthStrategy(BandwidthStrategy.AGGREGATED)
                .rateCard(ReferenceRateCard.standard())
                .plan();

        assertFalse(plan.getProviderConnections().isEmpty(), "an AWS connection is planned");
        for (PlannedConnection conn : plan.getProviderConnections()) {
            assertTrue(conn.getBandwidthMbps() >= 1000,
                    "the 10 Mbps declaration must be raised to the 1000 Mbps profile floor, got "
                            + conn.getBandwidthMbps());
        }
    }

    // ── wizard: CUSTOM bandwidth map keyed by provider label ──

    @Test
    @DisplayName("customBandwidthMap keyed by provider label alone is honoured (documented key form)")
    void wizardCustomBandwidthMapByProviderLabel() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web Tier").bandwidthMbps(1000).dependsOn(CloudProviderType.AWS).done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .constraints().maxMetros(1).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        // The AWS requirement's provider label is its corporate name; keying the custom map by that
        // label alone (not the compound "<metro>-<label>") must now match every metro.
        String providerLabel = result.getRecommendations().get(0).getAvailableProviders().stream()
                .filter(p -> p.isAvailable()).findFirst().orElseThrow().getProviderLabel();

        DeploymentPlan plan = DeploymentWizard.builder(fabric, result)
                .routerNamePrefix("FCR")
                .customBandwidthMap(java.util.Map.of(providerLabel, 7000))
                .rateCard(ReferenceRateCard.standard())
                .plan();

        assertFalse(plan.getProviderConnections().isEmpty());
        for (PlannedConnection conn : plan.getProviderConnections()) {
            assertEquals(7000, conn.getBandwidthMbps(),
                    "the provider-label custom key must apply: " + conn.getName());
            assertTrue(conn.getBandwidthAllocation().getReasoning().contains(providerLabel),
                    conn.getBandwidthAllocation().getReasoning());
        }
    }

    /** A card that records the bandwidth each pricing lookup is sized at and always quotes USD. */
    private static RateCard recordingRateCard(List<Integer> sized) {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                sized.add(bandwidthMbps);
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(100), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.CUSTOM));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.CUSTOM;
            }
        };
    }

    // ── stub builders (same shapes as MetroOptimizerLeversTest) ──

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(api.equinix.javasdk.core.model.MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}",
                ConnectedMetro.class);
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
