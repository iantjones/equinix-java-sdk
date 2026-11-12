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

package com.eqixiac.equinix.design.placement;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;

/**
 * Places every workload of a spec on a portfolio. Pure and deterministic: the same portfolio and
 * context give the same plan.
 *
 * <p>For each workload, in spec order:</p>
 * <ol>
 *   <li>Eligible metros: its {@code pinnedMetro} only (when the portfolio holds it), otherwise every
 *       portfolio metro.</li>
 *   <li>Keep the metros that reach every cloud the workload names; when none does, keep those that
 *       reach the most and record the missing clouds as an issue.</li>
 *   <li>Keep the metros whose worst known round trip to any site is within the workload's
 *       {@code maxLatencyMs}; when none is, keep them all and record the breach as an issue. Sites whose
 *       latency is unknown are not counted against the limit.</li>
 *   <li>Rank by site-weighted round trip (known sites only), then by metro code. The first metro is the
 *       primary. When the portfolio has two or more metros and the workload is not pinned, the next
 *       ranked eligible metro is its <em>standby</em>: its cloud connections are duplicated there at full
 *       size (active/standby), which is what lets the design survive the loss of the primary metro.</li>
 * </ol>
 * <p>A cloud of the spec that no workload names is connected at the best-ranked metro that reaches
 * it (and a standby), unsized, under the label {@code cloud:<id>}. A cloud leg is only created in a
 * metro that reaches the cloud: a placement that could not honour a cloud records the gap instead.</p>
 */
public final class WorkloadPlacer {

    /** Label prefix of the placement made for a cloud that no workload names. */
    public static final String CLOUD_PREFIX = "cloud:";

    private WorkloadPlacer() {
    }

    /**
     * @param portfolio the metros
     * @param context   the facts of the run
     * @return the plan
     */
    public static PlacementPlan place(MetroPortfolio portfolio, DesignContext context) {
        List<PlacementPlan.Placement> placements = new ArrayList<>();
        Set<String> namedClouds = new HashSet<>();
        for (WorkloadIntent workload : context.spec().workloads()) {
            namedClouds.addAll(workload.clouds());
            placements.add(placeWorkload(workload, portfolio, context));
        }
        for (CloudIntent cloud : context.spec().clouds()) {
            if (!namedClouds.contains(cloud.id())) {
                WorkloadIntent pseudo = WorkloadIntent.builder().label(CLOUD_PREFIX + cloud.id())
                        .clouds(List.of(cloud.id())).build();
                placements.add(placeWorkload(pseudo, portfolio, context));
            }
        }
        return new PlacementPlan(portfolio, placements, legs(placements, context));
    }

    private static PlacementPlan.Placement placeWorkload(WorkloadIntent workload, MetroPortfolio portfolio,
                                                        DesignContext context) {
        List<String> issues = new ArrayList<>();
        List<MetroId> eligible = new ArrayList<>(portfolio.metros());
        boolean pinned = workload.pinnedMetro() != null;
        if (pinned) {
            MetroId pin = MetroId.of(workload.pinnedMetro());
            if (!portfolio.contains(pin)) {
                issues.add("pinned to " + pin.code() + ", which this design does not include");
                return new PlacementPlan.Placement(workload.label(), null, null, workload.clouds(),
                        workload.bandwidthMbps(), "not placed: pinned metro " + pin.code() + " is not in the design",
                        issues);
            }
            eligible = new ArrayList<>(List.of(pin));
        }

        boolean fullyReached = true;
        if (!workload.clouds().isEmpty()) {
            int best = 0;
            for (MetroId metro : eligible) {
                best = Math.max(best, reachedCount(workload, metro, context));
            }
            final int target = best;
            List<MetroId> reaching = new ArrayList<>();
            for (MetroId metro : eligible) {
                if (reachedCount(workload, metro, context) == target) {
                    reaching.add(metro);
                }
            }
            if (target == 0 && workload.label().startsWith(CLOUD_PREFIX)) {
                return new PlacementPlan.Placement(workload.label(), null, null, workload.clouds(), null,
                        "not connected: no metro of the design reaches it", List.of("no metro of the design reaches "
                        + String.join(", ", workload.clouds())));
            }
            if (target < workload.clouds().size()) {
                fullyReached = false;
                Set<String> missing = new LinkedHashSet<>(workload.clouds());
                missing.removeAll(context.reachableClouds(reaching.get(0)));
                issues.add("no eligible metro reaches every cloud it needs; missing " + String.join(", ", missing));
            }
            eligible = reaching;
        }

        if (workload.maxLatencyMs() != null) {
            List<MetroId> within = new ArrayList<>();
            for (MetroId metro : eligible) {
                OptionalDouble worst = worstSiteLatency(metro, context);
                if (worst.isEmpty() || worst.getAsDouble() <= workload.maxLatencyMs()) {
                    within.add(metro);
                }
            }
            if (within.isEmpty()) {
                issues.add(String.format(Locale.ROOT, "no eligible metro keeps every site within %.1f ms",
                        workload.maxLatencyMs()));
            } else {
                eligible = within;
            }
        }

        eligible.sort(Comparator.comparingDouble((MetroId m) -> weightedLatency(m, context))
                .thenComparing(MetroId::code));
        MetroId primary = eligible.get(0);
        MetroId standby = !pinned && fullyReached && eligible.size() > 1 ? eligible.get(1) : null;
        double weighted = weightedLatency(primary, context);
        StringBuilder reasoning = new StringBuilder(pinned ? "pinned to " + primary.code() : "runs in " + primary.code());
        if (Double.isFinite(weighted)) {
            reasoning.append(String.format(Locale.ROOT, " (site-weighted round trip %.1f ms)", weighted));
        } else if (!pinned) {
            reasoning.append(" (no site latency known; first eligible metro by code)");
        }
        if (!workload.clouds().isEmpty()) {
            reasoning.append(", reaching ").append(String.join(", ", workload.clouds()));
        }
        if (standby != null) {
            reasoning.append("; standby in ").append(standby.code());
        }
        return new PlacementPlan.Placement(workload.label(), primary, standby, workload.clouds(),
                workload.bandwidthMbps(), reasoning.toString(), issues);
    }

