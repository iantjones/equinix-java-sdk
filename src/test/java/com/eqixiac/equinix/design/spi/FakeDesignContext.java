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

package com.eqixiac.equinix.design.spi;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * An in-memory {@link DesignContext} for tests: metros, latencies and cloud reachability are set
 * explicitly, so a plug-in can be tested without Fabric. Latencies are round-trip ms.
 */
public final class FakeDesignContext implements DesignContext {

    private final DesignSpec spec;
    private final Map<MetroId, MetroFacts> metros = new LinkedHashMap<>();
    private final Map<String, Double> siteLatency = new HashMap<>();
    private final Map<String, Double> metroLatency = new HashMap<>();
    private final Map<MetroId, Set<String>> clouds = new HashMap<>();
    private CostModel costModel;

    public FakeDesignContext(DesignSpec spec) {
        this.spec = spec;
    }

    public FakeDesignContext metro(String code, Region region, String... geoZones) {
        MetroId id = MetroId.of(code);
        metros.put(id, new MetroFacts(id, code, region, null, null, null, Set.of(geoZones)));
        return this;
    }

    /** Adds a metro with its ISO country code. */
    public FakeDesignContext metroIn(String code, Region region, String country) {
        MetroId id = MetroId.of(code);
        metros.put(id, new MetroFacts(id, code, region, country, null, null, Set.of()));
        return this;
    }

    /** Sets the round-trip latency from a metro to the site with the given label. */
    public FakeDesignContext latency(String metro, String siteLabel, double roundTripMs) {
        siteLatency.put(metro + "|" + siteLabel, roundTripMs);
        return this;
    }

    public FakeDesignContext metroLatency(String a, String b, double roundTripMs) {
        metroLatency.put(a + "|" + b, roundTripMs);
        metroLatency.put(b + "|" + a, roundTripMs);
        return this;
    }

    public FakeDesignContext reachable(String metro, String... cloudIds) {
        clouds.put(MetroId.of(metro), Set.of(cloudIds));
        return this;
    }

    public FakeDesignContext costModel(CostModel model) {
        this.costModel = model;
        return this;
    }

    @Override
    public DesignSpec spec() {
        return spec;
    }

    @Override
    public List<MetroFacts> metros() {
        List<MetroFacts> list = new ArrayList<>(metros.values());
        list.sort(Comparator.comparing(m -> m.id().code()));
        return list;
    }

    @Override
    public Optional<MetroFacts> metro(MetroId metro) {
        return Optional.ofNullable(metros.get(metro));
    }

    @Override
    public OptionalDouble roundTripLatencyMs(MetroId metro, SiteIntent site) {
        Double ms = siteLatency.get(metro.code() + "|" + site.label());
        return ms == null ? OptionalDouble.empty() : OptionalDouble.of(ms);
    }

    @Override
    public boolean latencyMeasured(MetroId metro, SiteIntent site) {
        return siteLatency.containsKey(metro.code() + "|" + site.label());
    }

    @Override
    public OptionalDouble roundTripLatencyMs(MetroId a, MetroId b) {
        if (a.equals(b)) {
            return OptionalDouble.of(0.5);
        }
        Double ms = metroLatency.get(a.code() + "|" + b.code());
        return ms == null ? OptionalDouble.empty() : OptionalDouble.of(ms);
    }

    @Override
    public double siteWeight(SiteIntent site) {
        double total = 0;
        for (SiteIntent s : spec.sites()) {
            total += raw(s);
        }
        return total == 0 ? 0 : raw(site) / total;
    }

    private static double raw(SiteIntent site) {
        if (site.weight() != null) {
            return site.weight();
        }
        return site.headcount() != null ? site.headcount() : 1;
    }

    @Override
    public Set<String> reachableClouds(MetroId metro) {
        return clouds.getOrDefault(metro, Set.of());
    }

    @Override
    public CostModel costModel() {
        return costModel;
    }

    @Override
    public Instant asOf() {
        return Instant.parse("2026-11-11T00:00:00Z");
    }
}
