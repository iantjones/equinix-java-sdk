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
import api.equinix.javasdk.design.peering.enums.DiversityRating;
import api.equinix.javasdk.design.peering.model.FailoverPath;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.peering.model.PeeringOpportunity;
import api.equinix.javasdk.design.peering.model.PeeringRequest;
import api.equinix.javasdk.design.peering.model.ResiliencyAssessment;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the resiliency-scoring, failover-ranking, opportunity-dedupe, and
 * label-null-safety fixes:
 *
 * <ul>
 *   <li>a customer metro with ZERO analyzed-ASN presence is excluded from blast scoring —
 *       having nothing at a site must never score as perfect resilience;</li>
 *   <li>failover paths are RANKED (diversity desc, capacity desc, route server) instead of
 *       leaking PeeringDB iteration order;</li>
 *   <li>one peering opportunity per (target ASN, IX) with capacity aggregated across parallel
 *       sessions — no duplicates per netixlan row;</li>
 *   <li>blast-radius labels are null-safe for {@code addAsn(long)} targets registered without
 *       a label (the request map stores a null VALUE, so getOrDefault returned null).</li>
 * </ul>
 */
@DisplayName("Peering resiliency scoring, failover ranking, opportunity dedupe, label safety")
class PeeringResiliencyAndOpportunityFixesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long TARGET = 16509L;
    private static final long CUSTOMER = 65100L;

    @Nested
    @DisplayName("Blast scoring never rewards absence")
    class BlastScoringTests {

        @Test
        @DisplayName("a customer metro with zero target presence is excluded from the blast average")
        void emptyMetroExcludedFromBlastAverage() {
            // Target peers at DC only; the customer claims DC plus ZZ (a metro with nothing).
            StubClient stub = StubClient.dcOnlyPresence();
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(TARGET, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("DC"), MetroId.of("ZZ"))))
                    .includeCapacity(true)
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcFabric(), stub, request).execute();

            ResiliencyAssessment resiliency = result.getResiliency();
            assertNotNull(resiliency);

            // DC impact is total (1/1); ZZ has NOTHING to lose and must be excluded, so the blast
            // factor is 1 - 1.0 = 0. With one HIGH correlation (-0.1), unknown-diversity neutral
            // (0.5 x 0.3) and the +0.2 base: 0*0.5 + 0.15 - 0.1 + 0.2 = 0.25. The pre-fix code
            // averaged ZZ's 0 impact in (blast factor 0.5) and yielded 0.5 — rewarding absence.
            assertEquals(0.25, resiliency.getOverallScore(), 1e-9,
                    "the empty ZZ metro must not dilute (improve) the blast score");

            assertTrue(resiliency.getFindings().stream()
                            .anyMatch(f -> f.contains("excluded from blast-radius scoring")),
                    "the exclusion must be stated in the findings; findings were: "
                            + resiliency.getFindings());
        }

        @Test
        @DisplayName("with NO relevant presence anywhere the blast factor is neutral, never perfect")
        void allEmptyMetrosScoreNeutral() {
            // The target has no Equinix presence at all; the customer's single metro is empty.
            StubClient stub = StubClient.noPresence();
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(TARGET, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("ZZ"))))
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcFabric(), stub, request).execute();

            ResiliencyAssessment resiliency = result.getResiliency();
            assertNotNull(resiliency);
            // Neutral blast factor (0.5 x 0.5) + neutral diversity (0.5 x 0.3) + 0.2 = 0.6.
            // The pre-fix code scored the empty metro's impactRatio 0 as blastScore 1.0 -> 0.85.
            assertEquals(0.6, resiliency.getOverallScore(), 1e-9,
                    "zero presence must score neutral, not perfect");
        }
    }

    @Nested
    @DisplayName("Failover paths are ranked")
    class FailoverRankingTests {

        @Test
        @DisplayName("paths are ordered by diversity rating desc, not PeeringDB iteration order")
        void rankedByDiversity() {
            // Alternates deliberately inserted worst-diversity-first: NY (~330 km, POOR),
            // DA (~1900 km, GOOD), SV (~3900 km, EXCELLENT).
            StubClient stub = StubClient.multiMetroPresence();
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(TARGET, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("DC"))))
                    .includeCapacity(true)
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcNySvDaFabric(), stub, request).execute();

            List<FailoverPath> paths = result.getResiliency().getFailoverPaths().get(MetroId.of("DC"));
            assertNotNull(paths);
            assertEquals(3, paths.size());
            assertEquals(MetroId.of("SV"), paths.get(0).getFailoverMetro(), "EXCELLENT diversity first");
            assertEquals(MetroId.of("DA"), paths.get(1).getFailoverMetro(), "GOOD diversity second");
            assertEquals(MetroId.of("NY"), paths.get(2).getFailoverMetro(), "POOR diversity last");
            assertEquals(DiversityRating.EXCELLENT, paths.get(0).getDiversity().getRating());
            assertEquals(DiversityRating.POOR, paths.get(2).getDiversity().getRating());
        }

        @Test
        @DisplayName("at equal diversity, capacity ranks first and route-server availability breaks the last tie")
        void rankedByCapacityThenRouteServer() {
            // AA/BB/CC share identical coordinates (identical diversity from DC). Sessions are
            // inserted AA(10G, no RS), CC(10G, RS), BB(40G, no RS) — expected BB, CC, AA.
            StubClient stub = StubClient.tiedDiversityPresence();
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(TARGET, "AWS"))
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("DC"))))
                    .includeCapacity(true)
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(tiedDiversityFabric(), stub, request).execute();

            List<FailoverPath> paths = result.getResiliency().getFailoverPaths().get(MetroId.of("DC"));
            assertNotNull(paths);
            assertEquals(3, paths.size());
            assertEquals(MetroId.of("BB"), paths.get(0).getFailoverMetro(), "biggest capacity first");
            assertEquals(40000, paths.get(0).getIxCapacityMbps());
            assertEquals(MetroId.of("CC"), paths.get(1).getFailoverMetro(),
                    "route-server availability wins at equal diversity and capacity");
            assertTrue(paths.get(1).isRouteServerAvailable());
            assertEquals(MetroId.of("AA"), paths.get(2).getFailoverMetro());
            assertFalse(paths.get(2).isRouteServerAvailable());
        }
    }

    @Nested
    @DisplayName("Peering opportunities are deduped per (target ASN, IX)")
    class OpportunityDedupeTests {

        @Test
        @DisplayName("parallel sessions on one IX collapse into a single aggregated opportunity")
        void parallelSessionsAggregate() {
            StubClient stub = StubClient.parallelSessions();
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(Map.of(TARGET, "AWS"))
                    .customerAsn(CUSTOMER)
                    .includeCapacity(true)
                    .includePolicies(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcSvFabric(), stub, request).execute();

            List<PeeringOpportunity> opportunities = result.getPeeringOpportunities();
            assertEquals(2, opportunities.size(),
                    "one opportunity per shared IX — two parallel ports at Ashburn must not duplicate");

            PeeringOpportunity ashburn = opportunities.stream()
                    .filter(o -> o.getIxId() == 100).findFirst().orElseThrow();
            assertEquals(2, ashburn.getTargetSessionCount(), "both parallel sessions are counted");
            assertEquals(150000L, ashburn.getTargetSpeedMbps(),
                    "capacity aggregates across the parallel sessions (100G + 50G)");
            assertTrue(ashburn.isTargetUsesRouteServer(),
                    "route-server participation is true when ANY session peers with the RS");
            assertEquals("Automatic", ashburn.getComplexity(),
                    "Open policy + route server stays Automatic on the aggregated opportunity");
            assertEquals(1.0, ashburn.getFeasibility(), 1e-9);

            PeeringOpportunity sanJose = opportunities.stream()
                    .filter(o -> o.getIxId() == 101).findFirst().orElseThrow();
            assertEquals(1, sanJose.getTargetSessionCount());
            assertEquals(40000L, sanJose.getTargetSpeedMbps());
        }
    }

    @Nested
    @DisplayName("Blast-radius labels are null-safe")
    class LabelSafetyTests {

        @Test
        @DisplayName("an addAsn(long) target without a label never yields a null blast label")
        void nullLabelFallsBack() {
            StubClient stub = StubClient.dcOnlyPresence();
            // addAsn(long) stores the ASN with a NULL label value: the key EXISTS, so the old
            // getOrDefault(asn, "AS"+asn) returned null instead of the default.
            Map<Long, String> unlabelled = new LinkedHashMap<>();
            unlabelled.put(TARGET, null);
            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(unlabelled)
                    .customerMetros(new LinkedHashSet<>(List.of(MetroId.of("DC"))))
                    .includeResiliency(true)
                    .build();

            PeeringIntelligenceResult result =
                    new PeeringIntelligenceEngine(dcFabric(), stub, request).execute();

            List<String> labels = result.getResiliency()
                    .blastRadiusFor(MetroId.of("DC")).getLostIxPeeringLabels();
            assertEquals(1, labels.size());
            assertFalse(labels.contains(null), "blast labels must never contain null");
            assertEquals("Amazon.com, Inc.", labels.get(0),
                    "the PeeringDB network name is the fallback label");
        }
    }

    // ---- Fabric stubs ----

    private static FabricGateway dcFabric() {
        return fabricWith(List.of(metro("DC", 38.9, -77.0, List.of("DC11"))));
    }

    private static FabricGateway dcSvFabric() {
        return fabricWith(List.of(
                metro("DC", 38.9, -77.0, List.of("DC11")),
                metro("SV", 37.4, -121.9, List.of("SV5"))));
    }

    private static FabricGateway dcNySvDaFabric() {
        return fabricWith(List.of(
                metro("DC", 38.9, -77.0, List.of("DC11")),
                metro("NY", 40.7, -74.0, List.of("NY1")),
                metro("DA", 32.8, -96.8, List.of("DA1")),
                metro("SV", 37.4, -121.9, List.of("SV5"))));
    }

    private static FabricGateway tiedDiversityFabric() {
        return fabricWith(List.of(
                metro("DC", 38.9, -77.0, List.of("DC11")),
                metro("AA", 10.0, 10.0, List.of("AA1")),
                metro("BB", 10.0, 10.0, List.of("BB1")),
                metro("CC", 10.0, 10.0, List.of("CC1"))));
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
        when(m.geoCoordinates()).thenReturn(read(
                "{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class));
        when(m.getIbxs()).thenReturn(ibxs);
        return m;
    }

    private static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("failed to build stub data", e);
        }
    }

    // ---- PeeringDB stub ----

    /**
     * A configurable canned {@link PeeringDbClient}. Facilities carry IBX-coded names (seeding the
     * IBX/city-to-metro bridge) and deliberately have NO coordinates, so diversity distances come
     * from the Fabric metro centroids the individual tests configure.
     */
    private static final class StubClient extends PeeringDbClient {

        private final Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        private final Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        private final Map<Long, List<PeeringDbNetIxlan>> ixPresence = new LinkedHashMap<>();
        private final Map<Long, PeeringDbNetwork> networks = new LinkedHashMap<>();

        private StubClient() {
            super((String) null);
        }

        /** Target peers at Ashburn (DC) only, Open policy. */
        static StubClient dcOnlyPresence() {
            StubClient stub = new StubClient();
            stub.addMetro(1, "DC11", "Ashburn", 10);
            stub.ixPresence.put(TARGET, List.of(stub.session(10, 100000, true)));
            stub.addNetwork(TARGET, "Amazon.com, Inc.", "Open");
            return stub;
        }

        /** Target has NO Equinix presence at all. */
        static StubClient noPresence() {
            StubClient stub = new StubClient();
            stub.addMetro(1, "DC11", "Ashburn", 10);
            stub.ixPresence.put(TARGET, List.of());
            stub.addNetwork(TARGET, "Amazon.com, Inc.", "Open");
            return stub;
        }

        /** Target peers at DC plus NY/DA/SV, inserted worst-diversity-first (NY, DA, SV). */
        static StubClient multiMetroPresence() {
            StubClient stub = new StubClient();
            stub.addMetro(1, "DC11", "Ashburn", 10);
            stub.addMetro(2, "NY1", "New York", 11);
            stub.addMetro(3, "DA1", "Dallas", 12);
            stub.addMetro(4, "SV5", "San Jose", 13);
            stub.ixPresence.put(TARGET, List.of(
                    stub.session(10, 100000, true),
                    stub.session(11, 10000, false),
                    stub.session(12, 10000, false),
                    stub.session(13, 10000, false)));
            stub.addNetwork(TARGET, "Amazon.com, Inc.", "Open");
            return stub;
        }

        /** Target peers at DC plus AA/BB/CC (identical diversity), inserted AA, CC, BB. */
        static StubClient tiedDiversityPresence() {
            StubClient stub = new StubClient();
            stub.addMetro(1, "DC11", "Ashburn", 10);
            stub.addMetro(2, "AA1", "Alphaville", 11);
            stub.addMetro(3, "BB1", "Betatown", 12);
            stub.addMetro(4, "CC1", "Gammaburg", 13);
            stub.ixPresence.put(TARGET, List.of(
                    stub.session(10, 100000, false),
                    stub.session(11, 10000, false),   // AA: 10G, no RS
                    stub.session(13, 10000, true),    // CC: 10G, RS
                    stub.session(12, 40000, false))); // BB: 40G, no RS
            stub.addNetwork(TARGET, "Amazon.com, Inc.", "Open");
            return stub;
        }

        /** Target has TWO parallel ports at Ashburn (ix 100) and one at San Jose (ix 101); the customer is at both. */
        static StubClient parallelSessions() {
            StubClient stub = new StubClient();
            stub.addMetroWithIx(1, "DC11", "Ashburn", 100);
            stub.addMetroWithIx(2, "SV5", "San Jose", 101);
            stub.ixPresence.put(TARGET, List.of(
                    stub.session(100, 100000, true),
                    stub.session(100, 50000, false),
                    stub.session(101, 40000, false)));
            stub.ixPresence.put(CUSTOMER, List.of(
                    stub.session(100, 10000, false),
                    stub.session(101, 10000, false)));
            stub.addNetwork(TARGET, "Amazon.com, Inc.", "Open");
            return stub;
        }

        private void addMetro(int facId, String ibxCode, String city, int ixId) {
            facs.put(facId, read("{\"id\":" + facId + ",\"org_id\":2,\"name\":\"Equinix " + ibxCode
                    + " - " + city + "\",\"city\":\"" + city + "\"}", PeeringDbFacility.class));
            ixes.put(ixId, read("{\"id\":" + ixId + ",\"org_id\":2,\"name\":\"Equinix " + city
                    + "\",\"city\":\"" + city + "\"}", PeeringDbIx.class));
        }

        private void addMetroWithIx(int facId, String ibxCode, String city, int ixId) {
            addMetro(facId, ibxCode, city, ixId);
        }

        private PeeringDbNetIxlan session(int ixId, long speed, boolean rsPeer) {
            return read("{\"ix_id\":" + ixId + ",\"asn\":" + TARGET + ",\"speed\":" + speed
                    + ",\"is_rs_peer\":" + rsPeer + ",\"operational\":true}", PeeringDbNetIxlan.class);
        }

        private void addNetwork(long asn, String name, String policy) {
            networks.put(asn, read("{\"asn\":" + asn + ",\"name\":\"" + name
                    + "\",\"info_type\":\"Content\",\"policy_general\":\"" + policy
                    + "\",\"info_ipv6\":true}", PeeringDbNetwork.class));
        }

        @Override public void loadEquinixCatalog() { /* pre-canned */ }
        @Override public Map<Integer, PeeringDbFacility> getEquinixFacMap() { return facs; }
        @Override public Map<Integer, PeeringDbIx> getEquinixIxMap() { return ixes; }
        @Override public PeeringDbIx getEquinixIx(int ixId) { return ixes.get(ixId); }
        @Override public PeeringDbNetwork getNetwork(long asn) { return networks.get(asn); }
        @Override public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) {
            return ixPresence.getOrDefault(asn, List.of());
        }
        @Override public List<PeeringDbNetFac> getEquinixFacPresence(long asn) { return List.of(); }
    }
}