    private static int reachedCount(WorkloadIntent workload, MetroId metro, DesignContext context) {
        Set<String> reachable = context.reachableClouds(metro);
        int count = 0;
        for (String cloud : workload.clouds()) {
            if (reachable.contains(cloud)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @param metro   a metro
     * @param context the facts of the run
     * @return the site-weighted mean round trip over the sites whose latency is known (weights renormalised
     *         over them), or positive infinity when none is known
     */
    public static double weightedLatency(MetroId metro, DesignContext context) {
        double sum = 0;
        double weight = 0;
        for (SiteIntent site : context.spec().sites()) {
            OptionalDouble ms = context.roundTripLatencyMs(metro, site);
            if (ms.isPresent()) {
                double w = context.siteWeight(site);
                sum += w * ms.getAsDouble();
                weight += w;
            }
        }
        return weight > 0 ? sum / weight : Double.POSITIVE_INFINITY;
    }

    /**
     * @param metro   a metro
     * @param context the facts of the run
     * @return the worst known site round trip from the metro, or empty when no site latency is known
     */
    public static OptionalDouble worstSiteLatency(MetroId metro, DesignContext context) {
        OptionalDouble worst = OptionalDouble.empty();
        for (SiteIntent site : context.spec().sites()) {
            OptionalDouble ms = context.roundTripLatencyMs(metro, site);
            if (ms.isPresent() && (worst.isEmpty() || ms.getAsDouble() > worst.getAsDouble())) {
                worst = ms;
            }
        }
        return worst;
    }

    private static List<PlacementPlan.CloudLeg> legs(List<PlacementPlan.Placement> placements, DesignContext context) {
        Map<String, List<Object[]>> grouped = new TreeMap<>();
        for (PlacementPlan.Placement p : placements) {
            for (String cloud : p.clouds()) {
                if (p.primary() != null && context.reachableClouds(p.primary()).contains(cloud)) {
                    grouped.computeIfAbsent(p.primary().code() + "|" + cloud, k -> new ArrayList<>())
                            .add(new Object[]{p, false});
                }
                if (p.standby() != null && context.reachableClouds(p.standby()).contains(cloud)) {
                    grouped.computeIfAbsent(p.standby().code() + "|" + cloud, k -> new ArrayList<>())
                            .add(new Object[]{p, true});
                }
            }
        }
        List<PlacementPlan.CloudLeg> legs = new ArrayList<>();
        for (Map.Entry<String, List<Object[]>> entry : grouped.entrySet()) {
            String[] key = entry.getKey().split("\\|", 2);
            int mbps = 0;
            List<String> carried = new ArrayList<>();
            List<String> unsized = new ArrayList<>();
            for (Object[] item : entry.getValue()) {
                PlacementPlan.Placement p = (PlacementPlan.Placement) item[0];
                boolean standby = (Boolean) item[1];
                carried.add(p.workload() + (standby ? " (standby)" : ""));
                if (p.bandwidthMbps() == null) {
                    unsized.add(p.workload());
                } else {
                    mbps += p.bandwidthMbps();
                }
            }
            legs.add(new PlacementPlan.CloudLeg(MetroId.of(key[0]), key[1], mbps, carried, unsized));
        }
        return legs;
    }
}
