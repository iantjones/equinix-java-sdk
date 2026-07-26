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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression cover for {@link DeploymentTopology#forMetro(MetroId)}.
 *
 * <p><strong>What broke.</strong> {@code forMetro} filtered with {@code ==}. {@link MetroId} is a
 * value type: {@code MetroId.of(...)} allocates a fresh instance on every call, and the live
 * accessors that produce one — {@code Metro.metroId()}, {@code MetroRecommendation.getMetroId()} —
 * re-derive it per invocation. Against real data the caller's id was therefore never the same
 * object as the one stored on the placement, so {@code forMetro} matched nothing:
 * {@code MetroRecommendation.assignedWorkloads} came back empty for every recommendation and the
 * Deployment Wizard fell through to its 1000 Mbps per-connection default while reporting that it
 * had summed the workloads' bandwidth.</p>
 *
 * <p><strong>Why it was invisible.</strong> Every existing exercise of this path built its
 * placements and its lookup key from one shared {@code MetroId} instance — either a field reused
 * across the fixture or a Mockito stub, since {@code when(m.metroId()).thenReturn(MetroId.of(code))}
 * hands back the <em>same</em> object on each call. Under those conditions {@code ==} is
 * accidentally true. Every test below therefore looks the key up with an instance that is equal to,
 * but deliberately not identical to, the one the placements were built from.</p>
 */
@DisplayName("DeploymentTopology.forMetro (value equality on MetroId)")
class DeploymentTopologyTest {

    private static WorkloadPlacement placement(String label, MetroId metro) {
        return WorkloadPlacement.builder()
                .workloadLabel(label)
                .assignedMetro(metro)
                .reasoning("fixture")
                .build();
    }

    private static List<String> labels(List<WorkloadPlacement> placements) {
        return placements.stream().map(WorkloadPlacement::getWorkloadLabel).collect(Collectors.toList());
    }

    @Test
    @DisplayName("MetroId.of allocates a new instance per call, so == is not a usable comparison")
    void metroIdIsAValueTypeNotAnInternedOne() {
        // The premise of the whole suite. If MetroId ever starts interning, these assertions fail
        // loudly rather than the coverage below quietly becoming vacuous.
        assertNotSame(MetroId.of("DC"), MetroId.of("DC"),
                "MetroId.of must not be assumed to intern; forMetro cannot rely on identity");
        assertEquals(MetroId.of("DC"), MetroId.of("DC"));
        assertEquals(MetroId.of(MetroCode.DC), MetroId.of("dc"),
                "normalization makes differently-spelled codes equal");
    }

    @Test
    @DisplayName("a distinct-but-equal MetroId selects the metro's placements")
    void forMetroMatchesOnValueNotIdentity() {
        // This is the assertion that fails under `p.getAssignedMetro() == metro`.
        DeploymentTopology topology = new DeploymentTopology(List.of(
                placement("Web Tier", MetroId.of("DC")),
                placement("App Tier", MetroId.of("DC")),
                placement("Analytics", MetroId.of("SV"))));

        MetroId lookupKey = MetroId.of("DC");
        assertNotSame(topology.getPlacements().get(0).getAssignedMetro(), lookupKey,
                "the lookup key must be a different object, or this test proves nothing");

        assertEquals(List.of("Web Tier", "App Tier"), labels(topology.forMetro(lookupKey)));
        assertEquals(List.of("Analytics"), labels(topology.forMetro(MetroId.of("SV"))));
    }

    @Test
    @DisplayName("the lookup normalizes: a lower-case or enum-derived id finds the same placements")
    void forMetroUsesNormalizedValueEquality() {
        DeploymentTopology topology = new DeploymentTopology(List.of(
                placement("Web Tier", MetroId.of(MetroCode.DC)),
                placement("Analytics", MetroId.of("SV"))));

        assertEquals(List.of("Web Tier"), labels(topology.forMetro(MetroId.of("dc"))));
        assertEquals(List.of("Web Tier"), labels(topology.forMetro(MetroId.of(" DC "))));
        assertEquals(List.of("Analytics"), labels(topology.forMetro(MetroId.of(MetroCode.SV))));
    }

    @Test
    @DisplayName("a metro with no placements, an unknown metro, and null all yield an empty list")
    void forMetroIsEmptyRatherThanNullOrThrowing() {
        DeploymentTopology topology = new DeploymentTopology(List.of(
                placement("Web Tier", MetroId.of("DC"))));

        assertTrue(topology.forMetro(MetroId.of("SV")).isEmpty(), "no placements in SV");
        assertTrue(topology.forMetro(MetroId.of("XX")).isEmpty(), "metro absent from the topology");
        assertTrue(topology.forMetro(null).isEmpty(), "a null key selects nothing rather than throwing");
    }

    @Test
    @DisplayName("an empty topology answers empty for every metro")
    void emptyTopologyAnswersEmpty() {
        DeploymentTopology topology = new DeploymentTopology(List.of());
        assertTrue(topology.forMetro(MetroId.of("DC")).isEmpty());
        assertTrue(topology.forMetro(MetroId.of(MetroCode.SV)).isEmpty());
    }

    @Test
    @DisplayName("forMetro returns the placement objects themselves, in declaration order")
    void forMetroPreservesOrderAndIdentityOfPlacements() {
        WorkloadPlacement web = placement("Web Tier", MetroId.of("DC"));
        WorkloadPlacement app = placement("App Tier", MetroId.of("DC"));
        DeploymentTopology topology = new DeploymentTopology(List.of(web, app));

        List<WorkloadPlacement> selected = topology.forMetro(MetroId.of("DC"));
        assertEquals(2, selected.size());
        assertSame(web, selected.get(0));
        assertSame(app, selected.get(1));
    }

    @Test
    @DisplayName("summary() groups distinct-but-equal MetroIds into one heading, in first-seen order")
    void summaryGroupsByValueAndRendersDeterministically() {
        // groupingBy already keys on equals/hashCode; pinned here because the summary is the
        // user-facing rendering of the same relation forMetro exposes programmatically, and the two
        // must not disagree about how many metros are in play.
        DeploymentTopology topology = new DeploymentTopology(List.of(
                placement("Web Tier", MetroId.of("SV")),
                placement("Analytics", MetroId.of("DC")),
                placement("App Tier", MetroId.of("SV"))));

        String summary = topology.summary();
        assertEquals(1, countOccurrences(summary, "  SV:"), "SV must appear as a single group");
        assertEquals(1, countOccurrences(summary, "  DC:"), "DC must appear as a single group");
        assertTrue(summary.indexOf("  SV:") < summary.indexOf("  DC:"),
                "groups render in first-placement order, so the same topology renders the same way");
        assertTrue(summary.indexOf("Web Tier") < summary.indexOf("App Tier"),
                "placements within a group keep their declaration order");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
