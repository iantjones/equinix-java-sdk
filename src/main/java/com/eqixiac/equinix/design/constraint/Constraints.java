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

package com.eqixiac.equinix.design.constraint;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Chooses the built-in constraints a spec needs, and shared helpers. */
public final class Constraints {

    private Constraints() {
    }

    /**
     * The built-in constraints for a spec: one per field of {@link DesignConstraints} that is set, the
     * workload rules (pinned metro, per-workload latency, cloud reachability) when the spec has workloads
     * or required clouds, the budget when one is stated, and {@link EveryMetroUsedConstraint} always.
     *
     * @param spec the design
     * @return the constraints, in a fixed order
     */
    public static List<Constraint> forSpec(DesignSpec spec) {
        DesignConstraints c = spec.constraints();
        List<Constraint> list = new ArrayList<>();
        if (!c.requiredMetros().isEmpty()) {
            list.add(new RequiredMetrosConstraint());
        }
        if (!c.excludedMetros().isEmpty()) {
            list.add(new ExcludedMetrosConstraint());
        }
        if (!c.requiredRegions().isEmpty()) {
            list.add(new RequiredRegionsConstraint());
        }
        if (!c.excludedRegions().isEmpty()) {
            list.add(new ExcludedRegionsConstraint());
        }
        if (!c.complianceZones().isEmpty()) {
            list.add(new ComplianceZonesConstraint());
        }
        if (!c.geoZones().isEmpty()) {
            list.add(new GeoZonesConstraint());
        }
        if (c.minimumRedundancy() != null) {
            list.add(new MinimumRedundancyConstraint());
        }
        if (c.maxLatencyMs() != null) {
            list.add(new MaxSiteLatencyConstraint());
        }
        if (c.minMetros() != null || c.maxMetros() != null) {
            list.add(new MetroCountConstraint());
        }
        if (spec.workloads().stream().anyMatch(w -> w.pinnedMetro() != null)) {
            list.add(new PinnedWorkloadsConstraint());
        }
        if (spec.workloads().stream().anyMatch(w -> w.maxLatencyMs() != null)) {
            list.add(new WorkloadLatencyConstraint());
        }
        if (spec.clouds().stream().anyMatch(cl -> Boolean.TRUE.equals(cl.required()))) {
            list.add(new RequiredCloudsConstraint());
        }
        if (spec.commercial().budgetMaxMonthly() != null) {
            list.add(new BudgetConstraint());
        }
        list.add(new EveryMetroUsedConstraint());
        return List.copyOf(list);
    }

    static Set<Region> regions(MetroPortfolio portfolio, DesignContext context) {
        Set<Region> regions = new TreeSet<>();
        for (MetroId id : portfolio.metros()) {
            Region region = context.metro(id).map(MetroFacts::region).orElse(null);
            if (region != null) {
                regions.add(region);
            }
        }
        return regions;
    }
}
