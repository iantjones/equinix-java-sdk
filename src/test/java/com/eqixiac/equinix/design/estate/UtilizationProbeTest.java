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
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;
import com.eqixiac.equinix.fabric.model.json.MetricJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.design.estate.EstateTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class UtilizationProbeTest {

    static final String C1 = "c0000000-0000-4000-8000-000000000001";

    static List<Metric> metrics(String fixture) throws Exception {
        JsonNode root = Constants.mapper().readTree(TestFixtures.load(fixture));
        List<Metric> out = new ArrayList<>();
        for (JsonNode n : root.get("data")) {
            out.add(Constants.mapper().treeToValue(n, MetricJson.class));
        }
        return out;
    }

    @Test
    void reducesTheLargerDirectionPerIntervalToMbps() throws Exception {
        UtilizationSnapshot.Stats s = UtilizationProbe.reduce(C1, 1000,
                metrics("/json/design/estate/metrics_connection_search_response.json"));
        // per interval max(tx, rx): 100, 200, 80, 90 Mbps (the last rx point has no value).
        assertTrue(s.known());
        assertEquals(4, s.samples());
        assertEquals("PT1H", s.interval());
        assertEquals(90.0, s.p50Mbps(), 1e-9);
        assertEquals(200.0, s.p95Mbps(), 1e-9);
        assertEquals(200.0, s.peakMbps(), 1e-9);
        assertEquals(20.0, s.p95Percent().orElseThrow(), 1e-9);
    }

    @Test
    void emptyDatapointsAreUnknownNotZero() throws Exception {
        UtilizationSnapshot.Stats s = UtilizationProbe.reduce("c2", 1000,
                metrics("/json/design/estate/metrics_empty_search_response.json"));
        assertFalse(s.known());
        assertNull(s.p95Mbps());
        assertTrue(s.p95Percent().isEmpty());
        assertEquals("no datapoints in the window", s.unknownReason());
        assertFalse(UtilizationProbe.reduce("c3", 1000, List.of()).known());
    }

    @Test
    void unrecognisedUnitIsUnknown() throws Exception {
        List<Metric> m = metrics("/json/design/estate/metrics_connection_search_response.json");
        JsonNode n = Constants.mapper().readTree("{\"name\":\"x\",\"unit\":\"packets\",\"datapoints\":"
                + "[{\"endDateTime\":\"2026-11-11T01:00:00Z\",\"value\":5}]}");
        List<Metric> withOdd = new ArrayList<>(m);
        withOdd.add(Constants.mapper().treeToValue(n, MetricJson.class));
        UtilizationSnapshot.Stats s = UtilizationProbe.reduce(C1, 1000, withOdd);
        assertFalse(s.known());
        assertTrue(s.unknownReason().contains("packets"));
    }

    @Test
    void probesOnlyLiveConnectionsWithOneSubjectPerSearch() throws Exception {
        List<Metric> data = metrics("/json/design/estate/metrics_connection_search_response.json");
        List<String> subjects = new ArrayList<>();
        List<Long> pauses = new ArrayList<>();
        EstateInventory inv = inventory()
                .connection(conn(C1, 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .connection(conn("c2", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")))
                .connection(withState(conn("c3", 1000, colo("p1", "DC"), cloud("aws", "AWS", "DC")), "PENDING",
                        "PENDING_APPROVAL", daysAgo(3))).build();
        UtilizationSnapshot u = UtilizationProbe.over((MetricQuery q) -> {
                    subjects.add(q.subject());
                    MetricQuery.validate(q.toFilter());
                    return q.subject().endsWith(C1) ? data : List.of();
                })
                .window(Duration.ofDays(7)).clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).pacer(pauses::add).build()
                .probe(inv);
        assertEquals(List.of("/fabric/v4/connections/" + C1, "/fabric/v4/connections/c2"), subjects);
        assertEquals(List.of(100L), pauses, "one pause between two searches");
        assertEquals(2, u.stats().size());
        assertTrue(u.known(C1).isPresent());
        assertTrue(u.known("c2").isEmpty());
        assertEquals("no metrics returned", u.stats().get("c2").unknownReason());
        assertEquals(daysAgo(7), u.windowStart());
        assertEquals(AS_OF, u.windowEnd());
    }

    @Test
    void ceilingAndErrorsBecomeCoverageGaps() {
        EstateInventory.Builder b = inventory();
        for (int i = 0; i < 5; i++) {
            b.connection(conn("c" + i, 100, colo("p1", "DC"), cloud("aws", "AWS", "DC")));
        }
        UtilizationSnapshot u = UtilizationProbe.over((MetricQuery q) -> {
                    throw new EquinixServiceException("forbidden", 403, "/fabric/v4/metrics/search", null, null);
                })
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).maxSubjects(3).pacer(ms -> { }).build().probe(b.build());
        assertEquals(3, u.stats().size());
        assertTrue(u.stats().values().stream().noneMatch(UtilizationSnapshot.Stats::known));
        assertTrue(u.coverageGaps().stream().anyMatch(g -> g.reason().equals("TRUNCATED")));
        assertTrue(u.coverageGaps().stream().anyMatch(g -> g.reason().equals("FORBIDDEN") && g.httpStatus() == 403
                && g.detail().startsWith("3 metrics search(es) failed")));
    }

    @Test
    void stopsAfterThreeRateLimitedSearchesInARow() {
        EstateInventory.Builder b = inventory();
        for (int i = 0; i < 6; i++) {
            b.connection(conn("c" + i, 100, colo("p1", "DC"), cloud("aws", "AWS", "DC")));
        }
        int[] calls = {0};
        UtilizationSnapshot u = UtilizationProbe.over((MetricQuery q) -> {
                    calls[0]++;
                    throw new EquinixServiceException("slow down", 429, "/fabric/v4/metrics/search", null, null);
                })
                .clock(Clock.fixed(AS_OF, ZoneOffset.UTC)).pacer(ms -> { }).build().probe(b.build());
        assertEquals(3, calls[0]);
        assertTrue(u.coverageGaps().stream().anyMatch(g -> g.reason().equals("RATE_LIMITED")
                && g.detail().contains("3 resource(s) not probed")));
    }

    @Test
    void snapshotJsonRoundTrips() {
        UtilizationSnapshot u = util(flat("c1", 1000, 100, 150, 30), UtilizationSnapshot.Stats.unknown("c2", 50, "x"));
        assertEquals(u, UtilizationSnapshot.fromJson(u.toJson()));
        assertEquals(Map.of(), UtilizationSnapshot.none().stats());
    }
}
