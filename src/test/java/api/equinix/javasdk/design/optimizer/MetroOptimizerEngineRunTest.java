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
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.OptimizationStrategy;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.CostEstimate;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A deterministic end-to-end run of the Metro Optimizer against a Mockito-stubbed {@link FabricGateway}.
 *
 * <p>The gateway serves three real AMER metros with inter-metro {@code avgLatency} data and a single
 * AWS service profile present in two of them. A single latency-weighted HQ site anchored at Ashburn
 * (DC) drives the ranking, so the engine's latency dimension deterministically favours DC. This
 * exercises the full {@code MetroOptimizer.builder(...).optimize()} pipeline — data collection,
 * candidate filtering, five-dimension scoring, selection, redundancy refinement, latency matrix,
 * provider connectivity, topology assembly, risk analysis, cost estimation (against a supplied
 * reference rate card so no live pricing call is made), and recommendation assembly — and asserts a
 * ranked, DC-primary result.</p>
 */
@DisplayName("MetroOptimizer end-to-end engine run (stubbed FabricGateway)")
class MetroOptimizerEngineRunTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Ashburn / Dallas / Silicon Valley centroids.
    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;

    private FabricGateway fabric;

    @BeforeEach
    void stubGateway() throws Exception {
        // DC is directly connected to DA (10ms) and SV (60ms), with data so latency uses the
        // Fabric avgLatency graph rather than the Haversine fallback.
        Metro dc = metro("DC", "Ashburn", DC_LAT, DC_LON, List.of(
                connectedMetro("DA", 10.0), connectedMetro("SV", 60.0)));
        Metro da = metro("DA", "Dallas", DA_LAT, DA_LON, List.of(
                connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", SV_LAT, SV_LON, List.of(
                connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));

        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv), null, null, null, null));

        // AWS service profile available in DC and DA but not SV. The optimizer matches a
        // CloudProviderType requirement against the profile name via
        // CloudProviderType.AWS.getProviderName() == "Amazon Web Services", so the name must
        // contain that phrase for the AWS requirement to resolve.
        ServiceProfile awsProfile = mock(ServiceProfile.class);
        when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(awsProfile.metros()).thenReturn(List.of(
                serviceProfileMetro("DC", "us-east-1"),
                serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        PaginatedFilteredList<ServiceProfile> spList =
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null);
        when(serviceProfiles.search()).thenReturn(spList);

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
    }

    @Test
    @DisplayName("ranks the three metros, chooses DC as primary, and prices every recommendation")
    void fullRunChoosesPrimaryAndRanks() {
        OptimizationResult result = MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
                .addWorkload("Web Tier").type(WorkloadType.GENERAL_COMPUTE).bandwidthMbps(1000).done()
                .strategy(OptimizationStrategy.BALANCED)
                .rateCard(ReferenceRateCard.standard())
                .optimize();

        List<MetroRecommendation> recs = result.getRecommendations();
        assertFalse(recs.isEmpty(), "the engine should return recommendations");

        // A primary is chosen and it is the site-anchor metro (DC has 0.5ms self-latency + AWS present).
        MetroRecommendation primary = result.primaryMetro();
        assertNotNull(primary, "a primary metro should be selected");
        assertEquals(MetroId.of(MetroCode.DC), primary.getMetroId(),
                "DC should rank first: lowest latency to the DC-anchored HQ and AWS available");
        assertEquals(1, primary.getRank(), "primary carries rank 1");

        // Ranks are contiguous and score-descending.
        for (int i = 0; i < recs.size(); i++) {
            assertEquals(i + 1, recs.get(i).getRank(), "ranks are 1..N in order");
        }
        for (int i = 1; i < recs.size(); i++) {
            assertTrue(recs.get(i - 1).getScore().getComposite() >= recs.get(i).getScore().getComposite(),
                    "recommendations are sorted by composite score descending");
        }

        // AWS-required filtering kept only the metros where the profile is present (DC, DA), excluding SV.
        List<String> codes = recs.stream().map(r -> r.getMetroId().code()).collect(Collectors.toList());
        assertTrue(codes.contains("DC"), "DC (AWS present) is a candidate: " + codes);
        assertTrue(codes.contains("DA"), "DA (AWS present) is a candidate: " + codes);
        assertFalse(codes.contains("SV"), "SV (AWS absent, required) should be filtered out: " + codes);

        // Every recommendation is priced via the supplied reference rate card, and the aggregate holds.
        CostEstimate cost = result.getCostEstimate();
        assertNotNull(cost, "a cost estimate is produced");
        assertEquals(recs.size(), cost.getPerMetro().size(), "one cost line per recommended metro");
        recs.forEach(r -> assertNotNull(r.getEstimatedCost(),
                "each recommendation carries a per-metro cost breakdown: " + r.getMetroId()));

        // The rendered report is non-trivial and names the primary.
        String summary = result.toSummary();
        assertTrue(summary.contains("Ashburn"), "summary names the DC primary: " + summary);
    }

    // ── stub builders (Jackson for the private-field Fabric models, Mockito for the interfaces) ──

    private static Metro metro(String code, String name, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(Region.AMER);
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
