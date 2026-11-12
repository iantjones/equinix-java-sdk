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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.HashSet;
import java.util.Set;

/**
 * Every required cloud must be reachable from at least one metro of the design, and every workload must
 * reach each required cloud it names from the metro it is placed in (a workload runs in one metro).
 */
public final class RequiredCloudsConstraint implements Constraint {

    /** The id. */
    public static final String ID = "required_clouds";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        Set<String> reached = new HashSet<>();
        for (MetroId metro : portfolio.metros()) {
            reached.addAll(context.reachableClouds(metro));
        }
        for (CloudIntent cloud : context.spec().clouds()) {
            if (Boolean.TRUE.equals(cloud.required()) && !reached.contains(cloud.id())) {
                return ConstraintVerdict.fail(ID, "required cloud '" + cloud.id() + "' is reachable from no metro of "
                        + portfolio.key());
            }
        }
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        for (PlacementPlan.Placement p : plan.placements()) {
            if (p.primary() == null) {
                continue;
            }
            for (String cloudId : p.clouds()) {
                CloudIntent cloud = context.spec().cloud(cloudId);
                if (cloud != null && Boolean.TRUE.equals(cloud.required())
                        && !context.reachableClouds(p.primary()).contains(cloudId)) {
                    return ConstraintVerdict.fail(ID, "'" + p.workload() + "' runs in " + p.primary().code()
                            + ", which does not reach required cloud '" + cloudId + "'");
                }
            }
        }
        return ConstraintVerdict.pass(ID, "every required cloud is reachable where it is needed");
    }
}
