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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.peering.enums.*;
import api.equinix.javasdk.design.peering.model.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PeeringIntelligenceResult}, including output methods
 * ({@code toSummary()}, {@code toMarkdown()}) and convenience accessors
 * ({@code unifiedView()}, {@code networkPresence()}, {@code metroReport()}).
 */
@DisplayName("PeeringIntelligenceResult")
class PeeringIntelligenceResultTest {

    private static final long AWS = 16509L;
    private static final long MSFT = 8075L;

    private PeeringIntelligenceResult result;

    @BeforeEach
    void buildResult() {
        // Build a request
        Map<Long, String> targetAsns = new LinkedHashMap<>();
        targetAsns.put(AWS, "AWS");
        targetAsns.put(MSFT, "Microsoft");

        PeeringRequest request = PeeringRequest.builder()
                .targetAsns(targetAsns)
                .customerMetros(new LinkedHashSet<>(Arrays.asList(MetroCode.DC, MetroCode.DA)))
                .customerAsn(65100L)
                .includeCapacity(true)
                .includePolicies(true)
                .includeFabricConnections(false)
                .includeResiliency(true)
                .build();

        // Build simple matrix
        List<Long> asns = Arrays.asList(AWS, MSFT);
        List<MetroCode> metros = Arrays.asList(MetroCode.DC, MetroCode.DA);
        Map<Long, Map<MetroCode, PresenceCell>> cells = new LinkedHashMap<>();

        Map<MetroCode, PresenceCell> awsRow = new LinkedHashMap<>();
        awsRow.put(MetroCode.DC, buildCell(AWS, MetroCode.DC, true, 100000));
        awsRow.put(MetroCode.DA, buildCell(AWS, MetroCode.DA, true, 10000));
        cells.put(AWS, awsRow);

        Map<MetroCode, PresenceCell> msftRow = new LinkedHashMap<>();
        msftRow.put(MetroCode.DC, buildCell(MSFT, MetroCode.DC, true, 200000));
        msftRow.put(MetroCode.DA, buildCell(MSFT, MetroCode.DA, false, 0));
        cells.put(MSFT, msftRow);

        PresenceMatrix matrix = PresenceMatrix.builder()
                .asns(asns)
                .asnLabels(new LinkedHashMap<>(targetAsns))
                .metros(metros)
                .cells(cells)
                .build();

        // Build network presences
        Map<Long, NetworkPresence> presences = new LinkedHashMap<>();
        presences.put(AWS, NetworkPresence.builder()
                .asn(AWS).label("AWS").peeringDbName("Amazon.com, Inc.")
                .peeringPolicy(PeeringPolicy.SELECTIVE).networkType(NetworkType.CONTENT)
                .trafficVolume("100-200Gbps").trafficRatio("Heavy Outbound")
                .routeServerParticipant(true).bfdSupported(true).ipv6Capable(true)
                .ixPeeringMetros(new LinkedHashSet<>(Arrays.asList(MetroCode.DC, MetroCode.DA)))
                .facilityMetros(new LinkedHashSet<>(Arrays.asList(MetroCode.DC, MetroCode.DA)))
                .allMetros(new LinkedHashSet<>(Arrays.asList(MetroCode.DC, MetroCode.DA)))
                .ixDetails(Collections.emptyList())
                .totalIxCapacityMbps(110000)
                .build());

        presences.put(MSFT, NetworkPresence.builder()
                .asn(MSFT).label("Microsoft").peeringDbName("Microsoft Corporation")
                .peeringPolicy(PeeringPolicy.OPEN).networkType(NetworkType.CONTENT)
                .trafficVolume("200-500Gbps").trafficRatio("Heavy Outbound")
                .routeServerParticipant(true).bfdSupported(true).ipv6Capable(true)
                .ixPeeringMetros(Collections.singleton(MetroCode.DC))
                .facilityMetros(Collections.singleton(MetroCode.DC))
                .allMetros(Collections.singleton(MetroCode.DC))
                .ixDetails(Collections.emptyList())
                .totalIxCapacityMbps(200000)
                .build());

        // Resiliency assessment
        ResiliencyAssessment resiliency = ResiliencyAssessment.builder()
                .overallScore(0.55)
                .overallRating("Moderate")
                .failoverPaths(Collections.emptyMap())
                .blastRadiusReports(Collections.emptyList())
                .correlatedFailures(Collections.emptyList())
                .diversityScores(Collections.emptyList())
                .findings(Collections.singletonList("Metro DC failure would impact 100% of analyzed ASN connectivity."))
                .build();

        // Metro reports
        Map<MetroCode, MetroPresenceReport> metroReports = new LinkedHashMap<>();
        metroReports.put(MetroCode.DC, MetroPresenceReport.builder()
                .metro(MetroCode.DC).metroName("Ashburn").ixCount(1).facilityCount(2)
                .asnPresence(Collections.emptyList()).build());
        metroReports.put(MetroCode.DA, MetroPresenceReport.builder()
                .metro(MetroCode.DA).metroName("Dallas").ixCount(1).facilityCount(1)
                .asnPresence(Collections.emptyList()).build());

        // Peering opportunities
        List<PeeringOpportunity> opportunities = Collections.singletonList(
                PeeringOpportunity.builder()
                        .customerAsn(65100L).targetAsn(AWS).targetLabel("AWS")
                        .metro(MetroCode.DC).ixName("Equinix Ashburn").ixId(1)
                        .targetPolicy(PeeringPolicy.SELECTIVE)
                        .targetUsesRouteServer(true).targetSpeedMbps(100000)
                        .feasibility(0.9).complexity("Negotiation Required")
                        .recommendation("Both networks are at Equinix Ashburn...")
                        .build()
        );

        result = PeeringIntelligenceResult.builder()
                .request(request)
                .presenceMatrix(matrix)
                .networkPresences(presences)
                .metroReports(metroReports)
                .resiliency(resiliency)
                .unifiedViews(null) // not requested
                .peeringOpportunities(opportunities)
                .computedAt(Instant.parse("2026-03-15T12:00:00Z"))
                .computeTimeMs(450)
                .dataSources(Arrays.asList("PeeringDB"))
                .build();
    }

