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
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

/**
 * No metro may be in a region of {@code constraints.excludedRegions}. A metro whose region the
 * catalogue does not state is not admitted: it cannot be shown to be outside the excluded regions.
 */
public final class ExcludedRegionsConstraint implements Constraint {

    /** The id. */
    public static final String ID = "excluded_regions";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean admits(MetroFacts metro, DesignContext context) {
        return metro.region() != null && !context.spec().constraints().excludedRegions().contains(metro.region());
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        for (MetroId id : portfolio.metros()) {
            MetroFacts facts = context.metro(id).orElse(null);
            if (facts == null || !admits(facts, context)) {
                return ConstraintVerdict.fail(ID, id.code() + " is in an excluded region or its region is unknown");
            }
        }
        return ConstraintVerdict.pass(ID, "no metro in " + context.spec().constraints().excludedRegions());
    }
}
