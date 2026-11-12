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

/** No metro in {@code constraints.excludedMetros} may be used. Prunes per metro. */
public final class ExcludedMetrosConstraint implements Constraint {

    /** The id. */
    public static final String ID = "excluded_metros";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean admits(MetroFacts metro, DesignContext context) {
        return !context.spec().constraints().excludedMetros().contains(metro.id().code());
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        for (MetroId metro : portfolio.metros()) {
            if (context.spec().constraints().excludedMetros().contains(metro.code())) {
                return ConstraintVerdict.fail(ID, metro.code() + " is excluded");
            }
        }
        return ConstraintVerdict.pass(ID, "uses none of " + context.spec().constraints().excludedMetros());
    }
}
