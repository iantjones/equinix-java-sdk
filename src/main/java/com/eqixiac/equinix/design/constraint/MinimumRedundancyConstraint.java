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
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@code constraints.minimumRedundancy} as an explicit rule on the metro set (see {@link RedundancyTier}):
 * fewest metros, fewest distinct regions, and for {@code N_PLUS_1} two metros sharing a region.
 */
public final class MinimumRedundancyConstraint implements Constraint {

    /** The id. */
    public static final String ID = "minimum_redundancy";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        RedundancyTier tier = context.spec().constraints().minimumRedundancy();
        if (tier == null) {
            return ConstraintVerdict.pass(ID, "no redundancy required");
        }
        if (portfolio.size() < tier.getMinimumMetros()) {
            return ConstraintVerdict.fail(ID, tier + " needs at least " + tier.getMinimumMetros() + " metros, design has "
                    + portfolio.size());
        }
        Set<Region> regions = Constraints.regions(portfolio, context);
        if (regions.size() < tier.getMinimumRegions()) {
            return ConstraintVerdict.fail(ID, tier + " needs metros in at least " + tier.getMinimumRegions()
                    + " regions, design spans " + regions);
        }
        if (tier.isSameRegionPairRequired()) {
            Map<Region, Integer> counts = new HashMap<>();
            boolean pair = false;
            for (MetroId id : portfolio.metros()) {
                Region region = context.metro(id).map(MetroFacts::region).orElse(null);
                if (region != null && counts.merge(region, 1, Integer::sum) >= 2) {
                    pair = true;
                }
            }
            if (!pair) {
                return ConstraintVerdict.fail(ID, tier + " needs two metros in the same region");
            }
        }
        return ConstraintVerdict.pass(ID, tier + " met: " + portfolio.size() + " metros in " + regions);
    }
}