    @Test
    @DisplayName("toSummary should produce concise text output")
    void toSummary() {
        String summary = result.toSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("ASNs analyzed: 2"));
        assertTrue(summary.contains("Metros with IX presence: 2"));
        assertTrue(summary.contains("Resiliency score: 55%"));
        assertTrue(summary.contains("Peering opportunities: 1"));
        assertTrue(summary.contains("450ms"));
    }

    @Test
    @DisplayName("toMarkdown should produce structured report")
    void toMarkdown() {
        String md = result.toMarkdown();
        assertNotNull(md);
        assertTrue(md.contains("# Peering Intelligence Report"));
        assertTrue(md.contains("## Presence Matrix"));
        assertTrue(md.contains("## Network Profiles"));
        assertTrue(md.contains("## Resiliency Assessment"));
        assertTrue(md.contains("## Peering Opportunities"));
        assertTrue(md.contains("AWS"));
        assertTrue(md.contains("Microsoft"));
    }

    @Test
    @DisplayName("networkPresence should return correct data for known ASN")
    void networkPresence() {
        NetworkPresence aws = result.networkPresence(AWS);
        assertNotNull(aws);
        assertEquals("AWS", aws.getLabel());
        assertEquals(PeeringPolicy.SELECTIVE, aws.getPeeringPolicy());
        assertEquals(2, aws.ixMetroCount());
    }

    @Test
    @DisplayName("networkPresence should return null for unknown ASN")
    void networkPresenceUnknown() {
        assertNull(result.networkPresence(99999L));
    }

    @Test
    @DisplayName("metroReport should return correct metro data")
    void metroReport() {
        MetroPresenceReport dcReport = result.metroReport(MetroCode.DC);
        assertNotNull(dcReport);
        assertEquals("Ashburn", dcReport.getMetroName());
        assertEquals(1, dcReport.getIxCount());
    }

    @Test
    @DisplayName("metroReport should return null for unknown metro")
    void metroReportUnknown() {
        assertNull(result.metroReport(MetroCode.SG));
    }

    @Test
    @DisplayName("unifiedView should return null when not requested")
    void unifiedViewNull() {
        assertNull(result.unifiedView(AWS));
    }

    @Test
    @DisplayName("PeeringOpportunity should carry all fields")
    void peeringOpportunity() {
        assertEquals(1, result.getPeeringOpportunities().size());
        PeeringOpportunity po = result.getPeeringOpportunities().get(0);
        assertEquals(65100L, po.getCustomerAsn());
        assertEquals(AWS, po.getTargetAsn());
        assertEquals(MetroCode.DC, po.getMetro());
        assertEquals(PeeringPolicy.SELECTIVE, po.getTargetPolicy());
        assertTrue(po.isTargetUsesRouteServer());
        assertEquals(0.9, po.getFeasibility(), 0.001);
    }

    @Nested
    @DisplayName("NetworkPresence convenience methods")
    class NetworkPresenceTests {

        @Test
        @DisplayName("hasIxPeeringAt should check correctly")
        void hasIxPeeringAt() {
            NetworkPresence aws = result.networkPresence(AWS);
            assertTrue(aws.hasIxPeeringAt(MetroCode.DC));
            assertTrue(aws.hasIxPeeringAt(MetroCode.DA));
            assertFalse(aws.hasIxPeeringAt(MetroCode.SG));
        }

        @Test
        @DisplayName("hasFacilityAt should check correctly")
        void hasFacilityAt() {
            NetworkPresence aws = result.networkPresence(AWS);
            assertTrue(aws.hasFacilityAt(MetroCode.DC));
            assertFalse(aws.hasFacilityAt(MetroCode.SG));
        }

        @Test
        @DisplayName("ixMetroCount should return correct count")
        void ixMetroCount() {
            assertEquals(2, result.networkPresence(AWS).ixMetroCount());
            assertEquals(1, result.networkPresence(MSFT).ixMetroCount());
        }
    }

    // ---- Helpers ----

    private static PresenceCell buildCell(long asn, MetroCode metro, boolean ixPresent, int capacity) {
        return PresenceCell.builder()
                .asn(asn).metro(metro)
                .connectivityType(ixPresent ? ConnectivityType.IX_PEERING : ConnectivityType.NONE)
                .ixPresent(ixPresent).fabricAvailable(false).facilityPresent(ixPresent)
                .ixSessionCount(ixPresent ? 1 : 0).totalIxCapacityMbps(capacity)
                .routeServerPeer(false).bfdSupported(false)
                .ixSessions(Collections.emptyList())
                .build();
    }
}
