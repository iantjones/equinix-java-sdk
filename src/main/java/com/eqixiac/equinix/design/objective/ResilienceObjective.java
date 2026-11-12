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

package com.eqixiac.equinix.design.objective;

import com.eqixiac.equinix.design.spi.ValueScale;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;

/**
 * What survives the worst single-metro loss, 0 to 100: the mean of up to three shares, each measured
 * after removing the metro whose loss hurts most.
 *
 * <ul>
 *   <li><b>sites</b>: share of site weight that still has a metro with known latency (within the spec's
 *       {@code maxLatencyMs} when set);</li>
 *   <li><b>clouds</b>: share of the required clouds the design connects to that are still connected
 *       (a cloud leg of the {@link WorkloadPlacer placement} survives in another metro);</li>
 *   <li><b>workloads</b>: share of placed workloads (not the placements made for clouds no workload
 *       names) whose primary or standby metro survives.</li>
 * </ul>
 * <p>A share is left out when it has nothing to measure. A one-metro design scores 0. The explanation
 * also states the same measure for the worst single-<em>region</em> loss. Unknown when there are no
 * sites, no connected required clouds and no placed workloads.</p>
 */
public final class ResilienceObjective implements Objective {

    @Override
    public String id() {
        return RESILIENCE;
    }

    @Override
    public Direction direction() {
        return Direction.MAXIMIZE;
    }

    /** A percentage is its own score. */
    @Override
    public ValueScale scale(DesignContext context) {
        return ValueScale.percent();
    }

    @Override
    public String unit() {
        return "%";
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        Double maxMs = context.spec().constraints().maxLatencyMs();

        double worst = Double.POSITIVE_INFINITY;
        String worstDescription = null;
        for (MetroId lost : portfolio.metros()) {
            Survival s = survival(portfolio, Set.of(lost), plan, context, maxMs);
            if (s == null) {
                return ObjectiveValue.unknown("nothing to protect: no sites with known latency, no connected required "
                        + "cloud and no placed workload");
            }
            if (s.value < worst) {
                worst = s.value;
                worstDescription = "losing " + lost.code() + " leaves " + s.describe();
            }
        }

        Map<Region, Set<MetroId>> regions = new TreeMap<>();
        for (MetroId metro : portfolio.metros()) {
            Region region = context.metro(metro).map(MetroFacts::region).orElse(null);
            if (region != null) {
                regions.computeIfAbsent(region, k -> new LinkedHashSet<>()).add(metro);
            }
        }
        double worstRegion = Double.POSITIVE_INFINITY;
        String regionDescription = "";
        for (Map.Entry<Region, Set<MetroId>> region : regions.entrySet()) {
            Survival s = survival(portfolio, region.getValue(), plan, context, maxMs);
            if (s != null && s.value < worstRegion) {
                worstRegion = s.value;
                regionDescription = String.format(Locale.ROOT, "; worst region loss (%s) leaves %.0f%%",
                        region.getKey(), s.value);
            }
        }
        return ObjectiveValue.of(worst, String.format(Locale.ROOT, "%.0f%% survives the worst single-metro loss (%s)%s",
                worst, worstDescription, regionDescription));
    }

    private record Survival(double value, List<String> parts) {
        String describe() {
            return String.join(", ", parts);
        }
    }

    private static Survival survival(MetroPortfolio portfolio, Set<MetroId> lost, PlacementPlan plan,
                                     DesignContext context, Double maxMs) {
        List<MetroId> remaining = new ArrayList<>(portfolio.metros());
        remaining.removeAll(lost);
        List<Double> shares = new ArrayList<>();
        List<String> parts = new ArrayList<>();

        double siteTotal = 0;
        double siteKept = 0;
        for (SiteIntent site : context.spec().sites()) {
            if (!hasLatency(portfolio.metros(), site, context, null)) {
                continue;
            }
            double w = context.siteWeight(site);
            siteTotal += w;
            if (hasLatency(remaining, site, context, maxMs)) {
                siteKept += w;
            }
        }
        if (siteTotal > 0) {
            shares.add(100.0 * siteKept / siteTotal);
            parts.add(String.format(Locale.ROOT, "%.0f%% of site weight served", 100.0 * siteKept / siteTotal));
        }

        Set<String> connected = new LinkedHashSet<>();
        Set<String> stillConnected = new LinkedHashSet<>();
        for (PlacementPlan.CloudLeg leg : plan.legs()) {
            CloudIntent cloud = context.spec().cloud(leg.cloudId());
            if (cloud == null || !Boolean.TRUE.equals(cloud.required())) {
                continue;
            }
            connected.add(leg.cloudId());
            if (!lost.contains(leg.metro())) {
                stillConnected.add(leg.cloudId());
            }
        }
        if (!connected.isEmpty()) {
            shares.add(100.0 * stillConnected.size() / connected.size());
            parts.add(stillConnected.size() + "/" + connected.size() + " required clouds connected");
        }

        int placed = 0;
        int kept = 0;
        for (PlacementPlan.Placement p : plan.placements()) {
            if (p.primary() == null || p.workload().startsWith(WorkloadPlacer.CLOUD_PREFIX)) {
                continue;
            }
            placed++;
            if (!lost.contains(p.primary()) || (p.standby() != null && !lost.contains(p.standby()))) {
                kept++;
            }
        }
        if (placed > 0) {
            shares.add(100.0 * kept / placed);
            parts.add(kept + "/" + placed + " workloads running");
        }
        if (shares.isEmpty()) {
            return null;
        }
        double mean = shares.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return new Survival(mean, parts);
    }

    private static boolean hasLatency(List<MetroId> metros, SiteIntent site, DesignContext context, Double maxMs) {
        for (MetroId metro : metros) {
            OptionalDouble ms = context.roundTripLatencyMs(metro, site);
            if (ms.isPresent() && (maxMs == null || ms.getAsDouble() <= maxMs)) {
                return true;
            }
        }
        return false;
    }
}
