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


package com.eqixiac.equinix.design.plan;

import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.design.plan.PlanFixtures.BGP_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.CONN_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.DIRECT_KEY;
import static com.eqixiac.equinix.design.plan.PlanFixtures.PROJECT;
import static com.eqixiac.equinix.design.plan.PlanFixtures.ROUTER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drift classification (pure: plan + live snapshot) and the checkpoint stores. */
@DisplayName("Drift detection and checkpoints")
class DriftDetectorTest {

    private static final DeploymentPlan PLAN = PlanFixtures.plan(null);

    private static LiveRouter router(String uuid, String name, String pkg) {
        return new LiveRouter(uuid, name, "PROVISIONED", "DC", pkg, PROJECT, List.of("noc@example.com"));
    }

    private static LiveConnection conn(String uuid, String name, int mbps, String aSide, Map<String, String> info) {
        return new LiveConnection(uuid, name, "PROVISIONED", "IP_VC", mbps, aSide, null, "sp-aws", "us-east-1",
                null, PROJECT, info, List.of("noc@example.com"));
    }

    private static Map<String, List<LiveRoutingProtocol>> protocols(String connUuid, String bgpPeer) {
        return Map.of(connUuid, List.of(
                new LiveRoutingProtocol("d1", "FCR-DC-to-aws-DIRECT", "DIRECT", "PROVISIONED", connUuid,
                        "10.100.0.1/30", null, null, null),
                new LiveRoutingProtocol("b1", "FCR-DC-to-aws-BGP", "BGP", "PROVISIONED", connUuid,
                        null, bgpPeer, 65100L, true)));
    }

    private static ResourceDrift drift(PlanDiff diff, String key) {
        return diff.drift(key).orElseThrow();
    }

    @Test
    @DisplayName("nothing live: every executable resource is MISSING, the Interconnect is NOT_EXECUTABLE")
    void allMissing() {
        PlanDiff diff = DriftDetector.diff(PLAN, LiveState.empty(), null);
        assertEquals(DriftStatus.MISSING, drift(diff, ROUTER_KEY).status());
        assertEquals(DriftStatus.MISSING, drift(diff, CONN_KEY).status());
        assertEquals(DriftStatus.MISSING, drift(diff, BGP_KEY).status());
        assertEquals(DriftStatus.NOT_EXECUTABLE, drift(diff, "interconnect/DC/core").status());
        assertFalse(diff.isInSync());
    }

    @Test
    @DisplayName("everything live and equal: PRESENT_MATCHING and in sync (BGP peer compared without prefix length)")
    void allMatching() {
        LiveState live = new LiveState(List.of(router("r1", "FCR-DC", "STANDARD")),
                List.of(conn("c1", "FCR-DC-to-aws", 1000, "r1", Map.of())), protocols("c1", "10.100.0.2"),
                List.of(), Instant.EPOCH);
        PlanDiff diff = DriftDetector.diff(PLAN, live, null);
        for (String key : List.of(ROUTER_KEY, CONN_KEY, DIRECT_KEY, BGP_KEY)) {
            assertEquals(DriftStatus.PRESENT_MATCHING, drift(diff, key).status(), key + ": " + diff.toMarkdown());
        }
        assertEquals("c1", drift(diff, CONN_KEY).liveUuid());
        assertEquals("name", drift(diff, CONN_KEY).matchedBy());
        assertTrue(diff.isInSync(), diff.toMarkdown());
    }

    @Test
    @DisplayName("differences are listed per field, each marked patchable or not")
    void presentDifferent() {
        LiveState live = new LiveState(List.of(router("r1", "FCR-DC", "LAB")),
                List.of(new LiveConnection("c1", "FCR-DC-to-aws", "PROVISIONED", "IP_VC", 500, "r1", null, "sp-other",
                        "us-east-1", null, PROJECT, Map.of(), List.of("noc@example.com"))),
                protocols("c1", "10.100.0.6"), List.of(), Instant.EPOCH);
        PlanDiff diff = DriftDetector.diff(PLAN, live, null);

        ResourceDrift router = drift(diff, ROUTER_KEY);
        assertEquals(DriftStatus.PRESENT_DIFFERENT, router.status());
        assertEquals(List.of(new FieldDifference("packageCode", "STANDARD", "LAB", true)), router.differences());

        ResourceDrift conn = drift(diff, CONN_KEY);
        assertEquals(DriftStatus.PRESENT_DIFFERENT, conn.status());
        assertTrue(conn.differences().contains(new FieldDifference("bandwidthMbps", "1000", "500", true)));
        assertTrue(conn.differences().contains(new FieldDifference("zSideServiceProfileUuid", "sp-aws", "sp-other", false)));
        assertFalse(conn.fullyPatchable());

        ResourceDrift bgp = drift(diff, BGP_KEY);
        assertEquals(List.of(new FieldDifference("customerPeerIpv4", "10.100.0.2", "10.100.0.6", false)), bgp.differences());
        assertEquals(DriftStatus.PRESENT_MATCHING, drift(diff, DIRECT_KEY).status());
    }

