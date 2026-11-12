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
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

/**
 * Every metro must be in a region that at least one zone of {@code constraints.complianceZones} allows
 * ({@code ComplianceZone.getAllowedRegions()}): the design stays within the stated zones. A metro with
 * no known region is not admitted.
 */
public final class ComplianceZonesConstraint implements Constraint {

    /** The id. */
    public static final String ID = "compliance_zones";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean admits(MetroFacts metro, DesignContext context) {
        if (metro.region() == null) {
            return false;
        }
        for (ComplianceZone zone : context.spec().constraints().complianceZones()) {
            if (zone.getAllowedRegions().contains(metro.region())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        for (MetroId id : portfolio.metros()) {
            MetroFacts facts = context.metro(id).orElse(null);
            if (facts == null || !admits(facts, context)) {
                return ConstraintVerdict.fail(ID, id.code() + " is outside the regions "
                        + context.spec().constraints().complianceZones() + " allow");
            }
        }
        return ConstraintVerdict.pass(ID, "every metro is in a region " + context.spec().constraints().complianceZones()
                + " allow");
    }
}
