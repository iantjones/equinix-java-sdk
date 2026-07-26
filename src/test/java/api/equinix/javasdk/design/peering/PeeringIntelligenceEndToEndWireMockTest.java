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

package api.equinix.javasdk.design.peering;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
import api.equinix.javasdk.design.peering.enums.NetworkType;
import api.equinix.javasdk.design.peering.enums.PeeringPolicy;
import api.equinix.javasdk.design.peering.model.NetworkPresence;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.peering.model.PeeringOpportunity;
import api.equinix.javasdk.design.peering.model.PresenceCell;
import api.equinix.javasdk.design.peering.model.ResiliencyAssessment;
import api.equinix.javasdk.design.peering.model.UnifiedConnectivityView;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Drives the public entry point — {@code PeeringIntelligence.builder(fabric, key).addAsn(...).analyze()}
 * — end-to-end against a WireMock-stubbed PeeringDB plus a Mockito {@link FabricGateway}. Unlike
 * {@link PeeringDiversityEngineTest} (which constructs the engine directly with a hand-stubbed
 * {@code PeeringDbClient}), this test runs the whole {@code Builder.analyze()} → {@code new
 * PeeringDbClient(...)} → {@code engine.execute()} chain so the builder wiring and the real PeeringDB
 * HTTP/parse path are both exercised in one pass.
 *
 * <p>The builder is pointed at WireMock through the package-private {@code peeringDbBaseUrl} seam
 * (see {@code sdkChangeNeeded}); Fabric is a plain Mockito stub whose {@code metros().list()} returns
 * a real {@link PaginatedList}, matching the pattern the engine's {@code loadMetroGeo()} expects.</p>
 */
