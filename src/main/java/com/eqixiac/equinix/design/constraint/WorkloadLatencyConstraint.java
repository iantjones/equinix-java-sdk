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

import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Each workload with a {@code maxLatencyMs} must be placed (by {@link WorkloadPlacer}) in a metro whose
 * worst known round trip to any site is within the limit. Unknown site latencies are not counted.
 */
public final class WorkloadLatencyConstraint implements Constraint {

    /** The id. */
    public static final String ID = "workload_max_latency";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        int checked = 0;
        for (WorkloadIntent w : context.spec().workloads()) {
            if (w.maxLatencyMs() == null) {
                continue;
            }
            PlacementPlan.Placement p = plan.placement(w.label()).orElseThrow();
            if (p.primary() == null) {
                return ConstraintVerdict.fail(ID, "workload '" + w.label() + "' could not be placed");
            }
            OptionalDouble worst = WorkloadPlacer.worstSiteLatency(p.primary(), context);
            if (worst.isPresent()) {
                checked++;
                if (worst.getAsDouble() > w.maxLatencyMs()) {
                    return ConstraintVerdict.fail(ID, String.format(Locale.ROOT, "workload '%s' in %s has a site %.1f ms "
                            + "away, over its %.1f ms limit", w.label(), p.primary().code(), worst.getAsDouble(),
                            w.maxLatencyMs()));
                }
            }
        }
        return ConstraintVerdict.pass(ID, checked == 0 ? "no workload limit could be evaluated (no site latency known)"
                : checked + " workload latency limit(s) met");
    }
}
