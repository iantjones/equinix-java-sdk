package com.eqixiac.equinix.design.peering;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.geo.SpeedOfLightLatency;
import com.eqixiac.equinix.design.peering.client.PeeringDbClient;
import com.eqixiac.equinix.design.peering.client.PeeringDbFacility;
import com.eqixiac.equinix.design.peering.client.PeeringDbIx;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetFac;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetIxlan;
import com.eqixiac.equinix.design.peering.client.PeeringDbNetwork;
import com.eqixiac.equinix.design.peering.model.DiversityScore;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.peering.model.PeeringRequest;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the Peering Intelligence engine's geographic-diversity path
 * ({@code computeDiversity} → {@code resolveCoordinates}) with a stubbed {@link PeeringDbClient} and a
 * mocked {@link FabricGateway} — no network, and <em>no PeeringDB API key required</em> (the client is
 * stubbed, so the optional key is irrelevant here; in production it only raises PeeringDB's rate limit).
 *
 * <p>It proves the IBX-to-IBX grounding introduced with the speed-of-light calculator: the diversity
 * distance is computed from the centroid of each metro's actual Equinix IBX data centers (the PeeringDB
 * facility coordinates), and falls back to the Fabric metro centroid only when a metro has no facility
 * coordinates. It also confirms {@code estimatedRttMs} is the round-trip speed-of-light floor for that
 * distance.</p>
 */