    @Test
    @DisplayName("a connection is matched by its marker even after a rename; marked resources outside the plan are UNMANAGED")
    void markerMatchingAndUnmanaged() {
        Map<String, String> ours = PlanMarker.connectionMarker("demo", CONN_KEY, "x");
        Map<String, String> stale = PlanMarker.connectionMarker("demo", "connection/DC/azure", "old");
        LiveState live = new LiveState(
                List.of(router("r1", "FCR-DC", "STANDARD"), router("r9", "FCR-SV", "STANDARD"),
                        router("r8", "OTHER-DC", "STANDARD")),
                List.of(conn("c1", "renamed", 1000, "r1", ours), conn("c2", "old-azure", 1000, "r1", stale),
                        conn("c3", "someone-elses", 1000, "r8", Map.of())),
                protocols("c1", "10.100.0.2"), List.of(), Instant.EPOCH);
        PlanDiff diff = DriftDetector.diff(PLAN, live, null);

        ResourceDrift conn = drift(diff, CONN_KEY);
        assertEquals("marker", conn.matchedBy());
        assertEquals("c1", conn.liveUuid());
        assertEquals(List.of(new FieldDifference("name", "FCR-DC-to-aws", "renamed", true)), conn.differences());

        assertEquals(2, diff.unmanaged().size(), diff.toMarkdown());
        assertTrue(diff.unmanaged().stream().anyMatch(u -> u.uuid().equals("c2") && u.reason().contains("connection/DC/azure")));
        assertTrue(diff.unmanaged().stream().anyMatch(u -> u.uuid().equals("r9") && u.kind() == ResourceKind.CLOUD_ROUTER));
        assertFalse(diff.unmanaged().stream().anyMatch(u -> u.uuid().equals("c3") || u.uuid().equals("r8")),
                "resources without the marker or prefix are not the plan's business");
    }

    @Test
    @DisplayName("a deprovisioned live resource does not count; a checkpoint uuid wins over a name match")
    void goneAndCheckpoint() {
        LiveState live = new LiveState(
                List.of(new LiveRouter("old", "FCR-DC", "DEPROVISIONED", "DC", "STANDARD", PROJECT, List.of()),
                        router("r2", "FCR-DC", "STANDARD"), router("r3", "FCR-DC", "STANDARD")),
                List.of(), Map.of(), List.of(), Instant.EPOCH);
        Checkpoint cp = Checkpoint.empty("x", "demo").with(
                new CompletedStep(ROUTER_KEY, ResourceKind.CLOUD_ROUTER, "FCR-DC", "r3", null, "PROVISIONED", true),
                Instant.EPOCH);
        ResourceDrift router = drift(DriftDetector.diff(PLAN, live, cp), ROUTER_KEY);
        assertEquals("r3", router.liveUuid());
        assertEquals("checkpoint", router.matchedBy());
    }

    @Test
    @DisplayName("protocols of a matched connection that could not be read are UNREADABLE, not MISSING")
    void unreadableProtocols() {
        LiveState live = new LiveState(List.of(router("r1", "FCR-DC", "STANDARD")),
                List.of(conn("c1", "FCR-DC-to-aws", 1000, "r1", Map.of())), Map.of(),
                List.of("403 on routing protocols"), Instant.EPOCH);
        PlanDiff diff = DriftDetector.diff(PLAN, live, null);
        assertEquals(DriftStatus.UNREADABLE, drift(diff, BGP_KEY).status());
        assertTrue(diff.notes().contains("403 on routing protocols"));
    }

    @Test
    @DisplayName("the file checkpoint store survives a new instance; the in-memory store keys by deployment")
    void checkpointStores(@TempDir Path dir) {
        Checkpoint cp = Checkpoint.empty("p1", "demo/x").with(
                new CompletedStep(ROUTER_KEY, ResourceKind.CLOUD_ROUTER, "FCR-DC", "r1", null, "PROVISIONED", true),
                Instant.parse("2026-11-12T00:00:00Z"));
        new FileCheckpointStore(dir).save(cp);
        Checkpoint back = new FileCheckpointStore(dir).load("demo/x").orElseThrow();
        assertEquals(cp, back);
        assertTrue(new FileCheckpointStore(dir).fileFor("demo/x").getFileName().toString().startsWith("demo_x"));
        new FileCheckpointStore(dir).delete("demo/x");
        assertTrue(new FileCheckpointStore(dir).load("demo/x").isEmpty());

        InMemoryCheckpointStore mem = new InMemoryCheckpointStore();
        mem.save(cp);
        assertEquals(cp, mem.load("demo/x").orElseThrow());
        assertEquals(cp.without(ROUTER_KEY, Instant.EPOCH).steps(), Map.of());
    }
}
