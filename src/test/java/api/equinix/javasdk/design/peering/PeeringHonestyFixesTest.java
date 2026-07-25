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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.client.PeeringDbClient;
import api.equinix.javasdk.design.peering.client.PeeringDbFacility;
import api.equinix.javasdk.design.peering.client.PeeringDbIx;
import api.equinix.javasdk.design.peering.client.PeeringDbNetFac;
import api.equinix.javasdk.design.peering.client.PeeringDbNetIxlan;
import api.equinix.javasdk.design.peering.client.PeeringDbNetwork;
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
import api.equinix.javasdk.design.peering.enums.DiversityRating;
import api.equinix.javasdk.design.peering.model.DiversityScore;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.peering.model.PeeringRequest;
import api.equinix.javasdk.design.peering.model.PresenceCell;
import api.equinix.javasdk.design.peering.model.ResiliencyAssessment;
import api.equinix.javasdk.design.peering.model.UnifiedConnectivityView;
import api.equinix.javasdk.design.peering.model.UnifiedConnectivityView.MetroConnectivity;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the PEERING / RESILIENCY honesty fixes: unknown distance is modelled as ABSENT
 * (never a fabricated 0-km "same-site" pairing or a score-poisoning 0), swallowed data-source and
 * optional-phase failures are surfaced in a warnings channel instead of silently, the removed dead lever
 * stays removed, capacity is precision-/overflow-safe, and the {@code UNKNOWN} region sentinel is never
 * treated as a real equal region.
 */