@DisplayName("Peering diversity engine — IBX-grounded distance (end-to-end, stubbed)")
class PeeringDiversityEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Fabric metro centroids (what loadMetroGeo reads from fabric.metros()).
    private static final double DC_METRO_LAT = 39.0, DC_METRO_LON = -77.5;
    private static final double SV_METRO_LAT = 37.4, SV_METRO_LON = -121.9;
    private static final double DA_METRO_LAT = 32.8, DA_METRO_LON = -96.8;

    // Equinix IBX facility coordinates (PeeringDB), deliberately offset from the metro centroids so the
    // test can tell which source was used. DA's facility intentionally has NO coordinates.
    private static final double DC_FAC_LAT = 40.0, DC_FAC_LON = -75.0;
    private static final double SV_FAC_LAT = 37.0, SV_FAC_LON = -122.5;

    @Test
    @DisplayName("diversity distance uses IBX facility centroids, with a metro-centroid fallback, and wires the RTT")
    void diversityIsIbxGroundedWithFallback() throws Exception {
        FabricGateway fabric = stubFabric();
        PeeringDbClient peeringDb = new StubPeeringDbClient();

        PeeringRequest request = PeeringRequest.builder()
                .targetAsns(Map.of(16509L, "AWS"))
                .customerMetros(new LinkedHashSet<>(List.of(
                        MetroId.of("DC"), MetroId.of("SV"), MetroId.of("DA"))))
                .includeResiliency(true)
                .build();

        PeeringIntelligenceResult result =
                new PeeringIntelligenceEngine(fabric, peeringDb, request).execute();

        assertNotNull(result.getResiliency(), "resiliency assessment should be produced");
        List<DiversityScore> scores = result.getResiliency().getDiversityScores();

        // DC <-> SV: both metros have facility coordinates → distance uses the facility centroids.
        DiversityScore dcSv = find(scores, "DC", "SV");
        double facilityBased = SpeedOfLightLatency.distanceKm(DC_FAC_LAT, DC_FAC_LON, SV_FAC_LAT, SV_FAC_LON);
        double centroidBased = SpeedOfLightLatency.distanceKm(DC_METRO_LAT, DC_METRO_LON, SV_METRO_LAT, SV_METRO_LON);
        assertTrue(Math.abs(facilityBased - centroidBased) > 50,
                "test data must make facility vs metro-centroid distances clearly distinguishable");
        assertEquals(facilityBased, dcSv.getDistanceKm(), 0.5,
                "DC<->SV distance should come from the IBX facility centroids, not the Fabric metro centroid");

        // RTT is the round-trip speed-of-light floor for that (IBX-grounded) distance.
        assertEquals(SpeedOfLightLatency.roundTrip().millisForKm(dcSv.getDistanceKm()),
                dcSv.getEstimatedRttMs(), 1e-9);

        // DC <-> DA: DA's only facility has no coordinates → DA falls back to its Fabric metro centroid,
        // while DC still uses its facility centroid.
        DiversityScore dcDa = find(scores, "DC", "DA");
        double dcFacToDaCentroid = SpeedOfLightLatency.distanceKm(DC_FAC_LAT, DC_FAC_LON, DA_METRO_LAT, DA_METRO_LON);
        assertEquals(dcFacToDaCentroid, dcDa.getDistanceKm(), 0.5,
                "a metro whose facilities lack coordinates falls back to its Fabric metro centroid");
    }

    // ---- helpers ----

    private static DiversityScore find(List<DiversityScore> scores, String a, String b) {
        Set<String> want = Set.of(a, b);
        return scores.stream()
                .filter(s -> Set.of(s.getPrimaryMetro().code(), s.getBackupMetro().code()).equals(want))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no diversity score for " + a + "<->" + b + " in " + scores));
    }

    private static FabricGateway stubFabric() throws Exception {
        List<Metro> metros = List.of(
                metro("DC", DC_METRO_LAT, DC_METRO_LON, List.of("DC11", "DC5")),
                metro("SV", SV_METRO_LAT, SV_METRO_LON, List.of("SV1", "SV5")),
                metro("DA", DA_METRO_LAT, DA_METRO_LON, List.of("DA1")));

        Metros metrosClient = mock(Metros.class);
        when(metrosClient.list()).thenReturn(new PaginatedList<>(metros, null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metrosClient);
        return fabric;
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

    /**
     * A {@link PeeringDbClient} that serves canned Equinix catalog + per-ASN data with no HTTP. The
     * facilities carry the IBX code in their name (e.g. {@code "Equinix DC11 - Ashburn"}); DA's facility
     * deliberately has no coordinates to exercise the metro-centroid fallback.
     */
    private static final class StubPeeringDbClient extends PeeringDbClient {

        private final Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        private final Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        private final Map<Long, List<PeeringDbNetIxlan>> ixPresence = new LinkedHashMap<>();
        private final Map<Long, PeeringDbNetwork> networks = new LinkedHashMap<>();

        StubPeeringDbClient() {
            super((String) null);
            try {
                facs.put(1, fac(1, "Equinix DC11 - Ashburn", "Ashburn", DC_FAC_LAT, DC_FAC_LON));
                facs.put(2, fac(2, "Equinix SV5 - San Jose", "San Jose", SV_FAC_LAT, SV_FAC_LON));
                facs.put(3, facNoCoords(3, "Equinix DA1 - Dallas", "Dallas"));

                ixes.put(10, ix(10, "Equinix Ashburn", "Ashburn"));
                ixes.put(11, ix(11, "Equinix San Jose", "San Jose"));
                ixes.put(12, ix(12, "Equinix Dallas", "Dallas"));

                ixPresence.put(16509L, List.of(netixlan(10, 16509L), netixlan(11, 16509L), netixlan(12, 16509L)));
                networks.put(16509L, network(16509L, "Amazon"));
            } catch (Exception e) {
                throw new IllegalStateException("failed to build stub PeeringDB data", e);
            }
        }

        @Override public void loadEquinixCatalog() { /* no-op: catalog is pre-canned */ }

        @Override public Map<Integer, PeeringDbFacility> getEquinixFacMap() { return facs; }

        @Override public Map<Integer, PeeringDbIx> getEquinixIxMap() { return ixes; }

        @Override public PeeringDbIx getEquinixIx(int ixId) { return ixes.get(ixId); }

        @Override public PeeringDbNetwork getNetwork(long asn) { return networks.get(asn); }

        @Override public List<PeeringDbNetIxlan> getEquinixIxPresence(long asn) {
            return ixPresence.getOrDefault(asn, List.of());
        }

        @Override public List<PeeringDbNetFac> getEquinixFacPresence(long asn) { return List.of(); }
    }

    private static PeeringDbFacility fac(int id, String name, String city, double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"id\":" + id + ",\"org_id\":2,\"name\":\"" + name + "\",\"city\":\"" + city
                + "\",\"latitude\":" + lat + ",\"longitude\":" + lon + "}", PeeringDbFacility.class);
    }

    private static PeeringDbFacility facNoCoords(int id, String name, String city) throws Exception {
        return MAPPER.readValue("{\"id\":" + id + ",\"org_id\":2,\"name\":\"" + name + "\",\"city\":\"" + city + "\"}",
                PeeringDbFacility.class);
    }

    private static PeeringDbIx ix(int id, String name, String city) throws Exception {
        return MAPPER.readValue("{\"id\":" + id + ",\"org_id\":2,\"name\":\"" + name + "\",\"city\":\"" + city + "\"}",
                PeeringDbIx.class);
    }

    private static PeeringDbNetIxlan netixlan(int ixId, long asn) throws Exception {
        return MAPPER.readValue("{\"ix_id\":" + ixId + ",\"asn\":" + asn
                + ",\"speed\":100000,\"is_rs_peer\":true,\"operational\":true}", PeeringDbNetIxlan.class);
    }

    private static PeeringDbNetwork network(long asn, String name) throws Exception {
        return MAPPER.readValue("{\"asn\":" + asn + ",\"name\":\"" + name
                + "\",\"info_type\":\"Content\",\"policy_general\":\"Open\",\"info_ipv6\":true}", PeeringDbNetwork.class);
    }
}