@DisplayName("PeeringIntelligence.analyze() — end-to-end via the public builder")
class PeeringIntelligenceEndToEndWireMockTest extends WireMockTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long AWS = 16509L;
    private static final long CUSTOMER = 65100L;

    // Fabric metro centroids (deliberately distinct from the PeeringDB facility coordinates so we can
    // tell the analysis used real Fabric data). IBX codes seed the IBX->metro bridge.
    private static final double DC_LAT = 39.0, DC_LON = -77.5;
    private static final double SV_LAT = 37.4, SV_LON = -121.9;
    private static final double DA_LAT = 32.8, DA_LON = -96.8;

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
        stubPeeringDb();
    }

    private String baseUrl() {
        return wireMockUrl() + "/api";
    }

    private void stubPeeringDb() {
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_org_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/net")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_net_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netixlan_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netfac")).withQueryParam("asn", equalTo("16509"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netfac_16509_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan")).withQueryParam("asn", equalTo("65100"))
                .willReturn(okJson(loadFixture("/json/peering/peeringdb_netixlan_65100_response.json"))));
    }

    @Test
    @DisplayName("basic analyze() builds the presence matrix from live PeeringDB data")
    void basicAnalyze() {
        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .analyze();

        assertNotNull(result);
        assertTrue(result.getDataSources().contains("PeeringDB"));
        // loadMetroGeo() succeeded against the Fabric stub, so Fabric is credited as a source.
        assertTrue(result.getDataSources().contains("Equinix Fabric"));

        // Network profile came from /net (label + policy + type parsed from PeeringDB).
        NetworkPresence aws = result.networkPresence(AWS);
        assertNotNull(aws);
        assertEquals("AWS", aws.getLabel());
        assertEquals("Amazon.com, Inc.", aws.getPeeringDbName());
        assertEquals(PeeringPolicy.SELECTIVE, aws.getPeeringPolicy());
        assertEquals(NetworkType.CONTENT, aws.getNetworkType());
        assertTrue(aws.isIpv6Capable());
        assertTrue(aws.isRouteServerParticipant(), "Ashburn session is is_rs_peer=true");

        // Equinix-only IX presence: Ashburn (DC) + San Jose (SV); the non-Equinix ix 999 was filtered.
        assertEquals(2, aws.ixMetroCount());
        assertTrue(aws.hasIxPeeringAt(MetroId.of("DC")));
        assertTrue(aws.hasIxPeeringAt(MetroId.of("SV")));
        assertFalse(aws.hasIxPeeringAt(MetroId.of("DA")), "AWS has no IX at Dallas in the fixtures");

        // Capacity is the sum of the two Equinix IX sessions (100G + 40G).
        assertEquals(140000, aws.getTotalIxCapacityMbps());

        // The matrix cell at DC reflects the parsed session.
        PresenceCell dcCell = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
        assertNotNull(dcCell);
        assertTrue(dcCell.isIxPresent());
        assertEquals(ConnectivityType.IX_PEERING, dcCell.getConnectivityType());
        assertEquals(100000, dcCell.getTotalIxCapacityMbps());
    }

    @Test
    @DisplayName("analyze() with customer metros produces a resiliency assessment")
    void analyzeWithResiliency() {
        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .customerMetros(MetroId.of("DC"), MetroId.of("SV"), MetroId.of("DA"))
                .includeResiliency(true)
                .analyze();

        ResiliencyAssessment resiliency = result.getResiliency();
        assertNotNull(resiliency, "resiliency requested with customer metros");
        assertNotNull(resiliency.getOverallRating());
        // Three customer metros → three metro-pair diversity scores.
        assertEquals(3, resiliency.getDiversityScores().size());
        // Blast-radius report exists per customer metro.
        assertEquals(3, resiliency.getBlastRadiusReports().size());
    }

    @Test
    @DisplayName("analyze() with a customer ASN discovers a mutual peering opportunity")
    void analyzeWithPeeringOpportunity() {
        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .customerAsn(CUSTOMER)
                .analyze();

        List<PeeringOpportunity> opportunities = result.getPeeringOpportunities();
        assertNotNull(opportunities);
        // Customer (65100) shares the Ashburn IX (100) with AWS → exactly one opportunity there.
        assertEquals(1, opportunities.size());
        PeeringOpportunity opp = opportunities.get(0);
        assertEquals(CUSTOMER, opp.getCustomerAsn());
        assertEquals(AWS, opp.getTargetAsn());
        assertEquals(MetroId.of("DC"), opp.getMetro());
        assertEquals(100, opp.getIxId());
        assertEquals(PeeringPolicy.SELECTIVE, opp.getTargetPolicy());
    }

    @Test
    @DisplayName("unified connectivity view reflects the reachable metros")
    void analyzeUnifiedView() {
        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .analyze();

        UnifiedConnectivityView view = result.unifiedView(AWS);
        assertNotNull(view);
        assertEquals(AWS, view.getAsn());
        assertEquals(2, view.getReachableMetroCount(), "AWS reaches DC and SV via IX");
        assertEquals(140000, view.getTotalIxCapacityMbps());
    }

    @Test
    @DisplayName("includeAll() enables every analysis flag, including the default-off resiliency assessment")
    void includeAllEnablesEveryAnalysis() {
        // includeResiliency defaults to false, so a resiliency assessment appearing here is the
        // observable proof that includeAll() flipped the flags on (capacity/policies default true;
        // resiliency is the discriminating one).
        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .customerMetros(MetroId.of("DC"), MetroId.of("SV"), MetroId.of("DA"))
                .includeAll()
                .analyze();

        ResiliencyAssessment resiliency = result.getResiliency();
        assertNotNull(resiliency, "includeAll() must turn the resiliency analysis on");
        assertNotNull(resiliency.getOverallRating());
        assertEquals(3, resiliency.getDiversityScores().size());

        // The capacity analysis (also covered by includeAll) still populates the presence matrix.
        NetworkPresence aws = result.networkPresence(AWS);
        assertEquals(140000, aws.getTotalIxCapacityMbps(), "capacity figures remain populated");
    }

    @Test
    @DisplayName("multiple target ASNs are queried in ONE batched asn__in request per endpoint")
    void multiAsnAnalyzeBatchesRequests() {
        long google = 15169L;
        // The engine must collapse the per-ASN loop into one asn__in request per endpoint
        // (net, netixlan, netfac) — deliberately NO per-ASN asn= stubs here, so a regression
        // back to per-ASN requests fails loudly.
        wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                .withQueryParam("asn__in", equalTo("16509,15169"))
                .willReturn(okJson("{\"data\":["
                        + "{\"asn\":16509,\"name\":\"Amazon.com, Inc.\",\"info_type\":\"Content\","
                        + "\"policy_general\":\"Selective\",\"info_ipv6\":true},"
                        + "{\"asn\":15169,\"name\":\"Google LLC\",\"info_type\":\"Content\","
                        + "\"policy_general\":\"Open\",\"info_ipv6\":true}]}")));
        wireMock.stubFor(get(urlPathEqualTo("/api/netixlan"))
                .withQueryParam("asn__in", equalTo("16509,15169"))
                .willReturn(okJson("{\"data\":["
                        + "{\"ix_id\":100,\"asn\":16509,\"speed\":100000,\"is_rs_peer\":true,\"operational\":true},"
                        + "{\"ix_id\":100,\"asn\":15169,\"speed\":40000,\"is_rs_peer\":true,\"operational\":true},"
                        + "{\"ix_id\":999,\"asn\":15169,\"speed\":10000,\"is_rs_peer\":false,\"operational\":true}]}")));
        wireMock.stubFor(get(urlPathEqualTo("/api/netfac"))
                .withQueryParam("asn__in", equalTo("16509,15169"))
                .willReturn(okJson("{\"data\":[]}")));

        PeeringIntelligenceResult result = PeeringIntelligence.builder(stubFabric(), null)
                .peeringDbBaseUrl(baseUrl())
                .addAsn(AWS, "AWS")
                .addAsn(google, "Google")
                .analyze();

        // Both networks were analyzed from the single batched response set.
        assertNotNull(result.networkPresence(AWS));
        assertNotNull(result.networkPresence(google));
        assertEquals("Amazon.com, Inc.", result.networkPresence(AWS).getPeeringDbName());
        assertEquals("Google LLC", result.networkPresence(google).getPeeringDbName());
        assertTrue(result.networkPresence(AWS).hasIxPeeringAt(MetroId.of("DC")));
        assertTrue(result.networkPresence(google).hasIxPeeringAt(MetroId.of("DC")));
        assertEquals(40000, result.networkPresence(google).getTotalIxCapacityMbps(),
                "the non-Equinix ix 999 session must still be filtered from the batched response");

        // Exactly ONE request per endpoint for the whole two-ASN analysis.
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/net")));
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/netixlan")));
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/netfac")));
    }

    @Test
    @DisplayName("analyze() with no target ASNs throws before any HTTP call")
    void analyzeRejectsNoAsns() {
        assertThrows(IllegalStateException.class, () ->
                PeeringIntelligence.builder(stubFabric(), null)
                        .peeringDbBaseUrl(baseUrl())
                        .analyze());
    }

    @Test
    @DisplayName("a PeeringDB failure surfaces as a RuntimeException out of analyze()")
    void analyzeWrapsPeeringDbFailure() {
        // Override the catalog stub with a hard error; loadEquinixCatalog() throws IOException,
        // which execute() wraps as a RuntimeException.
        wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                .willReturn(aResponse().withStatus(500).withBody("boom")));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                PeeringIntelligence.builder(stubFabric(), null)
                        .peeringDbBaseUrl(baseUrl())
                        .addAsn(AWS, "AWS")
                        .analyze());
        assertTrue(ex.getMessage().contains("PeeringDB"), () -> "message was: " + ex.getMessage());
    }

    // ---- Fabric stub (Mockito), matching PeeringDiversityEngineTest's shape ----

    private static FabricGateway stubFabric() {
        try {
            List<Metro> metros = List.of(
                    metro("DC", DC_LAT, DC_LON, List.of("DC11", "DC5")),
                    metro("SV", SV_LAT, SV_LON, List.of("SV1", "SV5")),
                    metro("DA", DA_LAT, DA_LON, List.of("DA1")));

            Metros metrosClient = mock(Metros.class);
            when(metrosClient.list()).thenReturn(new PaginatedList<>(metros, null, null, null, null));

            FabricGateway fabric = mock(FabricGateway.class);
            when(fabric.metros()).thenReturn(metrosClient);
            return fabric;
        } catch (Exception e) {
            throw new IllegalStateException("failed to build Fabric stub", e);
        }
    }

    private static Metro metro(String code, double lat, double lon, List<String> ibxs) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getIbxs()).thenReturn(ibxs);
        return m;
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