@DisplayName("Peering honesty fixes")
class PeeringHonestyFixesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long AWS = 16509L;
    private static final long CUSTOMER = 65100L;

    @Nested
    @DisplayName("C3 — unknown distance is ABSENT, not 0 km")
    class UnknownDistanceTests {

        @Test
        @DisplayName("metros without coordinates yield an UNAVAILABLE diversity score, not a CRITICAL same-site 0 km")
        void unknownCoordinatesAreAbsent() {
            // Customer metros ZZ/QQ exist in neither Fabric nor the Equinix facility set → no coordinates.
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(AWS, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("ZZ"), MetroId.of("QQ"))))
                    .includeCapacity(true)
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcSvFabric(), new AwsStub(), request).execute();

            ResiliencyAssessment resiliency = result.getResiliency();
            assertNotNull(resiliency);
            assertEquals(1, resiliency.getDiversityScores().size());
            DiversityScore score = resiliency.getDiversityScores().get(0);

            assertTrue(score.isDistanceUnavailable(), "distance must be modelled as unavailable");
            assertEquals(DiversityRating.UNKNOWN, score.getRating());
            assertNotEquals(DiversityRating.CRITICAL, score.getRating(),
                    "unknown distance must NOT be scored as CRITICAL same-site");
            assertTrue(Double.isNaN(score.getDistanceKm()), "distance must be NaN, never a fabricated 0");
            assertTrue(Double.isNaN(score.getEstimatedRttMs()), "RTT floor must be NaN when distance is unknown");
            assertTrue(score.getExplanation().toLowerCase().contains("unavailable"),
                    "explanation must state the distance is unavailable, not '0 km apart'");
            assertFalse(score.getExplanation().contains("0 km"),
                    "explanation must not fabricate a '0 km' proximity narrative");

            // Score not poisoned toward Critical by an absent-distance placeholder.
            assertNotEquals("Critical", resiliency.getOverallRating(),
                    "an unresolved distance must not drag the overall resiliency rating to Critical");
        }

        @Test
        @DisplayName("UNKNOWN region sentinel is not treated as a real equal region")
        void unknownRegionNotEqual() {
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(AWS, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("ZZ"), MetroId.of("QQ"))))
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcSvFabric(), new AwsStub(), request).execute();

            DiversityScore score = result.getResiliency().getDiversityScores().get(0);
            // Both metros have an unknown (unloaded) region — that must NOT read as "same region".
            assertFalse(score.isSameRegion(),
                    "two metros with unknown regions must not be reported as same-region");
        }
    }

    @Nested
    @DisplayName("C5 — swallowed failures are surfaced in a warnings channel")
    class WarningsChannelTests {

        @Test
        @DisplayName("a Fabric metro-geo load failure is surfaced, never swallowed silently")
        void geoFailureSurfaced() {
            FabricGateway fabric = mock(FabricGateway.class);
            when(fabric.metros()).thenThrow(new RuntimeException("Fabric metros API 503"));

            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(AWS, "AWS"))
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(fabric, new AwsStub(), request).execute();

            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Fabric metro data")),
                    "a geo-unavailable warning must be surfaced; warnings were: " + result.warnings());
            assertFalse(result.getDataSources().contains("Equinix Fabric"),
                    "Fabric must not be credited as a source when its metro data failed to load");
        }

        @Test
        @DisplayName("an optional peering-opportunity failure is skipped-and-noted, not aborting the analysis")
        void opportunityFailureSkippedNotAborted() {
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(AWS, "AWS"))
                    .customerAsn(CUSTOMER)
                    .build();

            // The customer-ASN lookup (only reached in the opportunity phase) fails.
            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcSvFabric(), new CustomerLookupFailsStub(), request).execute();

            // Core analysis still succeeded.
            assertNotNull(result.getPresenceMatrix());
            assertNotNull(result.networkPresence(AWS));
            // Opportunities empty, and the skip is surfaced honestly.
            assertTrue(result.getPeeringOpportunities().isEmpty());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Peering-opportunity discovery was skipped")),
                    "the skipped opportunity phase must be surfaced; warnings were: " + result.warnings());
        }
    }

    @Nested
    @DisplayName("C7 — dead includeFabricConnections lever removed")
    class DeadLeverTests {

        @Test
        @DisplayName("PeeringIntelligence.Builder no longer exposes includeFabricConnections")
        void builderHasNoFabricConnectionsLever() {
            assertThrows(NoSuchMethodException.class,
                    () -> PeeringIntelligence.Builder.class.getMethod("includeFabricConnections", boolean.class));
        }

        @Test
        @DisplayName("PeeringRequest no longer carries the includeFabricConnections flag")
        void requestHasNoFabricConnectionsFlag() {
            assertThrows(NoSuchMethodException.class,
                    () -> PeeringRequest.class.getMethod("isIncludeFabricConnections"));
        }
    }

    @Nested
    @DisplayName("C9 — capacity precision and overflow are guarded")
    class CapacityTests {

        @Test
        @DisplayName("per-cell capacity sums as a long and does not overflow int")
        void perCellCapacityDoesNotOverflow() {
            // Two IX sessions in the same metro whose Mbps sum exceeds Integer.MAX_VALUE.
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(AWS, "AWS"))
                    .includeCapacity(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcOnlyFabric(), new HugeCapacityStub(), request).execute();

            PresenceCell dc = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
            assertNotNull(dc);
            assertEquals(3_000_000_000L, dc.getTotalIxCapacityMbps(),
                    "1.5 Tbps + 1.5 Tbps must sum to 3e9 as a long, not overflow to a negative int");
            assertEquals(3_000_000_000L, result.networkPresence(AWS).getTotalIxCapacityMbps());
        }

        @Test
        @DisplayName("PresenceCell capacity holds values beyond Integer.MAX_VALUE")
        void presenceCellCapacityIsLong() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroId.of("DC"))
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixPresent(true).totalIxCapacityMbps(3_000_000_000L)
                    .ixSessions(Collections.emptyList())
                    .build();
            assertEquals(3_000_000_000L, cell.getTotalIxCapacityMbps());
        }

        @Test
        @DisplayName("sub-Gbps capacity is shown in Mbps, never truncated to a misleading 0G")
        void subGbpsNotTruncated() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroId.of("DC"))
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixPresent(true).facilityPresent(false).fabricAvailable(false)
                    .totalIxCapacityMbps(500)
                    .ixSessions(Collections.emptyList())
                    .build();
            String sym = cell.detailedSymbol();
            assertTrue(sym.contains("500M"), "500 Mbps must render as 500M, was: " + sym);
            assertFalse(sym.contains("0G"), "500 Mbps must not truncate to 0G, was: " + sym);

            MetroConnectivity mc = MetroConnectivity.builder()
                    .metro(MetroId.of("DC")).connectivityType(ConnectivityType.IX_PEERING)
                    .hasIxPeering(true).hasFabric(false).ixCapacityMbps(500)
                    .ixSessions(Collections.emptyList())
                    .build();
            UnifiedConnectivityView view = UnifiedConnectivityView.builder()
                    .asn(AWS).label("AWS").metroConnectivity(List.of(mc))
                    .reachableMetroCount(1).totalIxCapacityMbps(500).fabricAvailableAnywhere(false)
                    .build();
            String md = view.toMarkdown();
            assertTrue(md.contains("500M"), "sub-Gbps capacity must show as Mbps in the unified view markdown");
            assertFalse(md.contains("| 0G "), "sub-Gbps capacity must not render as 0G");
        }

        @Test
        @DisplayName("fractional-Gbps capacity keeps one decimal instead of truncating")
        void fractionalGbpsKeepsDecimal() {
            PresenceCell cell = PresenceCell.builder()
                    .asn(AWS).metro(MetroId.of("DC"))
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixPresent(true).totalIxCapacityMbps(10500)
                    .ixSessions(Collections.emptyList())
                    .build();
            assertTrue(cell.detailedSymbol().contains("10.5G"),
                    "10.5 Gbps must not truncate to 10G, was: " + cell.detailedSymbol());
        }
    }

    // ---- Fabric stubs ----

    private static FabricGateway dcSvFabric() {
        return fabricWith(List.of(
                metro("DC", 39.0, -77.5, List.of("DC11", "DC5")),
                metro("SV", 37.4, -121.9, List.of("SV1", "SV5"))));
    }

    private static FabricGateway dcOnlyFabric() {
        return fabricWith(List.of(metro("DC", 39.0, -77.5, List.of("DC11", "DC5"))));
    }

    private static FabricGateway fabricWith(List<Metro> metros) {
        Metros metrosClient = mock(Metros.class);
        when(metrosClient.list()).thenReturn(new PaginatedList<>(metros, null, null, null, null));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metrosClient);
        return fabric;
    }

    private static Metro metro(String code, double lat, double lon, List<String> ibxs) {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getIbxs()).thenReturn(ibxs);
        return m;
    }

    private static GeoCoordinate geo(double lat, double lon) {
        try {
            return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- PeeringDB stubs ----

    /** AWS present at Equinix Ashburn (DC) and San Jose (SV) IXes, plus DC/SV facilities with coordinates. */
    private static class AwsStub extends PeeringDbClient {
        final Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        final Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        final Map<Long, List<PeeringDbNetIxlan>> ixPresence = new LinkedHashMap<>();
        final Map<Long, PeeringDbNetwork> networks = new LinkedHashMap<>();

        AwsStub() {
            super((String) null);
            facs.put(200, fac(200, "Equinix DC11 - Ashburn", "Ashburn", 39.0181, -77.5389));
            facs.put(201, fac(201, "Equinix SV5 - San Jose", "San Jose", 37.2431, -121.7836));
            ixes.put(100, ix(100, "Equinix Ashburn", "Ashburn"));
            ixes.put(101, ix(101, "Equinix San Jose", "San Jose"));
            ixPresence.put(AWS, List.of(netixlan(100, AWS, 100000), netixlan(101, AWS, 40000)));
            networks.put(AWS, network(AWS, "Amazon.com, Inc."));
        }

        @Override public void loadEquinixCatalog() { /* pre-canned */ }
        @Override public Map<Integer, PeeringDbFacility> getEquinixFacMap() { return facs; }
        @Override public Map<Integer, PeeringDbIx> getEquinixIxMap() { return ixes; }
        @Override public PeeringDbIx getEquinixIx(int ixId) { return ixes.get(ixId); }
        @Override public PeeringDbNetwork getNetwork(long asn) { return networks.get(asn); }
        // Declares throws IOException (matching the base) so subclasses can model a failing lookup.
        @Override public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) throws IOException {
            return ixPresence.getOrDefault(asn, List.of());
        }
        @Override public List<PeeringDbNetFac> getEquinixFacPresence(long asn) { return List.of(); }
    }

    /** Like {@link AwsStub}, but the customer-ASN IX-presence lookup (opportunity phase) fails hard. */
    private static final class CustomerLookupFailsStub extends AwsStub {
        @Override public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) throws IOException {
            if (asn == CUSTOMER) {
                throw new IOException("PeeringDB 500 for customer ASN");
            }
            return super.getEquinixIxPresence(asn);
        }
    }

    /** AWS with two IX sessions in the same metro (DC) whose Mbps sum exceeds Integer.MAX_VALUE. */
    private static final class HugeCapacityStub extends PeeringDbClient {
        final Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        final Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();

        HugeCapacityStub() {
            super((String) null);
            facs.put(200, fac(200, "Equinix DC11 - Ashburn", "Ashburn", 39.0181, -77.5389));
            // Two distinct Equinix IXes, both in Ashburn → both resolve to the DC metro.
            ixes.put(100, ix(100, "Equinix Ashburn", "Ashburn"));
            ixes.put(110, ix(110, "Equinix Ashburn 2", "Ashburn"));
        }

        @Override public void loadEquinixCatalog() { /* pre-canned */ }
        @Override public Map<Integer, PeeringDbFacility> getEquinixFacMap() { return facs; }
        @Override public Map<Integer, PeeringDbIx> getEquinixIxMap() { return ixes; }
        @Override public PeeringDbIx getEquinixIx(int ixId) { return ixes.get(ixId); }
        @Override public PeeringDbNetwork getNetwork(long asn) { return network(asn, "Amazon.com, Inc."); }
        @Override public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) {
            return List.of(netixlan(100, asn, 1_500_000_000), netixlan(110, asn, 1_500_000_000));
        }
        @Override public List<PeeringDbNetFac> getEquinixFacPresence(long asn) { return List.of(); }
    }

    // ---- JSON builders ----

    private static PeeringDbFacility fac(int id, String name, String city, double lat, double lon) {
        return read("{\"id\":" + id + ",\"org_id\":2,\"name\":\"" + name + "\",\"city\":\"" + city
                + "\",\"latitude\":" + lat + ",\"longitude\":" + lon + "}", PeeringDbFacility.class);
    }

    private static PeeringDbIx ix(int id, String name, String city) {
        return read("{\"id\":" + id + ",\"org_id\":2,\"name\":\"" + name + "\",\"city\":\"" + city + "\"}",
                PeeringDbIx.class);
    }

    private static PeeringDbNetIxlan netixlan(int ixId, long asn, long speed) {
        return read("{\"ix_id\":" + ixId + ",\"asn\":" + asn + ",\"speed\":" + speed
                + ",\"is_rs_peer\":true,\"operational\":true}", PeeringDbNetIxlan.class);
    }

    private static PeeringDbNetwork network(long asn, String name) {
        return read("{\"asn\":" + asn + ",\"name\":\"" + name
                + "\",\"info_type\":\"Content\",\"policy_general\":\"Selective\",\"info_ipv6\":true}",
                PeeringDbNetwork.class);
    }

    private static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("failed to build stub PeeringDB data", e);
        }
    }
}
