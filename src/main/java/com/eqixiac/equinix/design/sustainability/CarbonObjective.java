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
package com.eqixiac.equinix.design.sustainability;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.ValueScale;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@value #ID}: the grid carbon intensity of where a portfolio runs its workloads, in
 * {@value CarbonIntensityTable#UNIT}, from a {@link CarbonIntensityTable}. Smaller is better.
 *
 * <p><strong>Beta.</strong> The objective, its scale and the bundled reference table may change in a minor
 * release. It is a siting indicator, not an emissions estimate: it knows the grid intensity of a metro's
 * country (or the caller's own figure for the metro) and no power draw of ports, routers or colocation.</p>
 *
 * <p>The value is the mean intensity of the metros the workloads' primaries run in (the
 * {@code WorkloadPlacer}'s placement), weighted by each workload's bandwidth (an unsized workload, or a
 * design whose workloads are all unsized, counts 1 per workload); a portfolio with no placed workload is
 * the plain mean over its metros.</p>
 *
 * <p>Scoring uses a fixed scale: 0 {@value CarbonIntensityTable#UNIT} scores 100 and
 * {@value #WORST_GRAMS_PER_KWH} scores 0 (an SDK assumption: about a coal-dominated grid), so a small
 * difference stays small whatever other designs are compared.</p>
 *
 * <p>A metro with no figure, of its own or for its country, makes the value unknown and names the metro;
 * nothing is filled in. With no table the objective is inert (unknown for every portfolio).</p>
 */
public final class CarbonObjective implements Objective {

    /** The objective id. */
    public static final String ID = "carbon";

    /** The intensity that scores 0, in {@value CarbonIntensityTable#UNIT}. */
    public static final double WORST_GRAMS_PER_KWH = 700.0;

    private final CarbonIntensityTable table;
    private final boolean onlyWhenWeighted;

    /**
     * @param table the intensity table, or {@code null} for an inert objective
     */
    public CarbonObjective(CarbonIntensityTable table) {
        this(table, false);
    }

    /**
     * @param table            the intensity table, or {@code null} for an inert objective
     * @param onlyWhenWeighted when {@code true}, a spec that gives {@value #ID} no positive weight gets
     *                         unknown for every portfolio, so the objective neither ranks nor enters the
     *                         frontier unless it was asked for
     */
    public CarbonObjective(CarbonIntensityTable table, boolean onlyWhenWeighted) {
        this.table = table;
        this.onlyWhenWeighted = onlyWhenWeighted;
    }

    /** A fixed scale: 0 scores 100, {@value #WORST_GRAMS_PER_KWH} scores 0. */
    @Override
    public ValueScale scale(DesignContext context) {
        return ValueScale.anchored(0.0, WORST_GRAMS_PER_KWH);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Direction direction() {
        return Direction.MINIMIZE;
    }

    @Override
    public String unit() {
        return CarbonIntensityTable.UNIT;
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        if (table == null) {
            return ObjectiveValue.unknown("no carbon-intensity table supplied");
        }
        if (onlyWhenWeighted) {
            Double w = WeightProfiles.resolve(context.spec().objectives()).get(ID);
            if (w == null || w <= 0) {
                return ObjectiveValue.unknown("not measured: the spec gives '" + ID + "' no weight");
            }
        }
        Map<MetroId, Double> weights = new LinkedHashMap<>();
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        for (PlacementPlan.Placement p : plan.placements()) {
            if (p.primary() != null && !p.workload().startsWith(WorkloadPlacer.CLOUD_PREFIX)) {
                weights.merge(p.primary(), p.bandwidthMbps() == null ? 1.0 : p.bandwidthMbps().doubleValue(),
                        Double::sum);
            }
        }
        if (weights.isEmpty()) {
            for (MetroId m : portfolio.metros()) {
                weights.put(m, 1.0);
            }
        }
        List<String> missing = new ArrayList<>();
        double sum = 0;
        double total = 0;
        for (Map.Entry<MetroId, Double> e : weights.entrySet()) {
            String country = context.metro(e.getKey()).map(MetroFacts::country).orElse(null);
            Double g = table.intensity(e.getKey().code(), country);
            if (g == null) {
                missing.add(e.getKey().code());
                continue;
            }
            sum += g * e.getValue();
            total += e.getValue();
        }
        if (!missing.isEmpty()) {
            return ObjectiveValue.unknown("no intensity for " + String.join(", ", missing) + " in the table ("
                    + table.source() + ")");
        }
        return ObjectiveValue.of(sum / total, String.format(Locale.ROOT,
                "Beta: %.0f %s, load-weighted over %s (%s, %s, %s)",
                sum / total, CarbonIntensityTable.UNIT, weights.keySet().stream().map(MetroId::code).toList(),
                table.source(), table.asOf(), table.method()));
    }
}
