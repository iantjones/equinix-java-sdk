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


package com.eqixiac.equinix.examples;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.examples.EndToEndDesignExample.Access;
import com.eqixiac.equinix.examples.EndToEndDesignExample.AccessDecision;
import com.eqixiac.equinix.examples.EndToEndDesignExample.ConnectivityDecision;
import com.eqixiac.equinix.examples.EndToEndDesignExample.Discovery;
import com.eqixiac.equinix.examples.EndToEndDesignExample.Inputs;
import com.eqixiac.equinix.examples.EndToEndDesignExample.Outcome;
import com.eqixiac.equinix.examples.EndToEndDesignExample.Reach;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs {@link EndToEndDesignExample} offline: a mocked Fabric catalogue of nine metros, the bundled
 * reference prices, fixed product availability and no network. It checks the decisions the example
 * makes, so the example cannot drift from the engines unnoticed.
 */
class EndToEndDesignExampleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    static Path output;

    static Outcome outcome;
    static String printed;

    @BeforeAll
    static void runOnce() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        outcome = EndToEndDesignExample.run(new Inputs(catalogue(), ReferenceRateCard.standard(),
                Discovery.of(Set.of("DC", "LD", "FR", "SG", "TY", "CH"), Set.of("DC", "FR", "SG", "TY", "LD")), null, output,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), false, null, null, null));
        printed = buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the optimizer keeps the required and pinned metros, covers three regions, and prices in USD")
    void metros() {
        List<String> metros = outcome.optimization().getRecommended().portfolio().metros().stream()
                .map(MetroId::code).toList();
        assertTrue(metros.size() == 3 || metros.size() == 4, metros.toString());
        assertTrue(metros.contains("SG"), "the pinned inference workload keeps Singapore in the design: " + metros);
        assertTrue(metros.contains("DC") && metros.contains("FR"), "the required metros are in the design: " + metros);
        assertEquals("FR", outcome.optimization().getPlacement().placement("plant-erp").orElseThrow().primary().code());
        assertTrue(outcome.optimization().getRecommended().values().get("carbon").known(),
                "the weighted Beta carbon objective is measured");
        assertEquals("USD", outcome.optimization().getRecommended().cost().currency().orElseThrow());
    }

    @Test
    @DisplayName("clouds go over Fabric and Cloud Router; peers are undecided without a peering run; internet uses EIA")
    void connectivity() {
        List<ConnectivityDecision> decisions = outcome.connectivity();
        for (String cloud : List.of("aws", "azure", "gcp")) {
            ConnectivityDecision d = decisions.stream().filter(c -> c.destination().equals(cloud)).findFirst()
                    .orElseThrow();
            assertEquals(Reach.FABRIC_CLOUD_ROUTER, d.reach());
            assertFalse(d.metros().isEmpty(), cloud + " is connected in a chosen metro");
        }
        assertEquals(3, decisions.stream().filter(c -> c.reach() == Reach.UNDECIDED).count(),
                "three peers, no Peering Intelligence result");
        ConnectivityDecision internet = decisions.get(decisions.size() - 1);
        assertEquals(Reach.INTERNET_ACCESS, internet.reach());
        assertTrue(outcome.skipped().stream().anyMatch(s -> s.contains("Peering Intelligence was not run")));
    }

    @Test
    @DisplayName("a data center metro gets redundant ports and cross connects; other metros get Network Edge")
    void access() {
        for (AccessDecision decision : outcome.access()) {
            if (decision.metro().equals("DC")) {
                assertEquals(Access.PHYSICAL_PORTS, decision.access());
                assertEquals(2, decision.orders().stream().filter(o -> o.startsWith("cross connect")).count());
            } else {
                assertEquals(Access.NETWORK_EDGE, decision.access(), decision.metro());
            }
            assertFalse(decision.orders().isEmpty());
        }
        assertEquals(outcome.optimization().getRecommended().portfolio().size(), outcome.access().size());
    }

    @Test
    @DisplayName("the plan has a Cloud Router per metro, a full-mesh backbone and BGP, and every document is written")
    void planAndDocuments() throws Exception {
        int metros = outcome.optimization().getRecommended().portfolio().size();
        assertEquals(metros, outcome.plan().getCloudRouters().size());
        assertEquals(metros * (metros - 1) / 2, outcome.plan().getBackboneLinks().size(), "a full mesh");
        assertFalse(outcome.plan().getProviderConnections().isEmpty());
        assertFalse(outcome.plan().getRoutingProtocols().isEmpty());
        assertEquals(outcome.spec().fingerprint(), outcome.plan().getSpecFingerprint());
        for (String file : List.of("spec.yaml", "optimization.md", "resilience.md", "growth.md", "decision-record.md",
                "plan.md", "plan.json", "main.tf", "topology.mmd")) {
            assertTrue(Files.size(output.resolve(file)) > 0, file);
        }
        assertNotNull(outcome.record().decision().content());
        assertTrue(Files.readString(output.resolve("main.tf")).contains("equinix_fabric_cloud_router"));
    }

    @Test
    @DisplayName("the default run deploys nothing and says which check stopped it")
    void gateStaysClosed() {
        assertFalse(outcome.gate().deployed());
        assertFalse(outcome.gate().open());
        assertTrue(outcome.gate().summary().startsWith("Not deploying:"), outcome.gate().summary());
        assertTrue(outcome.gate().checks().keySet().stream().anyMatch(k -> k.startsWith("deployment requested")));
        assertTrue(printed.contains("10. Deploy gate"), printed);
    }

    // A Fabric catalogue of nine metros in three regions, with measured round trips and cloud on-ramps.

    private static FabricGateway catalogue() {
        List<Metro> metros = List.of(
                metro("DC", "Ashburn", Region.AMER, "US", 39.0438, -77.4874, "CH:17.5", "DA:32.0", "NY:6.3",
                        "LD:73.4", "FR:84.0", "SG:215.0", "TY:150.0"),
                metro("CH", "Chicago", Region.AMER, "US", 41.8781, -87.6298, "DC:17.5", "DA:22.0", "NY:19.0"),
                metro("DA", "Dallas", Region.AMER, "US", 32.7767, -96.7970, "DC:32.0", "CH:22.0"),
                metro("NY", "New York", Region.AMER, "US", 40.7128, -74.0060, "DC:6.3", "CH:19.0", "LD:70.1"),
                metro("LD", "London", Region.EMEA, "GB", 51.5074, -0.1278, "DC:73.4", "NY:70.1", "FR:9.5",
                        "AM:6.0", "SG:170.0"),
                metro("FR", "Frankfurt", Region.EMEA, "DE", 50.1109, 8.6821, "LD:9.5", "AM:7.0", "DC:84.0",
                        "SG:160.0"),
                metro("AM", "Amsterdam", Region.EMEA, "NL", 52.3676, 4.9041, "LD:6.0", "FR:7.0"),
                metro("SG", "Singapore", Region.APAC, "SG", 1.3521, 103.8198, "TY:68.0", "DC:215.0", "LD:170.0",
                        "FR:160.0"),
                metro("TY", "Tokyo", Region.APAC, "JP", 35.6762, 139.6503, "SG:68.0", "DC:150.0"));
        List<ServiceProfile> profiles = List.of(
                profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "CH:us-east-2", "DA:us-east-1",
                        "NY:us-east-1", "LD:eu-west-2", "FR:eu-central-1", "AM:eu-west-1", "SG:ap-southeast-1",
                        "TY:ap-northeast-1"),
                profile("sp-azure", "Azure ExpressRoute", "DC:eastus", "CH:northcentralus", "LD:uksouth",
                        "FR:westeurope", "AM:westeurope", "SG:southeastasia", "TY:japaneast"),
                profile("sp-gcp", "Google Cloud Partner Interconnect Zone 1", "DC:us-east4", "LD:europe-west2",
                        "FR:europe-west3", "SG:asia-southeast1", "TY:asia-northeast1"));
        Metros metroClient = mock(Metros.class);
        when(metroClient.list()).thenAnswer(i -> new PaginatedList<>(metros, null, null, null, null));
        ServiceProfiles profileClient = mock(ServiceProfiles.class);
        when(profileClient.search()).thenAnswer(i -> new PaginatedFilteredList<>(profiles, null, null, null, null));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metroClient);
        when(fabric.serviceProfiles()).thenReturn(profileClient);
        return fabric;
    }

    private static Metro metro(String code, String name, Region region, String country, double lat, double lon,
                               String... links) {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.getCountry()).thenReturn(country);
        when(m.geoCoordinates()).thenReturn(read("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}",
                GeoCoordinate.class));
        List<ConnectedMetro> connected = new ArrayList<>();
        for (String link : links) {
            String[] parts = link.split(":");
            connected.add(read("{\"code\":\"" + parts[0] + "\",\"avgLatency\":" + parts[1] + "}", ConnectedMetro.class));
        }
        when(m.getConnectedMetros()).thenReturn(connected);
        when(m.getGeoScopes()).thenReturn(List.of());
        return m;
    }

    private static ServiceProfile profile(String uuid, String name, String... metroAndRegion) {
        ServiceProfile p = mock(ServiceProfile.class);
        when(p.getUuid()).thenReturn(uuid);
        when(p.getName()).thenReturn(name);
        List<ServiceProfileMetro> metros = new ArrayList<>();
        for (String mr : metroAndRegion) {
            String[] parts = mr.split(":");
            metros.add(read("{\"code\":\"" + parts[0] + "\",\"sellerRegions\":{\"" + parts[1] + "\":\"" + parts[1]
                    + "\"}}", ServiceProfileMetro.class));
        }
        when(p.metros()).thenReturn(metros);
        return p;
    }

    private static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
