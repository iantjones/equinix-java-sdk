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

import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

/**
 * {@code constraints.minMetros} and {@code constraints.maxMetros}. The solver already searches only that
 * size range; the constraint makes the limit visible in every verdict and in binding-constraint analysis.
 */
public final class MetroCountConstraint implements Constraint {

    /** The id. */
    public static final String ID = "metro_count";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        Integer min = context.spec().constraints().minMetros();
        Integer max = context.spec().constraints().maxMetros();
        if (min != null && portfolio.size() < min) {
            return ConstraintVerdict.fail(ID, portfolio.size() + " metros, fewer than minMetros " + min);
        }
        if (max != null && portfolio.size() > max) {
            return ConstraintVerdict.fail(ID, portfolio.size() + " metros, more than maxMetros " + max);
        }
        return ConstraintVerdict.pass(ID, portfolio.size() + " metros within " + (min == null ? 1 : min) + ".."
                + (max == null ? "any" : max));
    }
}
