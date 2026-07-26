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
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.client.PeeringDbClient;
import api.equinix.javasdk.design.peering.client.PeeringDbFacility;
import api.equinix.javasdk.design.peering.client.PeeringDbIx;
import api.equinix.javasdk.design.peering.client.PeeringDbNetFac;
import api.equinix.javasdk.design.peering.client.PeeringDbNetIxlan;
import api.equinix.javasdk.design.peering.client.PeeringDbNetwork;
import api.equinix.javasdk.design.peering.enums.ConnectivityType;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.peering.model.PeeringRequest;
import api.equinix.javasdk.design.peering.model.PresenceCell;
import api.equinix.javasdk.design.peering.model.UnifiedConnectivityView;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the Fabric-availability fix: the engine now ANALYZES Fabric service
 * profiles instead of hardcoding {@code fabricAvailable(false)} into every presence cell.
 *
 * <p>Covers: real matching (cell {@code BOTH} / {@code FABRIC_CONNECTION} with the matched
 * profile's UUID wired through to the unified view), the corporate-vs-product naming bridge
 * (PeeringDB says "Amazon.com, Inc.", Fabric says "AWS Direct Connect"), the NSP
 * false-positive guard ("&lt;NSP&gt; Direct Connect" is NOT evidence of AWS), and the honesty
 * path (an unreadable profile catalog is reported as NOT analyzed, never as {@code false}).</p>
 */
@DisplayName("Peering intelligence — Fabric service-profile availability")
class PeeringFabricAvailabilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long AWS = 16509L;
    private static final long CLOUDFLARE = 13335L;

    // ---- Real matching ----

    @Test
    @DisplayName("a matching profile turns the cell to BOTH / FABRIC_CONNECTION and carries the profile UUID")
    void fabricAvailabilityIsAnalyzed() {
        FabricGateway fabric = fabricWithProfiles(
                profile("uuid-aws", "AWS Direct Connect", "DC", "DA"),
                profile("uuid-nsp", "MegaNSP Direct Connect", "DC"),
                profile("uuid-azure", "Azure ExpressRoute", "DC"));

        PeeringIntelligenceResult result = new PeeringIntelligenceEngine(
                fabric, new AwsStub(), request(Map.of(AWS, "AWS"))).execute();

        // DC: IX peering (from PeeringDB) + Fabric profile -> BOTH, with the AWS profile's uuid
        // (never the NSP's or Azure's, which must not match an AWS target).
        PresenceCell dc = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
        assertNotNull(dc);
        assertTrue(dc.isFabricAvailable(), "the AWS Direct Connect profile publishes DC");
        assertEquals(ConnectivityType.BOTH, dc.getConnectivityType());
        assertEquals("uuid-aws", dc.getFabricServiceProfileUuid());

        // DA: no IX/facility presence, but the profile publishes DA -> a Fabric-only column.
        PresenceCell da = result.getPresenceMatrix().get(AWS, MetroId.of("DA"));
        assertNotNull(da, "a Fabric-only metro still earns a presence-matrix column");
        assertTrue(da.isFabricAvailable());
        assertFalse(da.isIxPresent());
        assertEquals(ConnectivityType.FABRIC_CONNECTION, da.getConnectivityType());
        assertEquals("uuid-aws", da.getFabricServiceProfileUuid());

        // SV: IX presence but the profile does not publish SV -> IX only.
        PresenceCell sv = result.getPresenceMatrix().get(AWS, MetroId.of("SV"));
        assertNotNull(sv);
        assertFalse(sv.isFabricAvailable());
        assertEquals(ConnectivityType.IX_PEERING, sv.getConnectivityType());

        // The unified view carries the real availability and uuid (no hardcoded nulls).
        UnifiedConnectivityView view = result.unifiedView(AWS);
        assertNotNull(view);
        assertTrue(view.isFabricAvailableAnywhere());
        assertEquals(2, view.fabricMetros().size(), "DC and DA are Fabric-reachable");
        assertEquals("uuid-aws", view.forMetro(MetroId.of("DA")).getFabricServiceProfileUuid());
    }

    @Test
    @DisplayName("corporate-vs-product bridge: PeeringDB 'Amazon.com, Inc.' matches the 'AWS Direct Connect' product profile")
    void corporateNameBridgesToProductProfile() {
        FabricGateway fabric = fabricWithProfiles(
                profile("uuid-aws", "AWS Direct Connect", "DC"));

        // No caller label at all: the only evidence is the PeeringDB name "Amazon.com, Inc.",
        // whose "amazon" token resolves CloudProviderType.AWS, which then matches the PRODUCT
        // name — the exact corporate-vs-product pitfall the optimizer fix (d38ab48) addressed.
        Map<Long, String> unlabelled = new LinkedHashMap<>();
        unlabelled.put(AWS, null);

        PeeringIntelligenceResult result = new PeeringIntelligenceEngine(
                fabric, new AwsStub(), request(unlabelled)).execute();

        PresenceCell dc = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
        assertNotNull(dc);
        assertTrue(dc.isFabricAvailable(),
                "the corporate PeeringDB name must bridge to the product-named Fabric profile");
        assertEquals("uuid-aws", dc.getFabricServiceProfileUuid());
    }

    @Test
    @DisplayName("generic-token guard: an NSP's 'Direct Connect' or shared industry words are never evidence")
    void genericTokensAreNotEvidence() {
        FabricGateway fabric = fabricWithProfiles(
                profile("uuid-nsp", "MegaNSP Direct Connect", "DC"),
                profile("uuid-generic", "Global Cloud Exchange Services", "DC"),
                profile("uuid-telecom", "Vodafone Telecom Interconnect", "DC"));

        // Target "Contoso Telecom": the only distinctive token is "contoso" — sharing the
        // generic "telecom"/"direct"/"cloud" vocabulary with a profile is NOT a match.
        PeeringIntelligenceResult result = new PeeringIntelligenceEngine(
                fabric, new AwsStub(AWS, "Contoso Telecom International"),
                request(Map.of(AWS, "Contoso Telecom"))).execute();

        PresenceCell dc = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
        assertNotNull(dc);
        assertFalse(dc.isFabricAvailable(),
                "shared generic tokens must never claim a Fabric on-ramp that does not exist");
        assertNull(dc.getFabricServiceProfileUuid());
        assertEquals(ConnectivityType.IX_PEERING, dc.getConnectivityType());
    }

    @Test
    @DisplayName("a non-cloud network matches on its own brand-distinctive token")
    void brandTokenMatchesForNonCloudNetworks() {
        FabricGateway fabric = fabricWithProfiles(
                profile("uuid-cf", "Cloudflare Interconnect", "SV"));

        PeeringIntelligenceResult result = new PeeringIntelligenceEngine(
                fabric, new AwsStub(CLOUDFLARE, "Cloudflare, Inc."),
                request(Map.of(CLOUDFLARE, "Cloudflare"))).execute();

        PresenceCell sv = result.getPresenceMatrix().get(CLOUDFLARE, MetroId.of("SV"));
        assertNotNull(sv);
        assertTrue(sv.isFabricAvailable(), "the distinctive 'cloudflare' token identifies the profile");
        assertEquals("uuid-cf", sv.getFabricServiceProfileUuid());
    }

    // ---- Honesty when the catalog is unreadable ----

    @Test
    @DisplayName("an unreadable profile catalog is surfaced as NOT analyzed — never silently false")
    void unreadableCatalogIsNotAnalyzed() {
        FabricGateway fabric = fabricDcSvDa();
        when(fabric.serviceProfiles()).thenThrow(new RuntimeException("Fabric service-profile API 503"));

        PeeringIntelligenceResult result = new PeeringIntelligenceEngine(
                fabric, new AwsStub(), request(Map.of(AWS, "AWS"))).execute();

        PresenceCell dc = result.getPresenceMatrix().get(AWS, MetroId.of("DC"));
        assertNotNull(dc);
        assertFalse(dc.isFabricAvailable());
        assertEquals(ConnectivityType.IX_PEERING, dc.getConnectivityType(),
                "the core IX analysis must be unaffected");
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("NOT analyzed")),
                "the result must say Fabric availability was not analyzed; warnings were: "
                        + result.warnings());
    }

    // ---- helpers ----

    private static PeeringRequest request(Map<Long, String> targetAsns) {
        return PeeringRequest.builder()
                .targetAsns(targetAsns)
                .includeCapacity(true)
                .includePolicies(true)
                .build();
    }

    private static FabricGateway fabricWithProfiles(ServiceProfile... profiles) {
        FabricGateway fabric = fabricDcSvDa();
        ServiceProfiles profilesClient = mock(ServiceProfiles.class);
        when(profilesClient.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(profiles), null, null, null, null));
        when(fabric.serviceProfiles()).thenReturn(profilesClient);
        return fabric;
    }

    private static ServiceProfile profile(String uuid, String name, String... metroCodes) {
        ServiceProfile profile = mock(ServiceProfile.class);
        when(profile.getUuid()).thenReturn(uuid);
        when(profile.getName()).thenReturn(name);
        when(profile.metros()).thenReturn(serviceMetros(metroCodes));
        return profile;
    }

    private static List<ServiceProfileMetro> serviceMetros(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> read("{\"code\":\"" + code + "\"}", ServiceProfileMetro.class))
                .collect(java.util.stream.Collectors.toList());
    }

    private static FabricGateway fabricDcSvDa() {
        List<Metro> metros = List.of(
                metro("DC", 39.0, -77.5, List.of("DC11", "DC5")),
                metro("SV", 37.4, -121.9, List.of("SV1", "SV5")),
                metro("DA", 32.8, -96.8, List.of("DA1")));
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

    /**
     * A stub {@link PeeringDbClient}: the target ASN peers at the Equinix Ashburn (DC) and San
     * Jose (SV) IXes, with DC/SV facilities carrying IBX-coded names to seed the metro bridge.
     */
    private static class AwsStub extends PeeringDbClient {
        final Map<Integer, PeeringDbFacility> facs = new LinkedHashMap<>();
        final Map<Integer, PeeringDbIx> ixes = new LinkedHashMap<>();
        final Map<Long, List<PeeringDbNetIxlan>> ixPresence = new LinkedHashMap<>();
        final Map<Long, PeeringDbNetwork> networks = new LinkedHashMap<>();

        AwsStub() {
            this(AWS, "Amazon.com, Inc.");
        }

        AwsStub(long asn, String peeringDbName) {
            super((String) null);
            facs.put(200, read("{\"id\":200,\"org_id\":2,\"name\":\"Equinix DC11 - Ashburn\","
                    + "\"city\":\"Ashburn\",\"latitude\":39.0181,\"longitude\":-77.5389}",
                    PeeringDbFacility.class));
            facs.put(201, read("{\"id\":201,\"org_id\":2,\"name\":\"Equinix SV5 - San Jose\","
                    + "\"city\":\"San Jose\",\"latitude\":37.2431,\"longitude\":-121.7836}",
                    PeeringDbFacility.class));
            ixes.put(100, read("{\"id\":100,\"org_id\":2,\"name\":\"Equinix Ashburn\",\"city\":\"Ashburn\"}",
                    PeeringDbIx.class));
            ixes.put(101, read("{\"id\":101,\"org_id\":2,\"name\":\"Equinix San Jose\",\"city\":\"San Jose\"}",
                    PeeringDbIx.class));
            ixPresence.put(asn, List.of(
                    read("{\"ix_id\":100,\"asn\":" + asn + ",\"speed\":100000,\"is_rs_peer\":true,"
                            + "\"operational\":true}", PeeringDbNetIxlan.class),
                    read("{\"ix_id\":101,\"asn\":" + asn + ",\"speed\":40000,\"is_rs_peer\":false,"
                            + "\"operational\":true}", PeeringDbNetIxlan.class)));
            networks.put(asn, read("{\"asn\":" + asn + ",\"name\":\"" + peeringDbName + "\","
                    + "\"info_type\":\"Content\",\"policy_general\":\"Selective\",\"info_ipv6\":true}",
                    PeeringDbNetwork.class));
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
