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
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.List;

/** Every metro in {@code constraints.requiredMetros} must be part of the design. */
public final class RequiredMetrosConstraint implements Constraint {

    /** The id. */
    public static final String ID = "required_metros";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        List<String> missing = new ArrayList<>();
        for (String code : context.spec().constraints().requiredMetros()) {
            if (!portfolio.contains(MetroId.of(code))) {
                missing.add(code);
            }
        }
        return missing.isEmpty()
                ? ConstraintVerdict.pass(ID, "includes every required metro " + context.spec().constraints().requiredMetros())
                : ConstraintVerdict.fail(ID, "missing required metro(s) " + String.join(", ", missing));
    }
}
