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
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;

/** Every workload with a {@code pinnedMetro} needs that metro in the design. */
public final class PinnedWorkloadsConstraint implements Constraint {

    /** The id. */
    public static final String ID = "pinned_workloads";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        List<String> pinned = new ArrayList<>();
        for (WorkloadIntent w : context.spec().workloads()) {
            if (w.pinnedMetro() == null) {
                continue;
            }
            if (!portfolio.contains(MetroId.of(w.pinnedMetro()))) {
                return ConstraintVerdict.fail(ID, "workload '" + w.label() + "' is pinned to " + w.pinnedMetro()
                        + ", which the design does not include");
            }
            pinned.add(w.label() + "@" + w.pinnedMetro());
        }
        return ConstraintVerdict.pass(ID, "pinned workloads placed: " + String.join(", ", pinned));
    }
}
