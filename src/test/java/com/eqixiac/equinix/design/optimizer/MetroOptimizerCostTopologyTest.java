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
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.MetroCostBreakdown;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression cover for two cost-estimation fixes:
 *
 * <ul>
 *   <li><strong>Per-metro cost follows the topology.</strong> The estimate used to split the total
 *       workload bandwidth EVENLY across the selected metros, contradicting the topology assembled
 *       two phases earlier in the very same result: the topology could put an 8&nbsp;Gbps workload
 *       entirely in one metro while the cost table priced each metro at half of it. Each metro is
 *       now priced at the bandwidth of the workloads the topology actually assigns to it.</li>
 *   <li><strong>Per-metro currency travels on the breakdown.</strong> {@code MetroCostBreakdown}
 *       had no currency field, so a renderer had nothing truthful to print per row; live Fabric
 *       pricing genuinely quotes different currencies per region. The engine now stamps each
 *       breakdown with its quote's currency and the markdown renders each row in it.</li>
 * </ul>
 */
@DisplayName("MetroOptimizer cost estimation: topology-driven bandwidth + per-metro currency")
class MetroOptimizerCostTopologyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;

    /** DC + DA (10ms apart), with one AWS profile published in DA only. */
    private FabricGateway gatewayAwsAtDaOnly() throws Exception {
        Metro dc = metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of(connectedMetro("DA", 10.0)));
        Metro da = metro("DA", "Dallas", Region.AMER, DA_LAT, DA_LON, List.of(connectedMetro("DC", 10.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da), null, null, null, null));

        ServiceProfile awsProfile = mock(ServiceProfile.class);
        lenient().when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        lenient().when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        lenient().when(awsProfile.metros()).thenReturn(List.of(serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    @Test
    @DisplayName("each metro is priced at the bandwidth the topology assigns it, not an even split")
    void perMetroCostFollowsTopologyAssignments() throws Exception {
        // "Bulk" (8000 Mbps, no dependencies) is placed in the highest-scored metro (DC);
        // "Cloudy" (2000 Mbps, depends on AWS) is placed in DA, the only metro carrying AWS.
        // The even split would have priced both metros at 5000; the topology says 8000 / 2000.
        Map<String, Integer> pricedBandwidthByMetro = new LinkedHashMap<>();
        RateCard recording = recordingRateCard(pricedBandwidthByMetro);

        OptimizationResult result = MetroOptimizer.builder(gatewayAwsAtDaOnly())
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Bulk").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(8000).done()
                .addWorkload("Cloudy").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(2000)
                    .dependsOn(CloudProviderType.AWS).done()
                .constraints().maxMetros(2).done()
                .rateCard(recording)
                .optimize();

        // Preconditions: the topology this run itself reports.
        assertEquals(MetroId.of(MetroCode.DC),
                result.getTopology().getPlacements().get(0).getAssignedMetro(),
                "Bulk sits in DC: " + result.getTopology().summary());
        assertEquals(MetroId.of(MetroCode.DA),
                result.getTopology().getPlacements().get(1).getAssignedMetro(),
                "Cloudy sits in DA (its AWS dependency): " + result.getTopology().summary());

        // The cost phase priced exactly what the topology assigned — not 5000/5000.
        assertEquals(Integer.valueOf(8000), pricedBandwidthByMetro.get("DC"),
                "DC is priced at Bulk's full 8000 Mbps: " + pricedBandwidthByMetro);
        assertEquals(Integer.valueOf(2000), pricedBandwidthByMetro.get("DA"),
                "DA is priced at Cloudy's 2000 Mbps: " + pricedBandwidthByMetro);

        // The per-metro line item states the topology-assigned sizing.
        MetroCostBreakdown dcCost = result.getCostEstimate().getPerMetro().stream()
                .filter(c -> MetroId.of(MetroCode.DC).equals(c.getMetroId()))
                .findFirst().orElseThrow();
        assertTrue(dcCost.getLineItems().keySet().stream()
                        .anyMatch(k -> k.contains("8000 Mbps assigned by the topology")),
                "the line item names the topology-assigned bandwidth: " + dcCost.getLineItems());
    }

    @Test
    @DisplayName("each MetroCostBreakdown carries the currency its quote was priced in")
    void perMetroCurrencyIsCarriedOntoTheBreakdown() throws Exception {
        // USD in DC, EUR everywhere else — the shape live Fabric pricing genuinely produces.
        // Before the fix MetroCostBreakdown had no currency field at all, so this information was
        // dropped on the floor and the renders hardcoded "$" for every row.
        RateCard perMetroCurrency = new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                Currency ccy = metro == MetroCode.DC
                        ? Currency.getInstance("USD") : Currency.getInstance("EUR");
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                        ccy, PriceSource.EQUINIX_LIVE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.EQUINIX_LIVE;
            }
        };

        OptimizationResult result = MetroOptimizer.builder(gatewayAwsAtDaOnly())
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .constraints().maxMetros(2).done()
                .rateCard(perMetroCurrency)
                .optimize();

        Map<String, String> currencyByMetro = new LinkedHashMap<>();
        for (MetroCostBreakdown mcb : result.getCostEstimate().getPerMetro()) {
            currencyByMetro.put(mcb.getMetroId().code(), mcb.getCurrency());
        }
        assertEquals("USD", currencyByMetro.get("DC"), currencyByMetro.toString());
        assertEquals("EUR", currencyByMetro.get("DA"), currencyByMetro.toString());

        // The markdown cost table renders each row in ITS currency, never a hardcoded dollar.
        String md = result.toMarkdown();
        assertTrue(md.contains("| DC | $1000 | $500 |"), "USD row renders in dollars: " + md);
        assertTrue(md.contains("| DA | €1000 | €500 |"), "EUR row renders in euros: " + md);
        assertFalse(md.contains("$1000 | $500 |\n| DA | $"),
                "the EUR row must not be rendered with a dollar sign: " + md);
    }

    /** A rate card that records, per metro code, the bandwidth its connection quote was sized at. */
    private static RateCard recordingRateCard(Map<String, Integer> pricedBandwidthByMetro) {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                pricedBandwidthByMetro.put(metro.name(), bandwidthMbps);
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
        lenient().when(m.metroId()).thenReturn(MetroId.of(code));
        lenient().when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        lenient().when(m.getName()).thenReturn(name);
        lenient().when(m.getRegion()).thenReturn(region);
        lenient().when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        lenient().when(m.getConnectedMetros()).thenReturn(connected);
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
