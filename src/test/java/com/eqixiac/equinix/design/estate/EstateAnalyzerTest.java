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

import com.eqixiac.equinix.core.TestFixtures;
import com.eqixiac.equinix.design.estate.rules.DefaultRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

/** Offline analysis from a stored snapshot (no API calls). */
class EstateAnalyzerTest {

    static EstateInventory snapshot() {
        return EstateInventory.fromJson(TestFixtures.load("/json/design/estate/inventory_snapshot.json"));
    }

    static UtilizationSnapshot utilisation() {
        // c-aws: p95 150 of 1000 -> 15 %, size down to 200 (150 x 1.3 = 195).
        return util(flat("c-aws", 1000, 150, 180, 168), flat("c-azure", 1000, 400, 450, 168),
                UtilizationSnapshot.Stats.unknown("c-google", 500, "no datapoints in the window"));
    }

    static EstateReport analyse() {
        return EstateAnalyzer.fromSnapshot(snapshot()).utilization(utilisation()).rateCard(customCard())
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).analyze();
    }

    @Test
    void producesTheExpectedFindingsFromTheSnapshot() {
        EstateReport r = analyse();
        List<String> ids = r.findings().stream().map(EstateFinding::id).sorted().toList();
        assertTrue(ids.contains("RIGHT_SIZE_DOWN:connection:c-aws"), ids.toString());
        assertTrue(ids.contains("CAPACITY_RISK:port:p-dc-1"), ids.toString());
        assertTrue(ids.contains("IDLE_OR_UNUSED:port:p-dc-2"), ids.toString());
        assertTrue(ids.contains("IDLE_OR_UNUSED:cloudRouter:r-sv"), ids.toString());
        assertTrue(ids.contains("SINGLE_POINT_OF_FAILURE:missing-secondary:c-aws"), ids.toString());
        assertTrue(ids.stream().anyMatch(i -> i.startsWith("CONSOLIDATE_BEHIND_ROUTER:metro-DC")), ids.toString());
        assertTrue(ids.stream().anyMatch(i -> i.startsWith("HYGIENE:stuck-connection-PENDING/PENDING_APPROVAL")),
                ids.toString());
        assertTrue(ids.stream().anyMatch(i -> i.startsWith("IDLE_OR_UNUSED:expired-service-tokens")));
        // The custom card has no term axis, so no on-demand-to-term saving is claimed.
        assertTrue(r.findings(FindingCategory.ON_DEMAND_TO_TERM).isEmpty());
        assertEquals(List.of("right-size", "on-demand-to-term", "consolidate-behind-router", "idle-or-unused",
                "single-point-of-failure", "capacity-risk", "hygiene"), r.rulesRun());
    }

    @Test
    void addressableSavingCountsOnlyPricedSavingsOncePerResource() {
        EstateReport r = analyse();
        // Right-size c-aws 300 -> 150 = 150; idle router 1500. Added costs (secondaries, router design) are excluded.
        assertEquals(0, new BigDecimal("1650.00").compareTo(r.addressableMonthlySaving().get("USD").monthly()));
        assertEquals(2, r.addressableMonthlySaving().get("USD").findings());
        assertTrue(r.unpriced().stream().anyMatch(u -> u.findingId().equals("IDLE_OR_UNUSED:port:p-dc-2")));
    }

    @Test
    void reportsWhatWasNotAnalysed() {
        EstateReport r = analyse();
        assertTrue(r.notAnalysed().stream().anyMatch(n -> n.startsWith("Utilisation: 1 of 3 live connection(s)")),
                r.notAnalysed().toString());
        assertTrue(r.notAnalysed().stream().anyMatch(n -> n.startsWith("1 connection(s) are not provisioned")));
        assertTrue(r.notAnalysed().stream().anyMatch(n -> n.startsWith("Cloud Router capacity: no connection limit")));
        assertTrue(r.coverageGaps().stream().anyMatch(g -> g.reason().equals("NOT_REQUESTED")));
        String md = r.toMarkdown();
        assertTrue(md.contains("## What was not analysed, and why"));
        assertTrue(md.contains("## Prioritised actions"));
        assertTrue(md.contains("Addressable saving: **USD 1650.00/month**"), md);
    }

    @Test
    void sameInputsGiveTheSameReportWhateverTheRuleOrder() {
        EstateReport a = analyse();
        List<EstateRule> shuffled = new ArrayList<>(DefaultRules.all());
        Collections.reverse(shuffled);
        EstateReport b = EstateAnalyzer.fromSnapshot(snapshot()).utilization(utilisation()).rateCard(customCard())
                .rules(shuffled).clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).analyze();
        assertEquals(a.findings(), b.findings());
        assertEquals(a.actions(), b.actions());
    }

    @Test
    void aFailingRuleIsAWarningNotAFailedRun() {
        EstateReport r = EstateAnalyzer.fromSnapshot(snapshot()).rules(List.of(new EstateRule() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<EstateFinding> evaluate(EstateInventory i, UtilizationSnapshot u, RuleContext c) {
                throw new IllegalStateException("boom");
            }
        })).addRule(new com.eqixiac.equinix.design.estate.rules.HygieneRule())
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).analyze();
        assertTrue(r.warnings().stream().anyMatch(w -> w.startsWith("Rule broken failed")));
        assertFalse(r.findings().isEmpty(), "the other rule still ran");
    }

    @Test
    void withoutARateCardEverythingIsUnpricedAndSaysSo() {
        EstateReport r = EstateAnalyzer.fromSnapshot(snapshot()).utilization(utilisation()).rateCard(null)
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).analyze();
        assertTrue(r.addressableMonthlySaving().isEmpty());
        assertTrue(r.findings().stream().noneMatch(f -> f.impact().isPriced()));
        assertTrue(r.notAnalysed().stream().anyMatch(n -> n.startsWith("Pricing: no rate card")));
    }

    @Test
    void liveRunNeedsAScope() {
        com.eqixiac.equinix.Fabric fabric = new com.eqixiac.equinix.Fabric(
                new com.eqixiac.equinix.core.auth.BasicEquinixCredentials("id", "secret"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> EstateAnalyzer.builder(fabric).analyze());
        assertTrue(e.getMessage().contains("needs a scope"));
        assertThrows(IllegalArgumentException.class, () -> new EstateCollector(fabric, null,
                new EstateScope(java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), false, 10), null, null));
    }

    @Test
    void designFacadeOffersTheAnalyzerOnlyOverAFullFabricClient() {
        com.eqixiac.equinix.Fabric fabric = new com.eqixiac.equinix.Fabric(
                new com.eqixiac.equinix.core.auth.BasicEquinixCredentials("id", "secret"));
        assertNotNull(com.eqixiac.equinix.Design.over(fabric).analyzeEstate());
        com.eqixiac.equinix.FabricGateway gateway = org.mockito.Mockito.mock(com.eqixiac.equinix.FabricGateway.class);
        assertThrows(IllegalStateException.class, () -> com.eqixiac.equinix.Design.over(gateway).analyzeEstate());
    }
}
