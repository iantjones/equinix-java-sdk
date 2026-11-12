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

package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignGraphTest {

    private static DesignGraph graph(double standbyFraction) {
        FakeDesignContext ctx = ResilienceFixtures.context();
        return DesignGraphBuilder.build(ctx, MetroPortfolio.ofCodes("DC", "NY"),
                ResilienceOptions.defaults().withStandbyCapacityFraction(standbyFraction));
    }

    @Test
    @DisplayName("nodes: sites, metros, a router per metro, an on-ramp per cloud leg, clouds and workloads")
    void nodes() {
        DesignGraph g = graph(1.0);
        assertEquals(2, g.nodes(NodeKind.SITE).size());
        assertEquals(2, g.nodes(NodeKind.METRO).size());
        assertEquals(2, g.nodes(NodeKind.CLOUD_ROUTER).size());
        assertEquals(List.of("onramp:aws@DC", "onramp:aws@NY", "onramp:azure@DC", "onramp:azure@NY"),
                g.nodes(NodeKind.CLOUD_ONRAMP).stream().map(GraphNode::id).toList());
        assertEquals(2, g.nodes(NodeKind.CLOUD).size());
        assertEquals(2, g.nodes(NodeKind.WORKLOAD).size());
        assertEquals(Region.AMER, g.node("metro:DC").orElseThrow().region());
        assertEquals(Set.of("US"), g.node("metro:DC").orElseThrow().geoZones());
        assertEquals("AWS us-east-1", g.node("cloud:aws").orElseThrow().providerRegion());
        assertEquals(0.75, g.node("site:HQ").orElseThrow().weight(), 1e-9);
        assertEquals(ResilienceFixtures.spec().fingerprint(), g.specFingerprint());
        assertEquals(List.of("aws", "azure"), g.requiredClouds());
    }

    @Test
    @DisplayName("edges: primary and standby connections sized from the placement, backbone and dual-homed access")
    void edges() {
        DesignGraph g = graph(1.0);
        GraphEdge dcAws = g.edge("conn:aws@DC").orElseThrow();
        assertEquals(EdgeRole.PRIMARY, dcAws.role());
        assertEquals(1500.0, dcAws.capacityMbps(), "api 1000 + batch 500 as primary");
        GraphEdge nyAws = g.edge("conn:aws@NY").orElseThrow();
        assertEquals(EdgeRole.STANDBY, nyAws.role());
        assertEquals(1500.0, nyAws.capacityMbps(), "standby sized at full bandwidth by default");
        assertEquals(500.0, g.edge("conn:azure@NY").orElseThrow().capacityMbps());

        GraphEdge backbone = g.edge("backbone:DC-NY").orElseThrow();
        assertEquals(6.3, backbone.roundTripMs(), 1e-9);
        assertNull(backbone.capacityMbps(), "backbone capacity is not provisioned by the spec: unknown");

        assertEquals(4, g.edges(EdgeKind.SITE_ACCESS).size(), "each site homed into both metros");
        assertEquals(EdgeRole.PRIMARY, g.edge("access:HQ@DC").orElseThrow().role());
        assertEquals(EdgeRole.STANDBY, g.edge("access:HQ@NY").orElseThrow().role());
        assertEquals(6.3, g.edge("access:HQ@NY").orElseThrow().roundTripMs(), 1e-9);
        assertEquals(4, g.edges(EdgeKind.WORKLOAD_HOSTING).size());
        assertEquals(EdgeRole.STANDBY, g.edge("host:api@NY").orElseThrow().role());
    }

    @Test
    @DisplayName("standby capacity fraction and access homing are options")
    void options() {
        assertEquals(600.0, graph(0.4).edge("conn:aws@NY").orElseThrow().capacityMbps(), 1e-9);
        assertEquals(1500.0, graph(0.4).edge("conn:aws@DC").orElseThrow().capacityMbps(), 1e-9);
        DesignGraph single = DesignGraphBuilder.build(ResilienceFixtures.context(), MetroPortfolio.ofCodes("DC", "NY"),
                ResilienceOptions.defaults().withAccessHoming(1));
        assertEquals(2, single.edges(EdgeKind.SITE_ACCESS).size());
    }

    @Test
    @DisplayName("an unsized leg has unknown capacity, never zero")
    void unsized() {
        DesignSpec spec = ResilienceFixtures.specBuilder().clearWorkloads().build();
        DesignGraph g = DesignGraphBuilder.build(ResilienceFixtures.context(spec), MetroPortfolio.ofCodes("DC", "NY"),
                null);
        GraphEdge conn = g.edge("conn:aws@DC").orElseThrow();
        assertNull(conn.capacityMbps());
        assertTrue(conn.note().contains("unknown"));
    }

    @Test
    @DisplayName("Mermaid: metro subgraphs, dashed standby edges, capacity and latency labels")
    void mermaid() {
        String m = graph(1.0).toMermaid();
        assertTrue(m.startsWith("flowchart LR"));
        assertTrue(m.contains("subgraph m_DC[\"DC\"]"));
        assertTrue(m.contains("-.-"), "standby edges are dashed");
        assertTrue(m.contains("1500 Mbps"));
        assertTrue(m.contains("6.3 ms"));
        assertTrue(m.contains("((\"aws (AWS us-east-1)\"))"));
    }

    @Test
    @DisplayName("summary table lists every edge with capacity and round trip, unknown spelled out")
    void table() {
        DesignGraph g = graph(1.0);
        String t = g.toTable();
        assertEquals(g.edges().size() + 2, t.split("\n").length);
        assertTrue(t.contains("| backbone:DC-NY | backbone | Cloud Router DC | Cloud Router NY |  | unknown | 6.3 ms"));
    }

    @Test
    @DisplayName("JSON: serialisable, reads back to an equal graph, no NaN")
    void json() throws Exception {
        DesignGraph g = graph(1.0);
        String json = g.toJson();
        assertFalse(json.contains("NaN"));
        JsonNode tree = ResilienceJson.mapper().readTree(json);
        assertEquals("DC+NY", tree.get("portfolio").asText());
        DesignGraph back = ResilienceJson.mapper().readValue(json, DesignGraph.class);
        assertEquals(g, back);
    }

    @Test
    @DisplayName("latency basis comes from the catalogue context: measured, estimated or intra-metro")
    void catalogBasis() {
        DesignSpec spec = ResilienceFixtures.spec();
        CatalogDesignContext ctx = CatalogDesignContext.builder(spec)
                .metro("DC", Region.AMER, 38.9, -77.4, "US").metro("NY", Region.AMER, 40.7, -74.0, "US")
                .measuredLatency("DC", "NY", 6.3).reachable("DC", "aws", "azure").reachable("NY", "aws", "azure")
                .build();
        DesignGraph g = DesignGraphBuilder.build(ctx, MetroPortfolio.ofCodes("DC", "NY"), null);
        assertEquals("measured", g.edge("backbone:DC-NY").orElseThrow().latencyBasis());
        assertEquals("intra-metro", g.edge("access:HQ@DC").orElseThrow().latencyBasis());
        assertEquals("measured", g.edge("access:HQ@NY").orElseThrow().latencyBasis());
    }

    @Test
    @DisplayName("the graph rejects dangling edges and duplicate ids")
    void integrity() {
        GraphNode a = new GraphNode("metro:DC", NodeKind.METRO, "DC", "DC", null, null, null, null);
        GraphEdge dangling = new GraphEdge("x", EdgeKind.BACKBONE, "metro:DC", "metro:NY", null, null, null, null,
                null, null);
        assertThrows(IllegalArgumentException.class, () -> new DesignGraph("f", "n", "DC", List.of(a),
                List.of(dangling), null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DesignGraph("f", "n", "DC", List.of(a, a),
                null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new GraphEdge("y", EdgeKind.BACKBONE, "a", "b", null,
                Double.NaN, null, null, null, null));
    }
}
