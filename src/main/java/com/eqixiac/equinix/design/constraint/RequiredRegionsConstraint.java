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
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The design must have at least one metro in every region of {@code constraints.requiredRegions}. */
public final class RequiredRegionsConstraint implements Constraint {

    /** The id. */
    public static final String ID = "required_regions";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        Set<Region> present = Constraints.regions(portfolio, context);
        List<Region> missing = new ArrayList<>();
        for (Region region : context.spec().constraints().requiredRegions()) {
            if (!present.contains(region)) {
                missing.add(region);
            }
        }
        return missing.isEmpty() ? ConstraintVerdict.pass(ID, "has a metro in every required region " + present)
                : ConstraintVerdict.fail(ID, "no metro in required region(s) " + missing);
    }
}
