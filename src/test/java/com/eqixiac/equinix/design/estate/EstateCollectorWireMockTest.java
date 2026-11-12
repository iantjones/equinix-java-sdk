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

package com.eqixiac.equinix.design.estate;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.WireMockTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.AS_OF;
import static com.eqixiac.equinix.design.estate.EstateTestData.customCard;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Collects a snapshot through the real Fabric and Network Edge clients against WireMock. Fixtures
 * follow the live response structure captured read-only on 2026-11-12 (synthetic values).
 */
class EstateCollectorWireMockTest extends WireMockTestBase {

    static final String PROJECT = "aaaaaaaa-0000-4000-8000-000000000001";
    static final String C1 = "c0000000-0000-4000-8000-000000000001";
    static final String C2 = "c0000000-0000-4000-8000-000000000002";
    static final String DIR = "/json/design/estate/";

    static Fabric fabric;
    static NetworkEdge networkEdge;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        networkEdge = new NetworkEdge(testCredentials());
        redirectToWireMock(networkEdge);
    }

    @BeforeEach
    void stubs() {
        resetStubs();
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/ports"))
                .willReturn(okJson(loadFixture(DIR + "ports_list_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("0")))
                .willReturn(okJson(loadFixture(DIR + "connections_search_page1.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("2")))
                .willReturn(okJson(loadFixture(DIR + "connections_search_page2.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers/search"))
                .willReturn(okJson(loadFixture(DIR + "routers_search_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/networks/search"))
                .willReturn(okJson(loadFixture(DIR + "networks_search_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/serviceTokens/search"))
                .willReturn(aResponse().withStatus(403).withHeader("Content-Type", "application/json")
                        .withBody("[{\"errorCode\":\"EQ-3034003\",\"errorMessage\":\"Access denied\"}]")));
        wireMock.stubFor(get(urlPathEqualTo("/fabric/v4/routerPackages"))
                .willReturn(okJson(loadFixture(DIR + "router_packages_response.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/ne/v1/devices/d0000000-0000-4000-8000-000000000001"))
                .willReturn(okJson(loadFixture(DIR + "ne_device_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/metrics/search"))
                .willReturn(okJson(loadFixture(DIR + "metrics_empty_search_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(containing("/fabric/v4/connections/" + C1))
                .willReturn(okJson(loadFixture(DIR + "metrics_connection_search_response.json"))));
    }

    private static EstateCollector collector(EstateScope scope, NetworkEdge ne) {
        return new EstateCollector(fabric, ne, scope, Clock.fixed(AS_OF, ZoneOffset.UTC), null);
    }

    @Test
    void collectsAScopedSnapshotAndReScopesClientSide() {
        EstateInventory inv = collector(EstateScope.projects(PROJECT), networkEdge).collect();

        assertEquals(AS_OF, inv.asOf());
        assertEquals(2, inv.ports().size(), "the other project's port is dropped");
        EstateInventory.PortRecord p1 = inv.ports().get(0);
        assertEquals(3, p1.connectionCount(), "read from operation.connectionCount, as the live API reports it");
        assertEquals(9000, p1.usedMbps());
        assertEquals("synthetic-switch-a", p1.deviceName());
        assertEquals("DC", p1.metro());

        assertEquals(List.of(C1, C2), inv.connections().stream().map(EstateInventory.ConnectionRecord::id).toList(),
                "both pages read; the other project's connection is dropped although the server returned it");
        EstateInventory.ConnectionRecord c1 = inv.connections().get(0);
        assertEquals("COLO", c1.aSide().type());
        assertEquals("p0000000-0000-4000-8000-000000000001", c1.aSide().portId());
        assertEquals("AWS Direct Connect", c1.zSide().serviceProfileName());
        assertEquals(1, c1.termMonths());
        assertEquals("d0000000-0000-4000-8000-000000000001", inv.connections().get(1).aSide().deviceId());

        assertEquals(1, inv.routers().size());
        assertEquals("STANDARD", inv.routers().get(0).packageCode());
        assertEquals(0, inv.routers().get(0).connectionCount());
        assertEquals(java.util.Map.of("STANDARD", 10), inv.routerPackageLimits());
        assertEquals(1, inv.networks().size());
        assertEquals(1, inv.devices().size());
        assertEquals("C8000V", inv.devices().get(0).deviceType());

        assertTrue(inv.serviceTokens().isEmpty());
        assertTrue(inv.coverageGaps().stream().anyMatch(g -> g.resourceType().equals("serviceTokens")
                && g.reason().equals("FORBIDDEN") && Integer.valueOf(403).equals(g.httpStatus())),
                inv.coverageGaps().toString());
        assertEquals(3, inv.stats().perType().get("connections").read());
        assertEquals(2, inv.stats().perType().get("connections").pages());
        assertTrue(inv.stats().requests() >= 8);

        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/project/projectId")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("="))));
        assertEquals(inv, EstateInventory.fromJson(inv.toJson()));
    }

    @Test
    void stopsPagingAtTheCeilingAndSaysSoLoudly() {
        EstateInventory inv = collector(EstateScope.projects(PROJECT).withMaxResourcesPerType(2), null).collect();
        assertEquals(1, inv.connections().size(), "page 1 only: one of its two rows is in scope");
        assertTrue(inv.stats().perType().get("connections").truncated());
        assertTrue(inv.coverageGaps().stream().anyMatch(g -> g.reason().equals("TRUNCATED")
                && g.resourceType().equals("connections")));
        assertTrue(inv.stats().warnings().stream().anyMatch(w -> w.startsWith("TRUNCATED: connections")));
        assertTrue(inv.coverageGaps().stream().anyMatch(g -> g.reason().equals("NOT_REQUESTED")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.pagination.offset", equalTo("2"))));
    }

    @Test
    void explicitResourcesAreSearchedByUuid() {
        collector(EstateScope.resources(List.of(C1)), null).collect();
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/uuid")))
                .withRequestBody(matchingJsonPath("$.filter.and[0].operator", equalTo("IN"))));
    }

    @Test
    void endToEndAnalysisIsReadOnly() {
        EstateReport r = EstateAnalyzer.builder(fabric).networkEdge(networkEdge).scope(EstateScope.projects(PROJECT))
                .window(Duration.ofDays(7)).metricLimits(50, 0).rateCard(customCard())
                .thresholds(EstateThresholds.builder().minUtilizationSamples(4).build()) // the fixture has 4 hours
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).analyze();

        assertEquals(2, r.utilization().probed());
        assertEquals(1, r.utilization().withData());
        List<String> ids = r.findings().stream().map(EstateFinding::id).toList();
        // C1: p95 200 of 1000 (max of tx/rx per hour) -> 260 with headroom -> 500 tier.
        assertTrue(ids.contains("RIGHT_SIZE_DOWN:connection:" + C1), ids.toString());
        assertTrue(ids.contains("CAPACITY_RISK:port:p0000000-0000-4000-8000-000000000001"), ids.toString());
        assertTrue(ids.contains("IDLE_OR_UNUSED:port:p0000000-0000-4000-8000-000000000002"), ids.toString());
        assertTrue(ids.contains("IDLE_OR_UNUSED:cloudRouter:r0000000-0000-4000-8000-000000000001"), ids.toString());
        assertTrue(ids.contains("SINGLE_POINT_OF_FAILURE:single-device:d0000000-0000-4000-8000-000000000001"),
                ids.toString());
        assertTrue(r.coverageGaps().stream().anyMatch(g -> g.resourceType().equals("serviceTokens")));

        // One metric search per live connection, each with a single subject.
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/metrics/search")));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/metrics/search"))
                .withRequestBody(matchingJsonPath("$.filter.and[0].property", equalTo("/subject"))));
        // Read-only: nothing but GETs, searches and the token request.
        wireMock.verify(0, patchRequestedFor(anyUrl()));
        wireMock.verify(0, putRequestedFor(anyUrl()));
        wireMock.verify(0, deleteRequestedFor(anyUrl()));
        wireMock.verify(0, postRequestedFor(urlPathMatching("^(?!/oauth2/v1/token$)(?!.*/search$).*")));
    }
}
