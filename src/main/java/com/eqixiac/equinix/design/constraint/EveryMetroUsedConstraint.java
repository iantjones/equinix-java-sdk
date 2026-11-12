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
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every metro of a design must carry a workload (primary or standby) or a cloud connection. A metro
 * that carries nothing costs nothing in the model and would otherwise be added for free; it is not a
 * real deployment. Applies only when the spec has workloads or clouds.
 */
public final class EveryMetroUsedConstraint implements Constraint {

    /** The id. */
    public static final String ID = "every_metro_used";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        if (context.spec().workloads().isEmpty() && context.spec().clouds().isEmpty()) {
            return ConstraintVerdict.pass(ID, "nothing to place");
        }
        PlacementPlan plan = WorkloadPlacer.place(portfolio, context);
        Set<MetroId> idle = plan.idleMetros();
        return idle.isEmpty() ? ConstraintVerdict.pass(ID, "every metro carries a workload or a cloud connection")
                : ConstraintVerdict.fail(ID, idle.stream().map(MetroId::code).collect(Collectors.joining(", "))
                        + " would carry nothing");
    }
}
